package org.openpdfsign;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

class SignatureVerifierTest {
    @Test
    public void testVerifyValidSignature() throws IOException, CertificateException {
        SignatureVerifier verifier = new SignatureVerifier();
        //@TODO
    }

}