package org.openpdfsign;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class SignatureParameters {
    @Parameter(required = false, names={"--image"}, description = "Image to be placed in signature block")
    @JsonProperty("image")
    private String imageFile;

    @Parameter(required = false, names={"--page"}, description = "Page where the signature block should be placed. [-1] for last page")
    @JsonProperty(value = "page")
    private Integer page;

    @Parameter(required = false, names={"--top"}, description = "Y coordinate of the signature block in cm")
    @JsonProperty("top")
    private float top = 1;

    @Parameter(required = false, names={"--left"}, description = "X coordinate of the signature block in cm")
    @JsonProperty("left")
    private float left = 1;

    @Parameter(required = false, names={"--width"}, description = "width of the signature block in cm")
    @JsonProperty("width")
    private float width = 10;

    @Parameter(required = false, names={"--hint"}, description = "text to be displayed in signature field")
    @JsonProperty("hint")
    private String hint;

    @Parameter(required = false, names={"--timestamp"}, description = "include signed timestamp")
    @JsonProperty("timestamp")
    private Boolean useTimestamp = false;

    @Parameter(required = false, names={"--tsa"}, description = "use specific time stamping authority as source (if multiple given, will be used in given order as fallback)")
    @JsonProperty("tsa")
    private List<String> TSA = new LinkedList<>();

    @Parameter(required = false, names = {"--timezone"}, description = "use specific timezone for time info, e.g. Europe/Vienna")
    @JsonProperty("timezone")
    private String timezone;
}
