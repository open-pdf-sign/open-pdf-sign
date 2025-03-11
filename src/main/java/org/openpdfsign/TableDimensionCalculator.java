package org.openpdfsign;

import java.awt.Color;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.vandeseer.easytable.settings.HorizontalAlignment;
import org.vandeseer.easytable.settings.VerticalAlignment;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.ImageCell;
import org.vandeseer.easytable.structure.cell.TextCell;

public class TableDimensionCalculator {

    public static float[] calculateTableDimensions(TableSignatureFieldParameters tableParameters, PDImageXObject imageXObject) {

        Table.TableBuilder myTableBuilder = Table.builder();

        final float imageColumnWidth = 75;
        final float labelColumnWidth = 90;
        float tableWidth = tableParameters.getWidth();
        tableWidth = Math.max((imageColumnWidth + labelColumnWidth + 50), tableWidth);

        boolean hasHint = tableParameters.getHint() != null;
        myTableBuilder
                .addColumnsOfWidth(imageColumnWidth, labelColumnWidth, (tableWidth - imageColumnWidth - labelColumnWidth))
                .backgroundColor(Color.WHITE)
                .borderWidth(0.75f)
                .padding(5)
                .fontSize(8)
                .verticalAlignment(VerticalAlignment.TOP)
                .addRow(Row.builder()
                        .add(ImageCell.builder().image(imageXObject).maxHeight(75)
                                .verticalAlignment(VerticalAlignment.MIDDLE).horizontalAlignment(HorizontalAlignment.CENTER).rowSpan((hasHint ? 3 : 2)).build())
                        .add(TextCell.builder().text(tableParameters.getLabelSignee()).font(PDType1Font.HELVETICA_BOLD).horizontalAlignment(HorizontalAlignment.RIGHT).build())
                        .add(TextCell.builder().text(tableParameters.getSignaturString()).build())
                        .build())
                .addRow(Row.builder()
                        .add(TextCell.builder().text(tableParameters.getLabelTimestamp()).font(PDType1Font.HELVETICA_BOLD).horizontalAlignment(HorizontalAlignment.RIGHT).build())
                        .add(TextCell.builder().text(tableParameters.getSignatureDate()).build())
                        .build());

        if (hasHint) {
            myTableBuilder = myTableBuilder.addRow(Row.builder()
                    .add(TextCell.builder().text(tableParameters.getLabelHint()).font(PDType1Font.HELVETICA_BOLD).horizontalAlignment(HorizontalAlignment.RIGHT).build())
                    .add(TextCell.builder().text(tableParameters.getHint()).build())
                    .build());
        }

        Table myTable = myTableBuilder.build();

        float height = myTable.getHeight();
        float width = myTable.getWidth();

        return new float[]{height, width};

    }

}
