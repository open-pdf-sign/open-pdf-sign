package org.openpdfsign;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pdf.PDFServiceMode;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawerFactory;
import eu.europa.esig.dss.pdf.visible.SignatureDrawer;

public class PdfBoxNativeTableObjectFactory extends PdfBoxNativeObjectFactory
{
    private NativePdfBoxVisibleSignatureTableDrawer.TableSignatureInformation signatureInformation;
    public PdfBoxNativeTableObjectFactory(NativePdfBoxVisibleSignatureTableDrawer.TableSignatureInformation signatureInformation) {
        super();
        this.signatureInformation = signatureInformation;
    }

    public PdfBoxNativeTableObjectFactory() {
        super();
    }

    @Override
    public PDFSignatureService newPAdESSignatureService() {
        return new PdfBoxSignatureService(PDFServiceMode.SIGNATURE, new PdfBoxSignatureDrawerFactory() {
            @Override
            public SignatureDrawer getSignatureDrawer(SignatureImageParameters imageParameters) {
                return new NativePdfBoxVisibleSignatureTableDrawer(signatureInformation);
            }
        });
    }
}
