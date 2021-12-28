package org.openpdfsign;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.NativePdfBoxVisibleSignatureDrawer;
import eu.europa.esig.dss.pdf.visible.ImageRotationUtils;
import eu.europa.esig.dss.pdf.visible.ImageUtils;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPosition;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;
import org.vandeseer.easytable.TableDrawer;
import org.vandeseer.easytable.settings.VerticalAlignment;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.ImageCell;
import org.vandeseer.easytable.structure.cell.TextCell;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class NativePdfBoxVisibleSignatureTableDrawer extends NativePdfBoxVisibleSignatureDrawer {
    TableSignatureInformation tableSignatureInformation;

    public NativePdfBoxVisibleSignatureTableDrawer(TableSignatureInformation tableSignatureInformation) {
        super();
        this.tableSignatureInformation = tableSignatureInformation;
    }


    @Override
    public void draw() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(doc, IOUtils.toByteArray(parameters.getImage().openStream()), parameters.getImage().getName());

            // Build the table
            Table myTable = Table.builder()
                    .addColumnsOfWidth(100, 120, 220)
                    .backgroundColor(Color.WHITE)
                    .borderWidth(1)
                    .padding(5)
                    .fontSize(8)
                    .verticalAlignment(VerticalAlignment.MIDDLE)
                    .addRow(Row.builder()
                            .add(ImageCell.builder().image(imageXObject).maxHeight(100).rowSpan(3).build())
                            .add(TextCell.builder().text("Unterzeichner").font(PDType1Font.HELVETICA_BOLD).build())
                            .add(TextCell.builder().text(tableSignatureInformation.getSignaturString()).build())
                            .build())
                    .addRow(Row.builder()
                            //.height(100f)
                            .add(TextCell.builder().text("Unterzeichnungszeitpunkt").font(PDType1Font.HELVETICA_BOLD).build())
                            .add(TextCell.builder().text(tableSignatureInformation.getSignatureDate()).build())
                            .build())
                    .addRow(Row.builder()
                            //.height(100f)
                            .add(TextCell.builder().text("Hinweis").font(PDType1Font.HELVETICA_BOLD).build())
                            .add(TextCell.builder().text(tableSignatureInformation.getHint()).build())
                            .build())
                    .build();


            int pageNumber = parameters.getFieldParameters().getPage() - ImageUtils.DEFAULT_FIRST_PAGE;
            PDPage originalPage = document.getPage(pageNumber);
            SignatureFieldDimensionAndPosition dimensionAndPosition = buildSignatureFieldBox();

            dimensionAndPosition.setBoxHeight(myTable.getHeight()); //
            dimensionAndPosition.setBoxWidth(myTable.getWidth()); //

            // create a new page
            PDPage page = new PDPage(originalPage.getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            PDRectangle rectangle = getPdRectangle(dimensionAndPosition, page);
            widget.setRectangle(rectangle);

            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);

            form.setBBox(new PDRectangle(rectangle.getWidth(), rectangle.getHeight()));

            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);




            /*try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
                //rotateSignature(cs, rectangle, dimensionAndPosition);
                //setFieldBackground(cs, parameters.getBackgroundColor());
                //setText(cs, dimensionAndPosition, parameters);
                setImage(cs, doc, dimensionAndPosition, parameters.getImage());
                // Set up the drawer

            }*/

            try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
                // Set up the drawer
                TableDrawer tableDrawer = TableDrawer.builder()
                        .contentStream(cs)
                        .startX(0) //start from left
                        .startY(myTable.getHeight()) //start from bottom because why not (in pdf)
                        .table(myTable)
                        .build();


                // And go for it!
                //cs.saveGraphicsState();
                tableDrawer.draw();
                //cs.transform(Matrix.getRotateInstance(
//                        ((double) 360 - ImageRotationUtils.getRotation(parameters.getRotation())), 400, 200));

  //              cs.restoreGraphicsState();

            }

            doc.save(baos);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                signatureOptions.setVisualSignature(bais);
                signatureOptions.setPage(pageNumber);
            }

        }
    }

    private PDRectangle getPdRectangle(SignatureFieldDimensionAndPosition dimensionAndPosition, PDPage page) {
        PDRectangle pageRect = page.getMediaBox();
        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(dimensionAndPosition.getBoxX());
        // because PDF starts to count from bottom
        pdRectangle.setLowerLeftY(
                pageRect.getHeight() - dimensionAndPosition.getBoxY() - dimensionAndPosition.getBoxHeight());
        pdRectangle.setUpperRightX(dimensionAndPosition.getBoxX() + dimensionAndPosition.getBoxWidth());
        pdRectangle.setUpperRightY(pageRect.getHeight() - dimensionAndPosition.getBoxY());
        return pdRectangle;
    }

    private void rotateSignature(PDPageContentStream cs, PDRectangle rectangle,
                                 SignatureFieldDimensionAndPosition dimensionAndPosition) throws IOException {
        switch (dimensionAndPosition.getGlobalRotation()) {
            case ImageRotationUtils.ANGLE_90:
                // pdfbox rotates in the opposite way
                cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_270), 0, 0));
                cs.transform(Matrix.getTranslateInstance(-rectangle.getHeight(), 0));
                break;
            case ImageRotationUtils.ANGLE_180:
                cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_180), 0, 0));
                cs.transform(Matrix.getTranslateInstance(-rectangle.getWidth(), -rectangle.getHeight()));
                break;
            case ImageRotationUtils.ANGLE_270:
                cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_90), 0, 0));
                cs.transform(Matrix.getTranslateInstance(0, -rectangle.getWidth()));
                break;
            case ImageRotationUtils.ANGLE_360:
            case ImageRotationUtils.ANGLE_0:
                // do nothing
                break;
            default:
                throw new IllegalStateException(ImageRotationUtils.SUPPORTED_ANGLES_ERROR_MESSAGE);
        }
    }

    private void setImage(PDPageContentStream cs, PDDocument doc,
                          SignatureFieldDimensionAndPosition dimensionAndPosition, DSSDocument image) throws IOException {
        if (image != null) {
            try (InputStream is = image.openStream()) {
                cs.saveGraphicsState();
                byte[] bytes = IOUtils.toByteArray(is);
                PDImageXObject imageXObject = PDImageXObject.createFromByteArray(doc, bytes, image.getName());

                float xAxis = dimensionAndPosition.getImageX();
                float yAxis = dimensionAndPosition.getImageY();
                float width = dimensionAndPosition.getImageWidth();
                float height = dimensionAndPosition.getImageHeight();

                cs.drawImage(imageXObject, xAxis, yAxis, width, height);
                cs.transform(Matrix.getRotateInstance(
                        ((double) 360 - ImageRotationUtils.getRotation(parameters.getRotation())), width, height));

                cs.restoreGraphicsState();
            }
        }
    }

    public static class TableSignatureInformation {
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
}
