package org.openpdfsign;

import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
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

        //case: URI
        if (source.startsWith("https://")) {
            FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
            onlineFileLoader.setCacheExpirationTime(60000);
            onlineFileLoader.setDataLoader(new CommonsDataLoader()); // instance of DataLoader which can access to Internet (proxy,...)
            onlineFileLoader.setFileCacheDirectory(new File(System.getProperty("java.io.tmpdir")));
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
