package org.openpdfsign;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.*;

class SignatureVerifierTest {
    @Test
    public void testVerifyValidSignature() throws IOException, CertificateException {
        SignatureVerifier verifier = new SignatureVerifier();
        //verifier.verifyPdfSignature(Paths.get("signed2.pdf"), Paths.get("src/test/resources/atrust_corporate07.crt"));
        verifier.verifyPdfSignature(Paths.get("signed3nbcom.pdf"), Paths.get("src/test/resources/isrgrootx1.pem"));

    }

}