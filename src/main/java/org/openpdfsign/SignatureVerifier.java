package org.openpdfsign;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.detailedreport.DetailedReport;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Slf4j
public class SignatureVerifier {

    public void verifyPdfSignature(Path pdfFile, Path certificate) throws IOException, CertificateException {
        //https://ec.europa.eu/digital-building-blocks/DSS/webapp-demo/doc/dss-documentation.html#_validating_an_ades_signature

        //load PDF file in DSSDocument format
        DSSDocument toValidateDocument = new FileDocument(pdfFile.toFile());

        // Create common certificate verifier
        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        // Create PAdESService for signature
        PAdESService service = new PAdESService(commonCertificateVerifier);

        // Capability to download resources from AIA
        commonCertificateVerifier.setAIASource(new DefaultAIASource());

        // Capability to request OCSP Responders
        commonCertificateVerifier.setOcspSource(new OnlineOCSPSource());

        // Capability to download CRL
        commonCertificateVerifier.setCrlSource(new OnlineCRLSource());

        // Create an instance of a trusted certificate source
        CommonTrustedCertificateSource trustedCertSource = new CommonTrustedCertificateSource();

        //get the trusted certificate store
        X509Certificate cert = null;
        try {
            PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificate));
            while (true) {
                X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
                if (certHolder == null) {
                    break;
                }
                cert = new JcaX509CertificateConverter().getCertificate(certHolder);

            }
        } catch (IOException | CertificateException e) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) factory.generateCertificate(Files.newInputStream(certificate));

        }

        CertificateToken certificateToken = new CertificateToken(cert);

        //CertificateToken certificate = signingToken.getKey("1").getCertificate();
        CommonCertificateSource commonCertificateSource = new CommonCertificateSource();
        commonCertificateSource.addCertificate(certificateToken);


        // import the keystore as trusted
        trustedCertSource.importAsTrusted(commonCertificateSource);

        commonCertificateVerifier.addTrustedCertSources(trustedCertSource);

        // Add trust anchors (trusted list, keystore,...) to a list of trusted certificate sources
        // Hint : use method {@code CertificateVerifier.setTrustedCertSources(certSources)} in order to overwrite the existing list
        commonCertificateVerifier.addTrustedCertSources(trustedCertSource);


        // We create an instance of DocumentValidator
        // It will automatically select the supported validator from the classpath
        SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(toValidateDocument);

        // We add the certificate verifier (which allows to verify and trust certificates)
        documentValidator.setCertificateVerifier(commonCertificateVerifier);



        // Here, everything is ready. We can execute the validation (for the example, we use the default and embedded
        // validation policy)
        Reports reports = documentValidator.validateDocument();

        // We have 4 reports
        // The diagnostic data which contains all used and static data
        DiagnosticData diagnosticData = reports.getDiagnosticData();

        // The detailed report which is the result of the process of the diagnostic data and the validation policy
        DetailedReport detailedReport = reports.getDetailedReport();

        // The simple report is a summary of the detailed report (more user-friendly)
        SimpleReport simpleReport = reports.getSimpleReport();

        // The JAXB representation of the ETSI Validation report (ETSI TS 119 102-2)
        ValidationReportType estiValidationReport = reports.getEtsiValidationReportJaxb();

        ObjectMapper om = new ObjectMapper();
        String s = om.writer(new DefaultPrettyPrinter()).writeValueAsString(simpleReport);
        System.out.println(s);

    }
}
