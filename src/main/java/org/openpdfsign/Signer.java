package org.openpdfsign;

import com.beust.jcommander.Strings;
import eu.europa.esig.dss.enumerations.CertificationPermission;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.tsp.CompositeTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.openpdfsign.dss.PdfBoxNativeTableObjectFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class Signer {

    //see PDRectangle
    private static final float POINTS_PER_INCH = 72;
    private static final float POINTS_PER_MM = 1 / (10 * 2.54f) * POINTS_PER_INCH;

    public void signPdf(Path pdfFile, Path outputFile, byte[] keyStore, char[] keyStorePassword, OutputStream binaryOutput, SignatureParameters params) throws IOException {
        boolean visibleSignature = params.getPage() != null;
        //https://github.com/apache/pdfbox/blob/trunk/examples/src/main/java/org/apache/pdfbox/examples/signature/CreateVisibleSignature2.java
        //https://ec.europa.eu/cefdigital/DSS/webapp-demo/doc/dss-documentation.html
        //load PDF file
        //PDDocument doc = PDDocument.load(pdfFile.toFile());

        //load PDF file in DSSDocument format
        DSSDocument toSignDocument = new FileDocument(pdfFile.toFile());

        //load certificate and private key
        JKSSignatureToken signingToken = new JKSSignatureToken(keyStore, new KeyStore.PasswordProtection(keyStorePassword));

        log.debug("Keystore created for signing");
        //PAdES parameters
        PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
        //signatureParameters.bLevel().setSigningDate(new Date());
        String keyAlias = "alias";
        if (signingToken.getKeys().get(0) instanceof KSPrivateKeyEntry) {
            keyAlias = ((KSPrivateKeyEntry) signingToken.getKeys().get(0)).getAlias();
        }
        ;
        signatureParameters.setSigningCertificate(signingToken.getKey(keyAlias).getCertificate());
        signatureParameters.setCertificateChain(signingToken.getKey(keyAlias).getCertificateChain());
        if (params.getUseLT()) {
            //extra signature space for the use of a timestamped signature
            signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LT);
            signatureParameters.setContentSize((int) (SignatureOptions.DEFAULT_SIGNATURE_SIZE * 1.5));
        } else if (params.getUseLTA()) {
            signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);
            signatureParameters.setContentSize((int) (SignatureOptions.DEFAULT_SIGNATURE_SIZE * 1.75));
        } else if (params.getUseTimestamp() || !params.getTSA().isEmpty()) {
            signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
            signatureParameters.setContentSize((int) (SignatureOptions.DEFAULT_SIGNATURE_SIZE * 1.5));
        } else {
            signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        }

        //set certification level
        switch (params.getCertification()) {
            case NOT_CERTIFIED:
                //don't set anything
                break;
            case CERTIFIED_NO_CHANGE_PERMITTED:
                signatureParameters.setPermission(CertificationPermission.NO_CHANGE_PERMITTED);
                break;
            case CERTIFIED_CHANGES_PERMITTED:
                signatureParameters.setPermission(CertificationPermission.CHANGES_PERMITTED);
                break;
            case CERTIFIED_MINIMAL_CHANGES_PERMITTED:
            default:
                signatureParameters.setPermission(CertificationPermission.MINIMAL_CHANGES_PERMITTED);
                break;
        }

        if(!Strings.isStringEmpty(params.getLocation()))
        {
            signatureParameters.setLocation(params.getLocation());
        }

        if(!Strings.isStringEmpty(params.getReason()))
        {
            signatureParameters.setReason(params.getReason());
        }

        if(!Strings.isStringEmpty(params.getContact()))
        {
            signatureParameters.setContactInfo(params.getContact());
        }
        signatureParameters.setAppName("open-pdf-sign");

        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();

        if (signatureParameters.getSignatureLevel() == SignatureLevel.PAdES_BASELINE_LT ||
        signatureParameters.getSignatureLevel() == SignatureLevel.PAdES_BASELINE_LTA) {
            // Capability to download resources from AIA
            commonCertificateVerifier.setAIASource(new DefaultAIASource());

            // Capability to request OCSP Responders
            commonCertificateVerifier.setOcspSource(new OnlineOCSPSource());

            // Capability to download CRL
            commonCertificateVerifier.setCrlSource(new OnlineCRLSource());

            // Still fetch revocation data for signing, even if a certificate chain is not trusted
            commonCertificateVerifier.setCheckRevocationForUntrustedChains(true);

            // Create an instance of a trusted certificate source
            CommonTrustedCertificateSource trustedCertSource = new CommonTrustedCertificateSource();

            // Import defaults
            //CommonCertificateSource commonCertificateSource = TrustedCertificatesLoader.getDefaults();
            CommonCertificateSource commonCertificateSource = new CommonCertificateSource();

            // import the keystore as trusted
            trustedCertSource.importAsTrusted(commonCertificateSource);

            commonCertificateVerifier.addTrustedCertSources(trustedCertSource);
        }

        // Create PAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);

        log.debug("Signature service initialized");

        // Initialize visual signature and configure
        if (visibleSignature) {
            SignatureImageParameters imageParameters = new SignatureImageParameters();
            TableSignatureFieldParameters fieldParameters = new TableSignatureFieldParameters();
            imageParameters.setFieldParameters(fieldParameters);

            if (!Strings.isStringEmpty(params.getImageFile())) {
                imageParameters.setImage(new InMemoryDocument(Files.readAllBytes(Paths.get(params.getImageFile()))));
            } else {
                imageParameters.setImage(new InMemoryDocument((IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("signature.png")))));
            }

            //add new page, if user requested, force reopen document
            if (params.getAddPage() != null && params.getAddPage() == true) {
                PDDocument pdDocument = PDDocument.load(toSignDocument.openStream());
                PDPage newPage = new PDPage(pdDocument.getPage(pdDocument.getNumberOfPages() - 1).getMediaBox());
                pdDocument.addPage(newPage);
                Set<COSDictionary> cosSet = new HashSet<>();
                cosSet.add(newPage.getCOSObject().getCOSDictionary(COSName.PARENT));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                pdDocument.saveIncremental(bos, cosSet);
                pdDocument.close();
                toSignDocument = new InMemoryDocument(bos.toByteArray());
            }

            if (params.getPage() < 0) {
                PDDocument pdDocument = PDDocument.load(toSignDocument.openStream());
                int pageCount = pdDocument.getNumberOfPages();
                fieldParameters.setPage(pageCount + (1 + params.getPage()));
                pdDocument.close();
                log.debug("PDF page count: " + pageCount);

            } else {
                fieldParameters.setPage(params.getPage());
            }
            fieldParameters.setOriginX(params.getLeft() * POINTS_PER_MM * 10f);
            fieldParameters.setOriginY(params.getTop() * POINTS_PER_MM * 10f);
            fieldParameters.setWidth(params.getWidth() * POINTS_PER_MM * 10f);

            // Get the SignedInfo segment that need to be signed.
            // respect local timezone
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
            // user-provided timezone, if any
            if (params.getTimezone() != null) {
                formatter = formatter.withZone(ZoneId.of(params.getTimezone()));
            }
            fieldParameters.setSignatureDate(formatter.format(signatureParameters.getSigningDate().toInstant()));
            fieldParameters.setSignaturString(signingToken.getKey(keyAlias).getCertificate().getSubject().getPrettyPrintRFC2253());
            fieldParameters.setLabelHint(ObjectUtils.firstNonNull(params.getLabelHint(), Configuration.getInstance().getResourceBundle().getString("hint")));
            fieldParameters.setLabelSignee(ObjectUtils.firstNonNull(params.getLabelSignee(), Configuration.getInstance().getResourceBundle().getString("signee")));
            fieldParameters.setLabelTimestamp(ObjectUtils.firstNonNull(params.getLabelTimestamp(), Configuration.getInstance().getResourceBundle().getString("timestamp")));
            if (!Strings.isStringEmpty(params.getHint())) {
                fieldParameters.setHint(params.getHint());
            } else {
                if (params.getNoHint()) {
                    fieldParameters.setHint(null);
                } else {
                    fieldParameters.setHint(Configuration.getInstance().getResourceBundle().getString("hint_text"));
                }
            }
            fieldParameters.setImageOnly(params.getImageOnly());

            signatureParameters.setImageParameters(imageParameters);


            PdfBoxNativeObjectFactory pdfBoxNativeObjectFactory = new PdfBoxNativeTableObjectFactory();
            service.setPdfObjFactory(pdfBoxNativeObjectFactory);
            log.debug("Visible signature parameters set");
        }

        //https://gist.github.com/Manouchehri/fd754e402d98430243455713efada710
        //only use TSP source, if parameter is set
        //if it is set to an url, us this
        //otherwise, default
        if (params.getUseTimestamp() || params.getUseLT() || params.getUseLTA() || params.getTSA() != null) {
            CompositeTSPSource compositeTSPSource = new CompositeTSPSource();
            Map<String, TSPSource> tspSources = new HashMap<>();
            compositeTSPSource.setTspSources(tspSources);
            if (params.getTSA().isEmpty()) {
                Arrays.stream(Configuration.getInstance().getProperties().getStringArray("tsp_sources")).forEach(source -> {
                    tspSources.put(source, new OnlineTSPSource(source));
                });
            } else {
                params.getTSA().stream().forEach(source -> {
                    tspSources.put(source, new OnlineTSPSource(source));
                });
            }
            service.setTspSource(compositeTSPSource);
        }

        //for encrypted PDF files, the passphrase is needed
        if(!StringUtils.isEmpty(params.getPdfPassphrase())) {
            signatureParameters.setPasswordProtection(params.getPdfPassphrase().toCharArray());
        }

        ToBeSigned dataToSign = service.getDataToSign(toSignDocument, signatureParameters);

        // This function obtains the signature value for signed information using the
        // private key and specified algorithm
        DigestAlgorithm digestAlgorithm = signatureParameters.getDigestAlgorithm();
        log.debug("Data to be signed loaded");
        SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, signingToken.getKey(keyAlias));

        /*if (service.isValidSignatureValue(dataToSign, signatureValue, signingToken.getKey("alias").getCertificate())) {
            log.debug("is true");
        }*/
        log.debug("Signature value calculated");

        DSSDocument signedDocument = service.signDocument(toSignDocument, signatureParameters, signatureValue);
        log.debug("Document signing complete");
        if (binaryOutput != null) {
            signedDocument.writeTo(binaryOutput);
        } else {
            signedDocument.save(outputFile.toAbsolutePath().toString());
        }
    }
}
