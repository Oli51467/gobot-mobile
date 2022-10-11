package com.irlab.view.process;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.irlab.view.utils.ImageUtils.imagePerspectiveTransform;
import static com.irlab.view.utils.ImageUtils.rotate;

public class InitialBoardDetector {

    public static final String TAG = InitialBoardDetector.class.getName();

    private List<Pair<Double, Double>> corners;

    public InitialBoardDetector(List<Pair<Double, Double>> corners) {
        this.corners = corners;
    }

    public Bitmap getPerspectiveTransformImage(Mat originBoard) {
        // 如果获取的图片为空，则直接返回
        if (originBoard == null) {
            String error = "帧图片为空，未获取图像信息";
            Log.e(TAG, error);
            return null;
        }

        Mat originMatImage = originBoard.clone();

        Bitmap originBitmap = Bitmap.createBitmap(originMatImage.width(), originMatImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(originMatImage, originBitmap);
        // 四个顶点
        List<Point> mc = new ArrayList<>(4);
        // 左上-右上-右下-左下
        mc.add(new Point(corners.get(0).first, corners.get(0).second));
        mc.add(new Point(corners.get(1).first, corners.get(1).second));
        mc.add(new Point(corners.get(2).first, corners.get(2).second));
        mc.add(new Point(corners.get(3).first, corners.get(3).second));

        // 进行图像透视变换和切割识别
        Mat cornerPoints = new Mat(4, 1, CvType.CV_32FC2);
        cornerPoints.put(0, 0,
                mc.get(0).x, mc.get(0).y,
                mc.get(1).x, mc.get(1).y,
                mc.get(2).x, mc.get(2).y,
                mc.get(3).x, mc.get(3).y);

        // TODO:根据实际相机摆放位置调整是否需要调用 rotate() 旋转
        Mat transformResult = imagePerspectiveTransform(originMatImage, cornerPoints);

        Bitmap resultBitmap = Bitmap.createBitmap(transformResult.width(), transformResult.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(transformResult, resultBitmap);
        return resultBitmap;
    }
}
