package org.openpdfsign;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageDimensionCalculator {

    /**
     * Gets the height of an image given its path and desired width.
     * 
     * @param imagePath    The path to the image file.
     * @param width        The desired width in mm.
     * @return The calculated height in mm, or -1 if an error occurs.
     */
    public static float getImageHeight(String imagePath, float width) {
        try {
            // Read the image file
            BufferedImage image = ImageIO.read(new File(imagePath));
            
            // Get original dimensions in pixels
            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            // Calculate the aspect ratio
            float aspectRatio = (float) originalWidth / originalHeight;

            // Calculate the new height maintaining the aspect ratio
            float height = calculateImageHeight(width, aspectRatio);

            return height;
        } catch (IOException e) {
            System.err.println("Error reading the image file. Please ensure the file path is correct.");
            return 10;
        }
    }

    /**
     * Calculates the height of an image given its width and aspect ratio.
     * 
     * @param width        The width of the image in mm.
     * @param aspectRatio  The aspect ratio of the image (width/height).
     * @return The calculated height of the image in mm.
     */
    private static float calculateImageHeight(float width, float aspectRatio) {
        return width / aspectRatio;
    }
}
