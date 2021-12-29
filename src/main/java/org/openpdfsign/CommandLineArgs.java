package org.openpdfsign;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class CommandLineArgs {
    @Parameter(required = true, names = {"-i", "--input"}, description = "input pdf file")
    private String inputFile;

    @Parameter(required = true, names = {"-o", "--output"}, description = "output pdf file")
    private String outputFile;

    @Parameter(required = true, names = {"-k", "--key"}, description = "signature key file or keystore")
    private String keyFile;

    @Parameter(required = false, names={"-p","--passphrase"}, description = "passphrase for the signature key or keystore")
    private String keyPassphrase;

    @Parameter(required = false, names={"-c", "--certificate"}, description = "certificate (chain) to be used")
    private String certificateFile;

    @Parameter(required = false, names={"--image"}, description = "Image to be placed in signature block")
    private String imageFile;

    @Parameter(required = false, names={"--page"}, description = "Page where the signature block should be placed. [-1] for last page")
    private int page;

    @Parameter(required = false, names={"--top"}, description = "Y coordinate of the signature block in cm")
    private float top;

    @Parameter(required = false, names={"--left"}, description = "X coordinate of the signature block in cm")
    private float left;

    @Parameter(required = false, names={"--width"}, description = "width of the signature block in cm")
    private float width;


}
