package org.openpdfsign;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class KeyStoreLoader {
    /**
     * Generate a Java Keystore (jks) from public and private key
     * @param certificatePath
     * @param privateKeyPath
     * @param privateKeyPassword
     * @return
     * @throws IOException
     * @throws CertificateException
     * @throws OperatorCreationException
     * @throws PKCSException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public static byte[] loadKeyStoreFromKeys(Path certificatePath, Path privateKeyPath, char[] privateKeyPassword, char[] keyStorePassword) throws IOException, CertificateException, OperatorCreationException, PKCSException, KeyStoreException, NoSuchAlgorithmException, KeyIsNeededException {
        //load key
        Security.addProvider(new BouncyCastleProvider());

        List<X509Certificate> certs = new ArrayList<>();

        X509Certificate cert = null;
        try {
            PEMParser pemParser = new PEMParser(Files.newBufferedReader(certificatePath));
            while (true) {
                X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
                if (certHolder == null) {
                    break;
                }
                cert = new JcaX509CertificateConverter().getCertificate(certHolder);
                certs.add(cert);
            }
        } catch (IOException e) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) factory.generateCertificate(Files.newInputStream(certificatePath));
            certs.add(cert);
        }




        PEMParser privPemParser = new PEMParser(Files.newBufferedReader(privateKeyPath));

        Object readObject = privPemParser.readObject();
        PrivateKeyInfo privateKeyInfo = null;
        if (readObject instanceof PKCS8EncryptedPrivateKeyInfo) {
            //throw exception if key is needed but no passphrase provided
            if (privateKeyPassword == null) {
                throw new KeyIsNeededException();
            }
            PKCS8EncryptedPrivateKeyInfo o = (PKCS8EncryptedPrivateKeyInfo) readObject;
            JceOpenSSLPKCS8DecryptorProviderBuilder builder = new JceOpenSSLPKCS8DecryptorProviderBuilder();
            privateKeyInfo = o.decryptPrivateKeyInfo(builder.build(privateKeyPassword));
        } else if (readObject instanceof PEMKeyPair) {
            PEMKeyPair pair = (PEMKeyPair) readObject;
            privateKeyInfo = pair.getPrivateKeyInfo();
        }
        else if (readObject instanceof  PrivateKeyInfo) {
            privateKeyInfo = (PrivateKeyInfo) readObject;
        }




        PrivateKey privateKey = (new JcaPEMKeyConverter()).getPrivateKey(privateKeyInfo);

        // Put them into a PKCS12 keystore and write it to a byte[]
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(null);
        keystore.setKeyEntry("alias", privateKey, keyStorePassword,
                certs.toArray(new java.security.cert.Certificate[]{}));
        keystore.store(bos, keyStorePassword);
        bos.close();
        byte[] bytes = bos.toByteArray();
        return bytes;
    }

    public static byte[] loadFromKeystore(Path keystorePath, char[] keystorePassphrase) throws IOException, KeyStoreException, KeyIsNeededException {
        byte[] keystore = Files.readAllBytes(keystorePath);

        //JKS - load as is
        try {
            KeyStore jks = KeyStore.getInstance("JKS");
            jks.load(new ByteArrayInputStream(keystore), keystorePassphrase);
            //try loading first key, check if password fits
            String firstAlias = jks.aliases().nextElement();
            if (firstAlias != null) {
                jks.getKey(firstAlias, keystorePassphrase);
            }
            return keystore;
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
            //throw new RuntimeException(e);
        } catch (IOException e) {
            //correct type, invalid passphrase
            throw new KeyIsNeededException();
        } catch (UnrecoverableKeyException e) {
            throw new KeyIsNeededException();
        }


        throw new KeyStoreException("keystore type currently unsupported, please open an issue");
    }


    public static class KeyIsNeededException extends Exception {

    }
}
