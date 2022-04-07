package org.openpdfsign;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class SignatureParameters {
    @Parameter(required = false, names={"--image"}, description = "Image to be placed in signature block")
    private String imageFile;

    @Parameter(required = false, names={"--page"}, description = "Page where the signature block should be placed. [-1] for last page")
    private Integer page;

    @Parameter(required = false, names={"--top"}, description = "Y coordinate of the signature block in cm")
    private float top = 1;

    @Parameter(required = false, names={"--left"}, description = "X coordinate of the signature block in cm")
    private float left = 1;

    @Parameter(required = false, names={"--width"}, description = "width of the signature block in cm")
    private float width = 10;

    @Parameter(required = false, names={"--hint"}, description = "text to be displayed in signature field")
    private String hint;

    @Parameter(required = false, names={"--timestamp"}, description = "include signed timestamp")
    private Boolean useTimestamp = false;

    @Parameter(required = false, names={"--tsa"}, description = "use specific time stamping authority as source (if multiple given, will be used in given order as fallback)")
    private List<String> TSA = new LinkedList<>();
}
