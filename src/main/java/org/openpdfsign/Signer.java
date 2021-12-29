package org.openpdfsign;

import com.beust.jcommander.Strings;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.CertificationPermission;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.CompositeTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.openpdfsign.dss.PdfBoxNativeTableObjectFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Signer {

    //see PDRectangle
    private static final float POINTS_PER_INCH = 72;
    private static final float POINTS_PER_MM = 1 / (10 * 2.54f) * POINTS_PER_INCH;

    public void signPdf(Path pdfFile, Path outputFile, byte[] keyStore, char[] keyStorePassword, SignatureParameters params) throws IOException {
        boolean visibleSignature = params.getPage() != null;
        //https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateVisibleSignature2.java
        //https://ec.europa.eu/cefdigital/DSS/webapp-demo/doc/dss-documentation.html
        //load PDF file
        //PDDocument doc = PDDocument.load(pdfFile.toFile());

        //load PDF file in DSSDocument format
        DSSDocument toSignDocument = new FileDocument(pdfFile.toFile());

        //load certificate and private key
        JKSSignatureToken signingToken = new JKSSignatureToken(keyStore, new KeyStore.PasswordProtection(keyStorePassword));

        //PAdES parameters
        PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
        //signatureParameters.bLevel().setSigningDate(new Date());
        signatureParameters.setSigningCertificate(signingToken.getKey("alias").getCertificate());
        signatureParameters.setCertificateChain(signingToken.getKey("alias").getCertificateChain());
        signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
        signatureParameters.setPermission(CertificationPermission.MINIMAL_CHANGES_PERMITTED);

        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        // Create PAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);

        // Initialize visual signature and configure
        if (visibleSignature) {
            SignatureImageParameters imageParameters = new SignatureImageParameters();
            TableSignatureFieldParameters fieldParameters = new TableSignatureFieldParameters();
            imageParameters.setFieldParameters(fieldParameters);

            if (!Strings.isStringEmpty(params.getImageFile())) {
                imageParameters.setImage(new InMemoryDocument(Files.readAllBytes(Paths.get(params.getImageFile()))));
            }
            else {
                imageParameters.setImage(new InMemoryDocument((IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("signature.png")))));
            }

            if (params.getPage() < 0) {
                PDDocument pdDocument = PDDocument.load(toSignDocument.openStream());
                int pageCount = pdDocument.getNumberOfPages();
                fieldParameters.setPage(pageCount + (1 + params.getPage()));
                pdDocument.close();
            } else {
                fieldParameters.setPage(params.getPage());
            }
            fieldParameters.setOriginX(params.getLeft() * POINTS_PER_MM * 10f);
            fieldParameters.setOriginY(params.getTop() * POINTS_PER_MM * 10f);
            fieldParameters.setWidth(params.getWidth() * POINTS_PER_MM * 10f);

            // Get the SignedInfo segment that need to be signed.
            fieldParameters.setSignatureDate(DateTimeFormatter.ISO_INSTANT.format(signatureParameters.getSigningDate().toInstant()));
            fieldParameters.setSignaturString(signingToken.getKey("alias").getCertificate().getSubject().getPrettyPrintRFC2253());
            if (!Strings.isStringEmpty(params.getHint())) {
                fieldParameters.setHint(params.getHint());
            }
            else {
                fieldParameters.setHint(Configuration.getInstance().getResourceBundle().getString("hint_text"));
            }

            signatureParameters.setImageParameters(imageParameters);


            PdfBoxNativeObjectFactory pdfBoxNativeObjectFactory = new PdfBoxNativeTableObjectFactory();
            service.setPdfObjFactory(pdfBoxNativeObjectFactory);
        }

        //https://gist.github.com/Manouchehri/fd754e402d98430243455713efada710
        CompositeTSPSource compositeTSPSource = new CompositeTSPSource();
        Map<String, TSPSource> tspSources = new HashMap<>();
        compositeTSPSource.setTspSources(tspSources);
        Arrays.stream(Configuration.getInstance().getProperties().getStringArray("tsp_sources")).forEach(source -> {
            tspSources.put(source, new OnlineTSPSource(source));
        });
        service.setTspSource(compositeTSPSource);

        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, signatureParameters);

        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = signatureParameters.getDigestAlgorithm();
        SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, signingToken.getKey("alias"));

        /*if (service.isValidSignatureValue(dataToSign, signatureValue, signingToken.getKey("alias").getCertificate())) {
            System.out.println("is true");
        }*/

        DSSDocument signedDocument = service.signDocument(toSignDocument, signatureParameters, signatureValue);
        signedDocument.save(outputFile.toAbsolutePath().toString());
    }
}
