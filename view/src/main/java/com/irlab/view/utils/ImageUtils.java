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


    /**
     * 分割位图
     * @param rawBitmap
     * @param piece
     * @return
     */
    public static Bitmap[][] splitBitmap(Bitmap rawBitmap, int piece) {

        Bitmap[][] bitmapMatrix = new Bitmap[piece + 1][piece + 1];
        int unitHeight = rawBitmap.getHeight() / piece;
        int unitWidth = rawBitmap.getWidth() / piece;
        Bitmap unitBitmap;
        for (int i = 0; i < piece; i ++ ) {
            for (int j = 0; j < piece; j ++ ) {
                unitBitmap = Bitmap.createBitmap(rawBitmap, j * unitWidth, i * unitHeight, unitWidth, unitHeight);
                bitmapMatrix[i + 1][j + 1] = unitBitmap;
                // savePNG_After(unitBitmap, i + "==" + j);
            }
        }
        return bitmapMatrix;
    }

    /**
     * 基于角坐标的围棋透视变换
     * @param originImage
     * @param cornerPoints
     * @return
     */
    public static Mat imagePerspectiveTransform(Mat originImage, Mat cornerPoints){
        int x = 4600;
        int y = 4900;

        Mat resultImage = new Mat(y, x, originImage.type());
        Mat resultCorners = new Mat(4, 1, CvType.CV_32FC2);
        // 添加四个点，左下-左上-右上-右下
        resultCorners.put(0,0,  0, 0, x, 0, x, y, 0, y);

        Mat transformationMatrix = Imgproc.getPerspectiveTransform(cornerPoints, resultCorners);
        Imgproc.warpPerspective(originImage, resultImage, transformationMatrix, resultImage.size());

        return resultImage;
    }

    /**
     * 原来的图像变换
     * @param originalImage
     * @param boardPositionInImage
     * @return
     */
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

    /**
     * 保存图像信息
     * @param bitmap
     * @param fileName
     */
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

    /**
     * 分割图片
     * @param rawBitmap
     * @param piece
     * @return
     */
    public static Bitmap[][] splitImage(Bitmap rawBitmap, int piece) {
        Bitmap[][] bitmapMatrix = new Bitmap[piece + 1][piece + 1];
        int unitHeight = rawBitmap.getHeight() / piece;
        int unitWidth = rawBitmap.getWidth() / piece;
        Bitmap unitBitmap;
        for (int i = 0; i < piece; i ++ ) {
            for (int j = 0; j < piece; j ++ ) {
                unitBitmap = Bitmap.createBitmap(rawBitmap, j * unitWidth, i * unitHeight, unitWidth, unitHeight);
                bitmapMatrix[i + 1][j + 1] = unitBitmap;
                savePNG_After(unitBitmap, i + "==" + j);
            }
        }
        return bitmapMatrix;
    }

    public static Mat matRotateClockWise90(Mat src)
    {
        // 矩阵转置
        //transpose(src, src);
        flip(src, src, 1);
        return src;
    }
}
