package org.openpdfsign;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Strings;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Locale;

public class CLIApplication {

    public static void main(String[] args) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, OperatorCreationException, PKCSException {
        CommandLineArguments cla = new CommandLineArguments();
        JCommander parser = JCommander.newBuilder()
                .addObject(cla)
                .build();


        try {
            parser.parse(args);
        }
        catch(ParameterException ex) {
            ex.printStackTrace();
            parser.usage();
            return;
        }

        //System.out.println("Running with " + cla);

        //load locale, if any
        if (!Strings.isStringEmpty(cla.getLocale())) {
            Configuration.getInstance(Locale.forLanguageTag(cla.getLocale()));
        }

        //convert to keystore, if not already given
        byte[] keystore = null;
        char[] keystorePassphrase = null;
        if (!Strings.isStringEmpty(cla.getKeyPassphrase())) {
            keystorePassphrase = cla.getKeyPassphrase().toCharArray();
        } else {
            keystorePassphrase = "123456789".toCharArray();
        }
        if (!Strings.isStringEmpty(cla.getCertificateFile()) &&
            !Strings.isStringEmpty(cla.getKeyFile())) {
            keystore = KeyStoreLoader.loadKeyStoreFromKeys(
                    Paths.get(cla.getCertificateFile()),
                    Paths.get(cla.getKeyFile()),
                    (cla.getKeyPassphrase() == null) ? null : cla.getKeyPassphrase().toCharArray(),
                    keystorePassphrase
            );
        }
        else if (!Strings.isStringEmpty(cla.getKeyFile()) &&
                !Strings.isStringEmpty(cla.getKeyPassphrase())) {
            keystore = Files.readAllBytes(Paths.get(cla.getKeyFile()));
        }

        Path pdfFile = Paths.get(cla.getInputFile());
        Path outputFile = Paths.get(cla.getOutputFile());

        Signer s = new Signer();
        s.signPdf(pdfFile, outputFile, keystore, keystorePassphrase, cla.isBinaryOutput(), cla);
    }
}
