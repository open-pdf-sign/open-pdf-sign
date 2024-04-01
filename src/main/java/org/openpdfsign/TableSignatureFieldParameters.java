package org.openpdfsign;

import eu.europa.esig.dss.pades.SignatureFieldParameters;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TableSignatureFieldParameters extends SignatureFieldParameters  {
    private String signatureDate;
    private String signaturString;
    private String hint;
    private String labelHint;
    private String labelSignee;
    private String labelTimestamp;
    private Boolean imageOnly;
}
