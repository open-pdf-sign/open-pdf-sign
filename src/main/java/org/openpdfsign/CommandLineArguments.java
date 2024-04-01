package org.openpdfsign;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandLineArguments extends SignatureParameters {

    @Parameter(required = false, names = { "-h", "--help" }, description = "prints this page", help = true)
    private boolean help = false;

    @Parameter(required = false, names = {"--version"}, description = "prints version of this program")
    private boolean version = false;

    @Parameter(required = false, names = { "-i", "--input" }, description = "input pdf file")
    @JsonProperty(value = "input", required = true)
    private String inputFile;

    @Parameter(required = false, names = {"-o", "--output"}, description = "output pdf file")
    @JsonProperty("output")
    private String outputFile;

    @Parameter(required = false, names = {"-k", "--key"}, description = "signature key file or keystore")
    @JsonProperty(value = "key", required = false)
    private String keyFile;

    @Parameter(required = false, names={"-p","--passphrase"}, description = "passphrase for the signature key or keystore")
    @JsonProperty("passphrase")
    private String keyPassphrase;

    @Parameter(required = false, names={"-c", "--certificate"}, description = "certificate (chain) to be used")
    @JsonProperty("certificate")
    private String certificateFile;

    @Parameter(required = false, names={"-l", "--locale"}, description = "Locale, e.g. de-AT")
    @JsonProperty("locale")
    private String locale;

    @Parameter(required = false, names={"-b", "--binary"}, description = "binary output of PDF")
    @JsonProperty("binary")
    private boolean binaryOutput = false;

    @Parameter(required = false, names={"--port"}, description = "run as server with the given port")
    @JsonProperty(value = "port")
    private int port;

    @Parameter(required = false, names={"--host"}, description = "run as server with the given hostname")
    @JsonProperty(value = "host")
    private String hostname;

    @Parameter(required = false, names={"--config"}, description = "use a configuration file")
    private String configFile;

    @JsonProperty("certificates")
    private ArrayList<HostKeyCertificatePair> certificates;

    @Getter
    @Setter
    public static class HostKeyCertificatePair {
        @JsonProperty
        private String host;
        @JsonProperty("key")
        private String keyFile;
        @JsonProperty("certificate")
        private String certificateFile;
    }
}
