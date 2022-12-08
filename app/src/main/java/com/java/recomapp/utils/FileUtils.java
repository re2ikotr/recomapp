package com.java.recomapp.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FileUtils {
    public final static String TAG = "FileUtils";
    public static void makeDir(String directory) {
        try {
            File file = new File(directory);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception ignored) {
        }
    }

    public static File makeFile(File file) {
        try {
            makeDir(file.getParent());
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception ignored) {
        }
        return file;
    }

    public static void writeStringToFile(String content, File saveFile) {
        writeStringToFile(content, saveFile, false);
    }

    public static void writeStringToFile(String content, File saveFile, boolean append) {
        makeFile(saveFile);
        String toWrite = content + "\r\n";
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(saveFile, append));
            writer.write(toWrite);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(File src, File dst) {
        try {
            InputStream in = new FileInputStream(src);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileContent(String filename) {
        Log.e(TAG, "Get file content " + filename);
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    public static void deleteFile(File file, String tag) {
        if (file.delete()) {
            Log.d("FileUtils", "[" + tag + "] Delete " + file.getAbsolutePath() + " successfully");
        } else {
            Log.d("FileUtils", "[" + tag + "] Failed to delete " + file.getAbsolutePath());
        }
    }
}
