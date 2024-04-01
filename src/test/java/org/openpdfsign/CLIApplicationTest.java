package org.openpdfsign;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class CLIApplicationTest {

    @Test
    void testParseArguments() {
        String[] args = new String[]{
                "-k", getClass().getClassLoader().getResource("key.pem").toString(),
                "-c", getClass().getClassLoader().getResource("cert.pem").toString(),
                "-i", getClass().getClassLoader().getResource("demo.pdf").toString(),
                "-o", "output.pdf"
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals("output.pdf", cla.getOutputFile());
    }

    @Test
    void testParseArgumentsBinary() {
        String[] args = new String[]{
                "-k", getClass().getClassLoader().getResource("key.pem").toString(),
                "-c", getClass().getClassLoader().getResource("cert.pem").toString(),
                "-i", getClass().getClassLoader().getResource("demo.pdf").toString(),
                "-b"
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals(true, cla.isBinaryOutput());
        assertEquals(SignatureParameters.CertificationMode.CERTIFIED_MINIMAL_CHANGES_PERMITTED, cla.getCertification());
    }

    @Test
    void testParseArgumentsOutputMissing() {
        String[] args = new String[]{
                "-k", getClass().getClassLoader().getResource("key.pem").toString(),
                "-c", getClass().getClassLoader().getResource("cert.pem").toString(),
                "-i", getClass().getClassLoader().getResource("demo.pdf").toString(),
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals(null, cla);
    }

    @Test
    void testParseEnum() {
        String[] args = new String[]{
                "-k", getClass().getClassLoader().getResource("key.pem").toString(),
                "-c", getClass().getClassLoader().getResource("cert.pem").toString(),
                "-i", getClass().getClassLoader().getResource("demo.pdf").toString(),
                "-b",
                "--certification","not-certified"
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals(true, cla.isBinaryOutput());
        assertEquals(SignatureParameters.CertificationMode.NOT_CERTIFIED, cla.getCertification());
    }

    @Test
    void testParseArgumentsFromYaml() throws URISyntaxException {
        String[] args = new String[]{
                "--config", (new File(getClass().getClassLoader().getResource("test-config.yml").toURI()).getAbsolutePath())
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals(5, cla.getCertificates().size());
        assertEquals("_",cla.getCertificates().get(0).getHost());
        assertEquals("exampleA.com",cla.getCertificates().get(1).getHost());
        assertEquals("exampleB.com",cla.getCertificates().get(2).getHost());
        assertEquals("exampleC.com",cla.getCertificates().get(3).getHost());
        assertEquals("example.com",cla.getCertificates().get(4).getHost());
        assertEquals(SignatureParameters.CertificationMode.CERTIFIED_NO_CHANGE_PERMITTED, cla.getCertification());

    }

    @Test
    void testParseArgumentsFromYamlAddPage() throws URISyntaxException {
        String[] args = new String[]{
                "--config", (new File(getClass().getClassLoader().getResource("test-config-addpage.yml").toURI()).getAbsolutePath()),
                "--input", "demo.pdf",
                "--output", "out.pdf",
                "-k", getClass().getClassLoader().getResource("key.pem").toString(),
                "-c", getClass().getClassLoader().getResource("cert.pem").toString(),
        };;
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertTrue(cla.getAddPage());
        assertEquals(-1, cla.getPage());
        assertEquals("/etc/openpdfsign/XXXX.png", cla.getImageFile());
        assertEquals("This document is copyrighted by XXXX", cla.getHint());
        assertEquals("Copyright", cla.getLabelHint());
        assertEquals("Reason", cla.getReason());
        assertEquals("Location", cla.getLocation());
        assertEquals("XXXXXX", cla.getContact());
        assertEquals(SignatureParameters.CertificationMode.CERTIFIED_NO_CHANGE_PERMITTED, cla.getCertification());
        assertTrue(cla.getUseLTA());
        assertEquals("fr-FR", cla.getLocale());
        assertEquals(15, cla.getWidth());
    }

    @Test
    void testParseArgumentsFromYamlAndCLI() throws URISyntaxException {
        String[] args = new String[]{
                "--config",
                (new File(getClass().getClassLoader().getResource("test-config-passphrase.yml").toURI()).getAbsolutePath()),
                "--input", "demo.pdf",
                "--output", "out.pdf",
                "--width","15"
        };
        CommandLineArguments cla = CLIApplication.parseArguments(args);
        assertEquals("KEY_PASSPHRASE", cla.getKeyPassphrase());
        assertEquals("cert.crt", cla.getCertificateFile());
        assertEquals("key.pem", cla.getKeyFile());
        assertEquals("demo.pdf", cla.getInputFile());
        assertEquals("out.pdf", cla.getOutputFile());
        assertEquals(11,cla.getLeft());
        assertEquals(15, cla.getWidth());
    }

}