package com.irlab.view.utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Drawer {
    private static Scalar mRed   = new Scalar(255,   0,   0);

    public static void drawBoardContour(Mat image, MatOfPoint boardContour) {
        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        contourList.add(boardContour);
        Imgproc.drawContours(image, contourList, -1, mRed, 6);
    }
}
