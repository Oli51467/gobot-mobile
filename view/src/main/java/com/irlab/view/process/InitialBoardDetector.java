package com.irlab.view.process;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.irlab.base.MyApplication;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.irlab.view.utils.ImageUtils.imagePerspectiveTransform;
import static com.irlab.view.utils.ImageUtils.rotate;
import static com.irlab.view.utils.ImageUtils.save_bitmap;

public class InitialBoardDetector {

    public static final String TAG = InitialBoardDetector.class.getName();

    private List<Pair<Double, Double>> corners;

    public InitialBoardDetector(List<Pair<Double, Double>> corners) {
        this.corners = corners;
    }

    /**
     * @return
     */
    public Mat getPerspectiveTransformImage(Mat originBoard) {
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
        mc.add(new Point(corners.get(1).first, corners.get(1).second));
        mc.add(new Point(corners.get(2).first, corners.get(2).second));
        mc.add(new Point(corners.get(3).first, corners.get(3).second));
        mc.add(new Point(corners.get(0).first, corners.get(0).second));

        mc.sort(new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int) (o1.getX() - o2.getX());
            }
        });
        // 进行图像透视变换和切割识别
        Mat cornerPoints = new Mat(4, 1, CvType.CV_32FC2);
        cornerPoints.put(0, 0,
                mc.get(0).x, mc.get(0).y,
                mc.get(1).x, mc.get(1).y,
                mc.get(2).x, mc.get(2).y,
                mc.get(3).x, mc.get(3).y);
        Mat transformResult = rotate(imagePerspectiveTransform(originMatImage, cornerPoints), -1);

        Bitmap resultBitmap = Bitmap.createBitmap(transformResult.width(), transformResult.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(transformResult, resultBitmap);
        save_bitmap(resultBitmap, "board_after_location");
        return transformResult;
    }
}
