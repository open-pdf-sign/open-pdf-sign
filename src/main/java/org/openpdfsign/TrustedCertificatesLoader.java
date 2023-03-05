package org.openpdfsign;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.tsl.source.TLSource;
import eu.europa.esig.dss.tsl.sync.AcceptAllStrategy;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class TrustedCertificatesLoader {
    public static CommonCertificateSource getDefaults() {
        //@TODO: load from file system, if possible
        CommonCertificateSource commonCertificateSource = new CommonCertificateSource();

        Arrays.stream(Configuration.getInstance().getProperties().getStringArray("trusted_certificates")).forEach(source -> {
            try {
                CommonCertificateSource certSource = loadFromSource(source);
                certSource.getCertificates().stream().forEach(cert -> commonCertificateSource.addCertificate(cert));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
        });

        return commonCertificateSource;
    }

    public static CommonCertificateSource loadFromSource(String source) throws IOException, CertificateException {
        CommonCertificateSource commonCertificateSource = new CommonCertificateSource();

        FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
        onlineFileLoader.setCacheExpirationTime(7 * 24 * 60 * 60 * 1000);
        onlineFileLoader.setDataLoader(new CommonsDataLoader()); // instance of DataLoader which can access to Internet (proxy,...)
        onlineFileLoader.setFileCacheDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + "open-pdf-sign"));

        //case: URI
        if (source.contains("lotl.xml")) {
            //case LOTL
            //https://ec.europa.eu/tools/lotl/eu-lotl.xml
            // -- parse trusted list
            TLValidationJob validationJob = new TLValidationJob();

            //LOTL
            LOTLSource lotlSource = new LOTLSource();
            lotlSource.setCertificateSource(new CommonCertificateSource());
            lotlSource.setUrl(source);

            validationJob.setOnlineDataLoader(onlineFileLoader);
            validationJob.setSynchronizationStrategy(new AcceptAllStrategy());
            TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();
            validationJob.setTrustedListCertificateSource(trustedListsCertificateSource);
            validationJob.setListOfTrustedListSources(lotlSource);
            validationJob.onlineRefresh();

            trustedListsCertificateSource.getCertificates().stream().forEach((cert) -> {
                commonCertificateSource.addCertificate(cert);
            });

        }
        else if (source.endsWith(".xml")) {
            //trusted list
            // -- parse trusted list
            TLValidationJob validationJob = new TLValidationJob();
            validationJob.setOnlineDataLoader(onlineFileLoader);
            validationJob.setSynchronizationStrategy(new AcceptAllStrategy());
            TrustedListsCertificateSource trustedListsCertificateSource = new TrustedListsCertificateSource();
            TLSource tlSource = new TLSource();
            tlSource.setCertificateSource(new CommonCertificateSource());
            validationJob.setTrustedListCertificateSource(trustedListsCertificateSource);
            tlSource.setUrl(source);

            validationJob.setTrustedListSources(tlSource);
            validationJob.onlineRefresh();

            trustedListsCertificateSource.getCertificates().stream().forEach((cert) -> {
                commonCertificateSource.addCertificate(cert);
            });
        }
        else if (source.startsWith("https://")) {
            onlineFileLoader.get(source);

            byte[] result = onlineFileLoader.get(source);
            PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(result)));
            while (true) {
                X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
                if (certHolder == null) {
                    break;
                }
                X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
                commonCertificateSource.addCertificate(new CertificateToken(cert));
            }
        }

        return commonCertificateSource;
    }
}
