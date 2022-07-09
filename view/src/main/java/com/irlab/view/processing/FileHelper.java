package com.irlab.view.processing;

import android.os.Environment;
import android.util.Log;

import com.irlab.view.models.Game;
import com.irlab.view.processing.cornerDetector.Corner;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FileHelper {

    private String gameName;
    private File gameRecordFolder;
    //private File gameRecordLogFolder;
    private File gameFile;

    public FileHelper(Game game) {
        gameName = generateGameName(game);
        gameRecordFolder = new File(Environment.getExternalStorageDirectory() + "/archive_recorder");
        gameFile = getGameFile();
    }

    private String generateGameName(Game game) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd-HH:mm");
        String timestamp = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));

        return timestamp + "_" + game.getWhitePlayer() + "-" + game.getBlackPlayer();
    }

    private File getGameFile() {
        File file = new File(gameRecordFolder, generateFilename(0, "", "sgf"));
        int counter = 1;

        while (file.exists()) {
            String newFilename = generateFilename(counter, "", "sgf");
            file = new File(gameRecordFolder, newFilename);
            counter++;
        }

        return file;
    }

    public File getFile(String name, String extension) {
        File file = new File(gameRecordFolder, generateFilename(0, name, extension));
        int counter = 1;

        while (file.exists()) {
            String newFilename = generateFilename(counter, name, extension);
            file = new File(gameRecordFolder, newFilename);
            counter++;
        }

        return file;
    }

    private String generateFilename(int repeatedNameCounter, String filename, String extension) {
        String counter = repeatedNameCounter > 0 ?
            "(" + repeatedNameCounter + ")" : "";

        StringBuilder string = new StringBuilder();
        string.append(gameName);
        if (!filename.isEmpty()) {
            string.append("_").append(filename);
        }
        string.append(counter).append(".").append(extension);
        return string.toString();
    }

    public File getTempFile() {
        return new File(gameRecordFolder, "temp_file");
    }

    public boolean saveGameFile(Game game) {
        String gameContent = game.sgf();

        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(gameFile, false);
                fos.write(gameContent.getBytes());
                fos.flush();
                fos.close();

                Log.i("Recorder", "Game saved: " + gameFile.getName());
                return true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void storeGameTemporarily(Game game, Corner[] corners) {
        File file = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(file, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(game);
                oos.writeObject(corners);
                oos.close();
                fos.close();
                Log.i("Recorder", "Game temporarily saved in " + file.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("Recorder", "External storage not available to store temporary game state.");
        }
    }

    public void restoreGameStoredTemporarily(Game game, Corner[] boardCorners) {
        File file = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);

                game.copy((Game) ois.readObject());
                Corner[] savedCorners = (Corner[]) ois.readObject();
                for (int i = 0; i < 4; i++) {
                    boardCorners[i].copy(savedCorners[i]);
                }

                ois.close();
                fis.close();
                Log.i("Recorder", "恢复游戏");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("Recorder", "External storage not available to restore temporary game state.");
        }
    }

    public void writePngImage(Mat image, int colorConversionCode, String filename) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, colorConversionCode);
        Imgcodecs.imwrite(getFile(filename, "png").getAbsolutePath(), correctColorFormatImage);
    }

    public void writePngImage(Mat image, String filename) {
        Imgcodecs.imwrite(getFile(filename, "png").getAbsolutePath(), image);
    }
}
