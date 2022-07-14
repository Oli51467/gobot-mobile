package com.irlab.base.utils;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    public static List<File> getFilesEndWithSameSuffix(File sgfPath, String suffix) {
        List<File> result = new ArrayList<>();
        File[] files = sgfPath.listFiles();
        if (files == null || files.length == 0) return result;
        for (File file : files) {
            if (file == null) continue;
            if (file.getName().endsWith(suffix)) {
                result.add(file);
            }
        }
        return result;
    }
}
