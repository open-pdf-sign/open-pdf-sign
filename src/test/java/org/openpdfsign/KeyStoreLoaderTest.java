package org.openpdfsign;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreLoaderTest {
    private final char[] privateKeyPassword = "123456789".toCharArray();
    private final char[] keyStorePassword = "987654321".toCharArray();

    @Test
    void testLoadKeyStoreFromPemKeys() throws URISyntaxException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, OperatorCreationException, PKCSException, UnrecoverableKeyException, KeyStoreLoader.KeyIsNeededException {
        URL pubKey = getClass().getClassLoader().getResource("cert.pem");
        URL privKey = getClass().getClassLoader().getResource("key.pem");

        byte[] keyStore = KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), privateKeyPassword, keyStorePassword);

        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(new ByteArrayInputStream(keyStore),keyStorePassword);
        assertNotNull(pkcs12.getCertificate("alias"));
        assertNotNull(pkcs12.getKey("alias", keyStorePassword));
        assertNotNull(keyStore);
    }

    @Test
    void testLoadKeyWithMissingPassword() {
        URL pubKey = getClass().getClassLoader().getResource("cert.pem");
        URL privKey = getClass().getClassLoader().getResource("key.pem");

        assertThrows(KeyStoreLoader.KeyIsNeededException.class,  () -> {
            KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), null, keyStorePassword);
        });
    }

    @Test
    void testLoadKeyStoreFromCrtKeys() throws URISyntaxException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, OperatorCreationException, PKCSException, UnrecoverableKeyException, KeyStoreLoader.KeyIsNeededException {
        URL pubKey = getClass().getClassLoader().getResource("cert.crt");
        URL privKey = getClass().getClassLoader().getResource("key_nopass.pem");

        byte[] keyStore = KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), privateKeyPassword, keyStorePassword);
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(new ByteArrayInputStream(keyStore),keyStorePassword);
        assertNotNull(pkcs12.getCertificate("alias"));
        assertNotNull(pkcs12.getKey("alias", keyStorePassword));
        assertNotNull(keyStore);
    }

    @Test
    void testLoadECCKeys() throws URISyntaxException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyStoreLoader.KeyIsNeededException, OperatorCreationException, PKCSException, UnrecoverableKeyException {
        URL pubKey = getClass().getClassLoader().getResource("cert-ecc.pem");
        URL privKey = getClass().getClassLoader().getResource("key-ecc.pem");

        byte[] keyStore = KeyStoreLoader.loadKeyStoreFromKeys(Paths.get(pubKey.toURI()), Paths.get(privKey.toURI()), privateKeyPassword, keyStorePassword);
        KeyStore pkcs12 = KeyStore.getInstance("PKCS12");
        pkcs12.load(new ByteArrayInputStream(keyStore),keyStorePassword);
        assertNotNull(pkcs12.getCertificate("alias"));
        assertNotNull(pkcs12.getKey("alias", keyStorePassword));
        assertNotNull(keyStore);
    }
}