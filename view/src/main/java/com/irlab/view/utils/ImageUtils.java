package com.irlab.view.utils;

import static org.opencv.core.Core.flip;
import static org.opencv.core.Core.transpose;

import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static final int ORTHOGONAL_BOARD_IMAGE_SIZE = 500;

    public static Mat transformOrthogonally(Mat originalImage, Mat boardPositionInImage) {
        Mat orthogonalBoard = new Mat(ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE, originalImage.type());

        Mat orthogonalBoardCorners = new Mat(4, 1, CvType.CV_32FC2);
        orthogonalBoardCorners.put(0, 0,
                0, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, 0,
                ORTHOGONAL_BOARD_IMAGE_SIZE, ORTHOGONAL_BOARD_IMAGE_SIZE,
                0, ORTHOGONAL_BOARD_IMAGE_SIZE);

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(boardPositionInImage, orthogonalBoardCorners);
        Imgproc.warpPerspective(originalImage, orthogonalBoard, transformationMatrix, orthogonalBoard.size());

        return orthogonalBoard;
    }

    public static MatOfPoint convertToMatOfPoint(Mat boardPositionInImage) {
        Point[] corners = {
                new Point(boardPositionInImage.get(0, 0)[0], boardPositionInImage.get(0, 0)[1]),
                new Point(boardPositionInImage.get(1, 0)[0], boardPositionInImage.get(1, 0)[1]),
                new Point(boardPositionInImage.get(2, 0)[0], boardPositionInImage.get(2, 0)[1]),
                new Point(boardPositionInImage.get(3, 0)[0], boardPositionInImage.get(3, 0)[1])
        };
        MatOfPoint boardContour = new MatOfPoint(corners);
        return boardContour;
    }

    public static Bitmap matToBitmap(Mat inputFrame) {
        Bitmap bitmap = Bitmap.createBitmap(inputFrame.cols(), inputFrame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(inputFrame, bitmap);
        return bitmap;
    }

    public static void savePNG_After(Bitmap bitmap, String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/recoder");
        if (!file.exists()) file.mkdirs();
        file = new File(file + File.separator, fileName + ".png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap[][] splitImage(Bitmap rawBitmap, int piece) {
        Bitmap[][] bitmapMatrix = new Bitmap[piece][piece];
        int unitHeight = rawBitmap.getHeight() / piece;
        int unitWidth = rawBitmap.getWidth() / piece;
        Bitmap unitBitmap;
        for (int i = 0; i < piece; i++) {
            for (int j = 0; j < piece; j++) {
                unitBitmap = Bitmap.createBitmap(rawBitmap, j * unitWidth, i * unitHeight, unitWidth, unitHeight);
                bitmapMatrix[i][j] = unitBitmap;
                savePNG_After(unitBitmap, i + "_" + j);
            }
        }
        return bitmapMatrix;
    }

    public static Mat matRotateClockWise90(Mat src)
    {
        // 矩阵转置
        transpose(src, src);
        //0: 沿X轴翻转； >0: 沿Y轴翻转； <0: 沿X轴和Y轴翻转
        flip(src, src, 1);// 翻转模式，flipCode == 0垂直翻转（沿X轴翻转），flipCode>0水平翻转（沿Y轴翻转），flipCode<0水平垂直翻转（先沿X轴翻转，再沿Y轴翻转，等价于旋转180°）
        return src;
    }
}
