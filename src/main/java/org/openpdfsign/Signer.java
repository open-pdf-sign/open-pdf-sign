package org.openpdfsign;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignerTextPosition;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.*;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Signer {
    public void signPdf(Path pdfFile, byte[] keyStore, char[] keyStorePassword) throws IOException {
        //https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateVisibleSignature2.java
        //https://ec.europa.eu/cefdigital/DSS/webapp-demo/doc/dss-documentation.html
        //load PDF file
        //PDDocument doc = PDDocument.load(pdfFile.toFile());

        //load PDF file in DSSDocument format
        DSSDocument toSignDocument = new FileDocument(pdfFile.toFile());
        PDDocument pdDocument = PDDocument.load(toSignDocument.openStream());

        int pageCount = pdDocument.getNumberOfPages();

        pdDocument.close();

        //load certificate and private key
        JKSSignatureToken signingToken = new JKSSignatureToken(keyStore, new KeyStore.PasswordProtection(keyStorePassword));

        //PAdES parameters
        PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
        signatureParameters.bLevel().setSigningDate(new Date());
        signatureParameters.setSigningCertificate(signingToken.getKey("alias").getCertificate());
        signatureParameters.setCertificateChain(signingToken.getKey("alias").getCertificateChain());
        signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        signatureParameters.setPermission(CertificationPermission.CHANGES_PERMITTED);

        // Initialize visual signature and configure
        SignatureImageParameters imageParameters = new SignatureImageParameters();
        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
        textParameters.setText("Wurde signiert von NetzBeweis");
        textParameters.setBackgroundColor(Color.green);
        textParameters.setPadding(3);
        textParameters.setSignerTextPosition(SignerTextPosition.BOTTOM);
        imageParameters.setTextParameters(textParameters);
        imageParameters.setFieldParameters(fieldParameters);
        try {
            imageParameters.setImage(new InMemoryDocument(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("signature.png").toURI())), "a.svg", MimeType.SVG));

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        fieldParameters.setPage(pageCount);
        fieldParameters.setOriginX(50);
        fieldParameters.setOriginY(400);
        fieldParameters.setWidth(300);
        fieldParameters.setHeight(200);
        signatureParameters.setImageParameters(imageParameters);

        //https://github.com/vandeseer/easytable/blob/master/src/test/java/org/vandeseer/MinimumWorkingExample.java - Table drawer


        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        // Create PAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);

        // Get the SignedInfo segment that need to be signed.
        NativePdfBoxVisibleSignatureTableDrawer.TableSignatureInformation tableSignatureInformation = new NativePdfBoxVisibleSignatureTableDrawer.TableSignatureInformation();
        tableSignatureInformation.setSignatureDate(DateTimeFormatter.ISO_INSTANT.format(signatureParameters.getSigningDate().toInstant()));
        tableSignatureInformation.setSignaturString(signingToken.getKey("alias").getCertificate().getSubject().getPrettyPrintRFC2253());
        tableSignatureInformation.setHint("Die Echtheit der Signatur kann unter www.signaturprüfung.at überprüft werden.");

        PdfBoxNativeObjectFactory pdfBoxNativeObjectFactory = new PdfBoxNativeTableObjectFactory(tableSignatureInformation);
        service.setPdfObjFactory(pdfBoxNativeObjectFactory);


        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, signatureParameters);

        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = signatureParameters.getDigestAlgorithm();
        SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, signingToken.getKey("alias"));


        /*if (service.isValidSignatureValue(dataToSign, signatureValue, signingToken.getKey("alias").getCertificate())) {
            System.out.println("is true");
        }*/

        DSSDocument signedDocument = service.signDocument(toSignDocument, signatureParameters, signatureValue);
        signedDocument.save("signed2.pdf");

        System.out.println(1);
    }
}
