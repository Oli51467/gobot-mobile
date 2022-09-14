package com.irlab.view.processing.initialBoardDetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.irlab.base.MyApplication;
import com.irlab.view.utils.Drawer;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.irlab.view.utils.ImageUtils.imagePerspectiveTransform;
import static com.irlab.view.utils.ImageUtils.matRotateClockWise90;
import static com.irlab.view.utils.ImageUtils.rotateRight;

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

    /**
     * 找到棋盘位置，并获取透视变换后的图片
     *
     * @return
     */
    public Boolean findMarker() {
        // TODO 暂时都先返回 True
        if (true){
            return true;
        }

        // 如果获取的图片为空，则直接返回
        if (image == null) {
            String error = "图片为空，无法发现角点";
            Log.e(TAG, error);
            Toast.makeText(MyApplication.getContext(), error, Toast.LENGTH_SHORT).show();
            return null;
        }
        Mat testImg = image.clone();
        Mat testImg_RGB = image.clone();

        Bitmap originBitmap = Bitmap.createBitmap(testImg_RGB.cols(), testImg_RGB.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg_RGB, originBitmap);

        // 高斯模糊处理
        Imgproc.GaussianBlur(testImg, testImg, new Size(25, 25), 0);

        Imgproc.GaussianBlur(testImg_RGB, testImg_RGB, new Size(25, 25), 0);
        Imgproc.cvtColor(testImg_RGB, testImg_RGB, Imgproc.COLOR_RGB2BGR);
        Scalar lower_RGB = new Scalar(49, 39, 48);
        Scalar upper_RGB = new Scalar(112, 105, 216);
        Core.inRange(testImg_RGB, lower_RGB, upper_RGB, testImg_RGB);
        //
        Bitmap bitmap12 = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg_RGB, bitmap12);

        // 转为HSV格式
        Imgproc.cvtColor(testImg, testImg, Imgproc.COLOR_RGB2HSV);
        // Imgproc.cvtColor(testImg, testImg, Imgproc.COLOR_GRAY2BGR);
        // 颜色过滤，保留红色
        Scalar lower = new Scalar(0, 117, 164);
        Scalar upper = new Scalar(179, 181, 214);
        Core.inRange(testImg, lower, upper, testImg);

        Bitmap bitmap1 = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg, bitmap1);
        Core.bitwise_and(testImg, testImg_RGB, testImg);

        Mat structImage = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        // 溶解和膨胀
        Imgproc.erode(testImg, testImg, structImage, new Point(-1, -1), 2);
        Imgproc.dilate(testImg, testImg, structImage, new Point(-1, -1), 2);

        Bitmap bitmap = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg, bitmap);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(testImg, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() == 4) {
            // 找到marker
            return true;
        } else {
            // 未找到marker
            String error = "发现角点数量不为4，未识别标准棋盘，请调整后再次识别";
            Log.e(TAG, error);
            Toast.makeText(MyApplication.getContext(), error, Toast.LENGTH_SHORT).show();
            return false;
        }

    }


    /**
     * 找到棋盘位置，并获取透视变换后的图片
     *
     * @return
     */
    public Mat getPerspectiveTransformImage() {
        // 如果获取的图片为空，则直接返回
        if (image == null) {
            String error = "帧图片为空，未获取图像信息";
            Log.e(TAG, error);
            Toast.makeText(MyApplication.getContext(), error, Toast.LENGTH_SHORT).show();

            return null;
        }
        Context context = MyApplication.getContext();

        // 测试图像
        /*
        String imagePath = "image/qipan.jpeg";
        Bitmap testBitImg = getImageFromAssetsFile(context, imagePath);
        Mat testImg = new Mat();
        Utils.bitmapToMat(testBitImg, testImg);
        */

        Mat testImg = image.clone();
        Mat originMatImage = testImg.clone();

        Mat testImg_RGB = image.clone();

        // 高斯模糊处理
        Imgproc.GaussianBlur(testImg, testImg, new Size(25, 25), 0);

        Imgproc.GaussianBlur(testImg_RGB, testImg_RGB, new Size(25, 25), 0);
        Imgproc.cvtColor(testImg_RGB, testImg_RGB, Imgproc.COLOR_RGB2BGR);
        Scalar lower_RGB = new Scalar(49, 39, 48);
        Scalar upper_RGB = new Scalar(112, 105, 216);
        Core.inRange(testImg_RGB, lower_RGB, upper_RGB, testImg_RGB);
        //
        Bitmap bitmap12 = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg_RGB, bitmap12);

        // 转为HSV格式
        Imgproc.cvtColor(testImg, testImg, Imgproc.COLOR_RGB2HSV);
        // Imgproc.cvtColor(testImg, testImg, Imgproc.COLOR_GRAY2BGR);
        // 颜色过滤，保留红色
        Scalar lower = new Scalar(0, 117, 164);
        Scalar upper = new Scalar(179, 181, 214);
        Core.inRange(testImg, lower, upper, testImg);

        Bitmap bitmap1 = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg, bitmap1);
        Core.bitwise_and(testImg, testImg_RGB, testImg);

        Mat structImage = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        // 溶解和膨胀
        Imgproc.erode(testImg, testImg, structImage, new Point(-1, -1), 2);
        Imgproc.dilate(testImg, testImg, structImage, new Point(-1, -1), 2);

        Bitmap bitmap = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(testImg, bitmap);


        // Bitmap bitmap = Bitmap.createBitmap(testImg.cols(), testImg.rows(), Bitmap.Config.ARGB_8888);
        // Utils.matToBitmap(testImg, bitmap);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(testImg, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() != 4) {
            // 图像获取不正确
            return null;
        }

        // 计算区域中心
        List<Moments> mu = new ArrayList<>(contours.size());
        for (int i = 0; i < contours.size(); i++) {
            mu.add(Imgproc.moments(contours.get(i)));
        }

        // 四个顶点
        List<Point> mc = new ArrayList<>(contours.size());
        for (int i = 0; i < contours.size(); i++) {
            //add 1e-5 to avoid division by zero
            mc.add(new Point((int) Math.round(mu.get(i).m10 / (mu.get(i).m00 + 1e-5)), (int) Math.round(mu.get(i).m01 / (mu.get(i).m00 + 1e-5))));
        }

        // 对获取的四个点进行重新排序
        Collections.sort(mc, (a, b) -> {
            return (int) (a.getX() - b.getX());
        });

        // 进行图像透视变换和切割识别
        Mat cornerPoints = new Mat(4, 1, CvType.CV_32FC2);
        cornerPoints.put(0, 0,
                mc.get(1).x - 10, mc.get(1).y - 10,
                mc.get(2).x + 10, mc.get(2).y - 10,
                mc.get(3).x + 30, mc.get(3).y + 30,
                mc.get(0).x - 30, mc.get(0).y + 30);

        Mat transformResult = imagePerspectiveTransform(originMatImage, cornerPoints);

        // 将图像透视转换结果转为bitmap，并进行切割
        // Bitmap resultBitmap = Bitmap.createBitmap(transformResult.width(), transformResult.height(), Bitmap.Config.ARGB_8888);
        // Utils.matToBitmap(transformResult, resultBitmap);
        // Bitmap[][] splitResult = splitBitmap(resultBitmap, 19);

        return transformResult;
    }

    /**
     * TODO 临时演示用，之后要删掉
     *
     * @return
     */
    public Mat getPerspectiveTransformImage2(Mat originBoard, List<Pair<Double, Double>> corners) {
        // 如果获取的图片为空，则直接返回
        if (originBoard == null) {
            String error = "帧图片为空，未获取图像信息";
            Log.e(TAG, error);
            Toast.makeText(MyApplication.getContext(), error, Toast.LENGTH_SHORT).show();
            return null;
        }

        Mat originMatImage = originBoard.clone();

        Bitmap originBitmap = Bitmap.createBitmap(originMatImage.width(), originMatImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(originMatImage, originBitmap);

        // 四个顶点
        List<Point> mc = new ArrayList<>(4);
        // 左上-右上-右下-左下
        /*mc.add(new Point(378.0, 58.0));
        mc.add(new Point(1100.0, 82.0));
        mc.add(new Point(1292.0, 826.0));
        mc.add(new Point(152.0, 832.0));*/
        mc.add(new Point(corners.get(0).first, corners.get(0).second));
        mc.add(new Point(corners.get(2).first, corners.get(2).second));
        mc.add(new Point(corners.get(3).first, corners.get(3).second));
        mc.add(new Point(corners.get(1).first, corners.get(1).second));

        // 进行图像透视变换和切割识别
        Mat cornerPoints = new Mat(4, 1, CvType.CV_32FC2);
        cornerPoints.put(0, 0,
                mc.get(0).x, mc.get(0).y,
                mc.get(1).x, mc.get(1).y,
                mc.get(2).x, mc.get(2).y,
                mc.get(3).x, mc.get(3).y);

        // TODO 删除旋转
        // Mat transformResult = rotateRight(imagePerspectiveTransform(originMatImage, cornerPoints));
        Mat transformResult = imagePerspectiveTransform(originMatImage, cornerPoints);

        Bitmap resultBitmap = Bitmap.createBitmap(transformResult.width(), transformResult.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(transformResult, resultBitmap);

        return transformResult;
    }

    /**
     * 图像识别处理方法
     *
     * @return
     */
    public boolean process() {
        // 如果获取的图片为空，则直接返回
        if (image == null) {
            return false;
        }
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
     *
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
        for (int counterIdx = 0; counterIdx < contours.size(); counterIdx++) {
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
