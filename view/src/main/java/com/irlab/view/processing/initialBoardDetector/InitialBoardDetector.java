package com.irlab.view.processing.initialBoardDetector;

import com.irlab.view.utils.Drawer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InitialBoardDetector {

    public static final String TAG = InitialBoardDetector.class.getName();

    // Camera image
    private Mat image;
    private Mat previewImage;

    // 计算得出的属性
    private boolean processedWithSuccess = false;
    private Mat positionOfBoardInImage;
    private boolean shouldDrawPreview;

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
        // 检测四边形
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
                (int) boardCorners.get(0).x, (int) boardCorners.get(0).y,
                (int) boardCorners.get(1).x, (int) boardCorners.get(1).y,
                (int) boardCorners.get(2).x, (int) boardCorners.get(2).y,
                (int) boardCorners.get(3).x, (int) boardCorners.get(3).y);
        processedWithSuccess = true;
        return true;
    }

    private Mat detectBorders() {
        Mat intermediaryImage = new Mat();
        // 边缘检测
        Imgproc.Canny(image, intermediaryImage, 30, 100);
        // 图像膨胀
        Imgproc.dilate(intermediaryImage, intermediaryImage, Mat.ones(3, 3, CvType.CV_32F));
        return intermediaryImage;
    }

    private List<MatOfPoint> detectContours(Mat imageWithBordersInEvidence) {
        // 轮廓发现
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // 边缘提取 在一幅图像里得到轮廓区域的参数
        Imgproc.findContours(imageWithBordersInEvidence, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // 删除可能是噪音的非常小的轮廓
        for (Iterator<MatOfPoint> it = contours.iterator(); it.hasNext();) {
            MatOfPoint contour = it.next();
            if (Imgproc.contourArea(contour) < 700) {
                it.remove();
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
            // 多边形拟合
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.012, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            // 如果它有 4 个边、是凸包、并且不是太小, 则它是一个有效的四边形
            if (approx2f.toList().size() == 4 && contourArea > 400 && Imgproc.isContourConvex(approx)) {
                quadrilaterals.add(approx);
            }
        }
        return quadrilaterals;
    }

    private MatOfPoint detectBoard(List<MatOfPoint> quadrilaterals) {
        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilaterals);

        MatOfPoint contourClosestToTheBoard = null;
        int numberOfChildren = 9999;
        // 内部必须至少有这个数量的叶子四边形
        int threshold = 10;

        for (MatOfPoint contour : quadrilateralHierarchy.externals) {
            if (quadrilateralHierarchy.hierarchy.get(contour).size() < numberOfChildren && quadrilateralHierarchy.hierarchy.get(contour).size() > threshold) {
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

    public Mat getPositionOfBoardInImage() {
        return positionOfBoardInImage;
    }
}
