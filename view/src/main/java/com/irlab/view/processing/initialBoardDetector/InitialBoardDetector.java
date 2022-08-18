package com.irlab.view.processing.initialBoardDetector;

import android.util.Log;

import com.irlab.view.utils.Drawer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class InitialBoardDetector {

    public static final String TAG = InitialBoardDetector.class.getName();
    public static final int NUMBER_OF_CHILDREN = 9999;
    public static final int INNER_CHILDREN = 2;  // 内部必须至少有这个数量的四边形

    // Camera image
    private Mat image;
    private Mat previewImage;

    // 计算得出的属性
    private Mat positionOfBoardInImage;
    private final boolean shouldDrawPreview;

    public InitialBoardDetector(boolean shouldDrawPreview) {
        this.shouldDrawPreview = shouldDrawPreview;
    }

    public void setImage(Mat image) {
        this.image = image;
    }

    public void setPreviewImage(Mat previewImage) {
        this.previewImage = previewImage;
    }

    public boolean process() {
        if (image == null) return false;
        Mat imageWithBordersInEvidence = detectBorders();
        List<MatOfPoint> contours = detectContours(imageWithBordersInEvidence);
        if (contours.isEmpty()) {
            //Log.i(TAG, "> Image processing: 没有发现棋盘轮廓");
            return false;
        }
        // 由轮廓检测四边形
        List<MatOfPoint> quadrilaterals = detectQuadrilaterals(contours);
        if (quadrilaterals.isEmpty()) {
            //Log.i(TAG, "> Image processing: 没有找到四边形");
            return false;
        }
        MatOfPoint boardQuadrilateral = detectBoard(quadrilaterals);
        if (boardQuadrilateral == null) {
            //Log.i(TAG, "> Image processing: 没有找到符合棋盘的四边形");
            return false;
        }
        List<Point> boardCorners = orderCorners(boardQuadrilateral);
        if (shouldDrawPreview) {
            Drawer.drawBoardContour(previewImage, boardQuadrilateral);
        }
        positionOfBoardInImage = new Mat(4, 1, CvType.CV_32FC2);
        positionOfBoardInImage.put(0, 0,
                boardCorners.get(0).x, boardCorners.get(0).y,
                boardCorners.get(1).x, boardCorners.get(1).y,
                boardCorners.get(2).x, boardCorners.get(2).y,
                boardCorners.get(3).x, boardCorners.get(3).y);
        return true;
    }

    /**
     * 检测边缘
     * @return
     */
    private Mat detectBorders() {
        Mat intermediaryImage = new Mat();
        // 边缘检测
        Imgproc.Canny(image, intermediaryImage, 80, 220);
        // 图像膨胀
        Imgproc.dilate(intermediaryImage, intermediaryImage, Mat.ones(3, 3, CvType.CV_32F));
        return intermediaryImage;
    }

    private List<MatOfPoint> detectContours(Mat imageWithBordersInEvidence) {
        // 轮廓发现
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // 边缘提取 在一幅图像里得到轮廓区域的参数 检测函数: Imgproc.findContours(image, contours, hierarchy, mode, method)
        Imgproc.findContours(imageWithBordersInEvidence, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // 删除可能是噪音的非常小的轮廓
        for (int counterIdx = 0; counterIdx < contours.size(); counterIdx ++ ) {
            double contourArea = Imgproc.contourArea(contours.get(counterIdx));
            if (contourArea < 1000) {
                contours.remove(counterIdx);
            }
        }
        // 图像再次转换为颜色格式
        Imgproc.cvtColor(imageWithBordersInEvidence, image, Imgproc.COLOR_GRAY2BGR, 4);
        imageWithBordersInEvidence.release();
        return contours;
    }

    // 由轮廓检测四边形
    private List<MatOfPoint> detectQuadrilaterals(List<MatOfPoint> contours) {
        List<MatOfPoint> quadrilaterals = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            // 对图像轮廓点多边形拟合
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.012, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            // 如果它有 4 个边、是凸包、并且不是太小, 则它是一个有效的四边形
            if (approx2f.total() == 4 && contourArea > 400 && Imgproc.isContourConvex(approx)) {
                quadrilaterals.add(approx);
            }
        }
        return quadrilaterals;
    }

    private MatOfPoint detectBoard(List<MatOfPoint> quadrilaterals) {
        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);

        MatOfPoint contourClosestToTheBoard = null;
        int numberOfChildren = NUMBER_OF_CHILDREN;

        for (MatOfPoint contour : quadrilateralHierarchy.externals) {
            List<MatOfPoint> convex = quadrilateralHierarchy.hierarchy.get(contour);
            if (convex != null && convex.size() < numberOfChildren && convex.size() > INNER_CHILDREN) {
                contourClosestToTheBoard = contour;
                numberOfChildren = convex.size();
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

    public Mat getPositionOfBoardInImage() {
        return positionOfBoardInImage;
    }
}
