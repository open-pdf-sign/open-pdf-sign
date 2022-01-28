package org.openpdfsign;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CommandLineArguments extends SignatureParameters {
    @Parameter(required = true, names = {"-i", "--input"}, description = "input pdf file")
    private String inputFile;

    @Parameter(required = false, names = {"-o", "--output"}, description = "output pdf file")
    private String outputFile;

    @Parameter(required = true, names = {"-k", "--key"}, description = "signature key file or keystore")
    private String keyFile;

    @Parameter(required = false, names={"-p","--passphrase"}, description = "passphrase for the signature key or keystore")
    private String keyPassphrase;

    @Parameter(required = false, names={"-c", "--certificate"}, description = "certificate (chain) to be used")
    private String certificateFile;

    @Parameter(required = false, names={"-l", "--locale"}, description = "Locale, e.g. de-AT")
    private String locale;

    @Parameter(required = false, names={"-b", "--binary"}, description = "binary output of PDF")
    private boolean binaryOutput = false;
}
