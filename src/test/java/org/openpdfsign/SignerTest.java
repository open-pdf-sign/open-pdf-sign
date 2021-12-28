package org.openpdfsign;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

class SignerTest {

    @Test
    void testSignPdf() throws URISyntaxException, IOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, PKCSException, KeyStoreException {
        URL pubKey = getClass().getClassLoader().getResource("cert.crt");
        URL privKey = getClass().getClassLoader().getResource("key.pem");

        final char[] password = "123456789".toCharArray();
        final char[] keyStorePassword = "987654321".toCharArray();

        byte[] keyStore = KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), password, keyStorePassword);


        URL demoPdf = getClass().getClassLoader().getResource("demo2.pdf");
        Signer signer = new Signer();
        signer.signPdf(Paths.get(demoPdf.toURI()),keyStore,keyStorePassword);
        System.out.println(2 + demoPdf.toString());
    }
}