package org.openpdfsign.dss;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pdf.PDFServiceMode;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxNativeObjectFactory;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxSignatureService;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawerFactory;
import eu.europa.esig.dss.pdf.visible.SignatureDrawer;

public class PdfBoxNativeTableObjectFactory extends PdfBoxNativeObjectFactory
{

    @Override
    public PDFSignatureService newPAdESSignatureService() {
        return new PdfBoxSignatureService(PDFServiceMode.SIGNATURE, new PdfBoxSignatureDrawerFactory() {
            @Override
            public SignatureDrawer getSignatureDrawer(SignatureImageParameters imageParameters) {
                return new NativePdfBoxVisibleSignatureTableDrawer();
            }
        });
    }
}
