package org.openpdfsign;

import eu.europa.esig.dss.pades.SignatureFieldParameters;

public class TableSignatureFieldParameters extends SignatureFieldParameters  {
    private String signatureDate;
    private String signaturString;
    private String hint;

    public String getSignatureDate() {
        return signatureDate;
    }

    public void setSignatureDate(String signatureDate) {
        this.signatureDate = signatureDate;
    }

    public String getSignaturString() {
        return signaturString;
    }

    public void setSignaturString(String signaturString) {
        this.signaturString = signaturString;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

}
