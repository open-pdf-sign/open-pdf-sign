package org.openpdfsign;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.Locale;

class SignerTest {

    @Test
    void testSignPdf() throws URISyntaxException, IOException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, PKCSException, KeyStoreException {
        URL pubKey = getClass().getClassLoader().getResource("cert.pem");
        URL privKey = getClass().getClassLoader().getResource("key.pem");
        Configuration.getInstance(new Locale("de","AT"));

        final char[] password = "123456789".toCharArray();
        final char[] keyStorePassword = "987654321".toCharArray();

        byte[] keyStore = KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), password, keyStorePassword);


        URL demoPdf = getClass().getClassLoader().getResource("demo.pdf");

        SignatureParameters params = new SignatureParameters();
        params.setPage(-1);
        params.setLeft(3);
        params.setTop(24);
        params.setWidth(15);
        Path image = Paths.get(getClass().getClassLoader().getResource("signature.png").toURI());
        params.setImageFile(image.toAbsolutePath().toString());

        Signer signer = new Signer();
        signer.signPdf(Paths.get(demoPdf.toURI()), Paths.get("signed3.pdf"),keyStore,keyStorePassword, true, params);
        System.out.println(2 + demoPdf.toString());
    }
}