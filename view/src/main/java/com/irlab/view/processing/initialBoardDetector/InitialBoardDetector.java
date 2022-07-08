package com.irlab.view.processing.initialBoardDetector;

import android.util.Log;

import com.irlab.view.processing.Drawer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Detects the position of a Go board in an image and its dimension (9x9, 13x13 or 19x19).
 */
public class InitialBoardDetector {

    public static final String TAG = InitialBoardDetector.class.getName();

    // Camera image
    private Mat image;
    private Mat previewImage;

    // Attributes calculated by the class
    private boolean processedWithSuccess = false;
    private int boardDimension;
    private Mat positionOfBoardInImage;
    private boolean shouldDrawPreview = false;

    public InitialBoardDetector(boolean shouldDrawPreview) {
        this.shouldDrawPreview = shouldDrawPreview;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    public void setPreviewImage(Mat previewImage) {
        this.previewImage = previewImage;
    }

    /**
     * Processes the provided image. Returns true if the complete processing ran with success, i.e.,
     * if a Go board was detected in the image. Returns false otherwise.
     *
     * @return boolean
     */
    public boolean process() {
        if (image == null) {
            // throw error
            return false;
        }

        Mat imageWithBordersInEvidence = detectBorders();

        List<MatOfPoint> contours = detectContours(imageWithBordersInEvidence);

        if (contours.isEmpty()) {
            Log.i(TAG, "> Image processing: contours were not found.");
            return false;
        }

        List<MatOfPoint> quadrilaterals = detectQuadrilaterals(contours);

        if (quadrilaterals.isEmpty()) {
            Log.i(TAG, "> Image processing: quadrilaterals were not found.");
            return false;
        }

        MatOfPoint boardQuadrilateral = detectBoard(quadrilaterals);

        if (boardQuadrilateral == null) {
            Log.i(TAG, "> Image processing: board quadrilateral was not found.");
            return false;
        }

        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);
        double averageArea = 0;
        for (MatOfPoint quadrilateral : quadrilateralHierarchy.hierarchy.get(boardQuadrilateral)) {
            averageArea += Imgproc.contourArea(quadrilateral);
        }
        averageArea /= quadrilateralHierarchy.hierarchy.get(boardQuadrilateral).size();
        double boardArea = Imgproc.contourArea(boardQuadrilateral);
        double ratio = averageArea / boardArea;

        // Determines the dimension of the board according to the ratio between the area of the
        // internal quadrilaterals and the area of the board quadrilateral
        if (ratio <= 1.0 / 324.0) {
            boardDimension = 19;
        }
        else if (ratio <= 1.0 / 144.0) {
            boardDimension = 13;
        }
        else {
            boardDimension = 9;
        }

        List<Point> boardCorners = orderCorners(boardQuadrilateral);

        if (shouldDrawPreview) {
            Drawer.drawBoardContour(previewImage, boardQuadrilateral);
        }

        positionOfBoardInImage = new Mat(4, 1, CvType.CV_32FC2);
        positionOfBoardInImage.put(0, 0,
                (int) boardCorners.get(0).x, (int) boardCorners.get(0).y,
                (int) boardCorners.get(1).x, (int) boardCorners.get(1).y,
                (int) boardCorners.get(2).x, (int) boardCorners.get(2).y,
                (int) boardCorners.get(3).x, (int) boardCorners.get(3).y);

        processedWithSuccess = true;

        return true;
    }

    private Mat detectBorders() {
        Mat intermediaryImage = new Mat();
        Imgproc.Canny(image, intermediaryImage, 30, 100);
        Imgproc.dilate(intermediaryImage, intermediaryImage, Mat.ones(3, 3, CvType.CV_32F));
        return intermediaryImage;
    }

    private List<MatOfPoint> detectContours(Mat imageWithBordersInEvidence) {
        // The contours delimited by lines are found
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imageWithBordersInEvidence, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Log.d(TAG, "Number of contours found: " + contours.size());

        // Remove very small contours which are probably noise
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            // With 1000 already loses the smaller quadrilaterals in a 19x19 board
            // The ideal would be to do this as a ratio on the area of the image
            if (Imgproc.contourArea(contour) < 700) {
                it.remove();
            }
        }

        // Image is converted to a color format again
        Imgproc.cvtColor(imageWithBordersInEvidence, image, Imgproc.COLOR_GRAY2BGR, 4);
        imageWithBordersInEvidence.release();
        return contours;
    }

    private List<MatOfPoint> detectQuadrilaterals(List<MatOfPoint> contours) {
        List<MatOfPoint> quadrilaterals = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.012, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            // If it has 4 sides, it's convex and not too small, it's a valid quadrilateral
            if (approx2f.toList().size() == 4 &&
                    contourArea > 400 &&
                    Imgproc.isContourConvex(approx)) {
                quadrilaterals.add(approx);
            }
        }

        Log.d(TAG, "Number of quadrilaterals found: " + quadrilaterals.size());
        return quadrilaterals;
    }

    private MatOfPoint detectBoard(List<MatOfPoint> quadrilaterals) {
        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);

        MatOfPoint contourClosestToTheBoard = null;
        int numberOfChildren = 9999;
        // Must have at least this number of leaf quadrilaterals inside
        int threshold = 10;

        for (MatOfPoint contour : quadrilateralHierarchy.externals) {
            if (quadrilateralHierarchy.hierarchy.get(contour).size() < numberOfChildren &&
                    quadrilateralHierarchy.hierarchy.get(contour).size() > threshold) {
                contourClosestToTheBoard = contour;
                numberOfChildren = quadrilateralHierarchy.hierarchy.get(contour).size();
            }
        }

        return contourClosestToTheBoard;
    }

    private List<Point> orderCorners(MatOfPoint boardQuadrilateral) {
        List<Point> corners = new ArrayList<>();
        corners.add(boardQuadrilateral.toArray()[0]);
        corners.add(boardQuadrilateral.toArray()[3]);
        corners.add(boardQuadrilateral.toArray()[2]);
        corners.add(boardQuadrilateral.toArray()[1]);
        return corners;
    }

    public int getBoardDimension() {
        if (!processedWithSuccess) {
            // throw error
        }
        return boardDimension;
    }

    public Mat getPositionOfBoardInImage() {
        if (!processedWithSuccess) {
            // throw error
        }
        return positionOfBoardInImage;
    }

}
