// Created by ruoyi.sjd on 2025/1/10.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.alibaba.mnnllm.android.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileUtils {

    public static final String TAG = "FileUtils";
    public static long getAudioDuration(String audioFilePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(audioFilePath);
            String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Long.parseLong(durationStr) / 1000 : -1;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return 1;
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    public static String generateDestDiffusionFilePath(Context context, String sessionId) {
        return generateDestFilePathKindOf(context, sessionId, "diffusion", "jpg");
    }

    public static String generateDestDiffusionFilePath(String dirPath, String sessionId) {
        return generateDestFilePathKindOf(dirPath, sessionId, "diffusion", "jpg");
    }

    public static String generateDestPhotoFilePath(Context context, String sessionId) {
        return generateDestFilePathKindOf(context, sessionId, "photo", "jpg");
    }

    public static String generateDestAudioFilePath(Context context, String sessionId) {
        return generateDestFilePathKindOf(context, sessionId, "audio", "wav");
    }

    public static String generateDestRecordFilePath(Context context, String sessionId) {
        return generateDestFilePathKindOf(context, sessionId, "record", "wav");
    }

    public static String generateDestImageFilePath(Context context, String sessionId) {
        return generateDestFilePathKindOf(context, sessionId, "image", "jpg");
    }

    private static String generateDestFilePathKindOf(Context context, String sessionId, String kind, String extension) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + sessionId + "/" + kind + "_" + System.currentTimeMillis() + "." + extension;
        ensureParentDirectoriesExist(new File(path));
        return path;
    }

    private static String generateDestFilePathKindOf(String dirPath ,String sessionId, String kind, String extension) {
        String path = dirPath + "/" + sessionId + "/" + kind + "_" + System.currentTimeMillis() + "." + extension;
        ensureParentDirectoriesExist(new File(path));
        return path;
    }

    public static String getSessionResourceBasePath(Context context, String sessionId) {
        return context.getFilesDir().getAbsolutePath() + "/" + sessionId;
    }

    public static void ensureParentDirectoriesExist(File file) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public static File copyFileUriToPath(Context context, Uri fileUri, String destFilePath) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Open an InputStream from the Uri
            inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                throw new IllegalArgumentException("Unable to open InputStream from Uri");
            }
            // Create the destination file
            File destinationFile = new File(destFilePath);
            ensureParentDirectoriesExist(destinationFile);
            outputStream = Files.newOutputStream(destinationFile.toPath());

            // Buffer for data transfer
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            return destinationFile;
        }  finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {}
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 将 assets 文件夹中的文件拷贝到应用的外部存储目录，并返回目标路径
     */
    public static String copyAssetsToAppStorage(Context context, String assetDir) {
        AssetManager assetManager = context.getAssets();

        // 获取目标目录路径
        String targetDir = context.getFilesDir().getAbsolutePath() + "/.mnnmodels";
        String targetPath = targetDir + "/" + assetDir;
        File targetFolder = new File(targetPath);

        // 如果目标文件夹不存在，创建该文件夹
        if (!targetFolder.exists()) {
            if (targetFolder.mkdirs()) {
                Log.d("AssetUtil", "Directory created: " + targetDir);
            } else {
                Log.e("AssetUtil", "Failed to create directory: " + targetDir);
                return null;
            }
        } else {
            return targetPath;
        }

        try {
            // 获取 assets/model 文件夹中的所有文件
            String[] files = assetManager.list(assetDir);
            if (files != null) {
                for (String file : files) {
                    // 创建目标文件路径
                    String targetFilePath =  targetPath + "/" + file;
                    File targetFile = new File(targetFilePath);

                    // 如果文件是文件夹，则递归复制
                    if (file.endsWith("/")) {
                        targetFile.mkdirs();
                        // 递归调用复制文件夹中的文件
                        copyAssetsToAppStorage(context, assetDir + "/" + file);
                    } else {
                        // 如果是文件，复制文件
                        copyFileFromAssets(context, assetDir + "/" + file, targetFilePath);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("AssetUtil", "Error copying assets: ", e);
        }

        return targetPath;
    }

    /**
     * 将 assets 文件夹中的文件拷贝到 SD 卡，并返回目标路径
     */
    public static String copyAssetsToSdCard(Context context, String assetDir, String targetDir) {
        AssetManager assetManager = context.getAssets();
        String targetDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + targetDir;
        try {
            // 获取 assets/model 文件夹中的所有文件
            String[] files = assetManager.list(assetDir);
            if (files != null) {
                for (String file : files) {
                    // 创建目标文件路径
                    String targetFilePath = targetDirectoryPath + "/" + file;
                    File targetFile = new File(targetFilePath);

                    // 如果文件是文件夹，则递归复制
                    if (file.endsWith("/")) {
                        targetFile.mkdirs();
                        // 递归调用复制文件夹中的文件
                        copyAssetsToSdCard(context, assetDir + "/" + file, targetDir + "/" + file);
                    } else {
                        // 如果是文件，复制文件
                        copyFileFromAssets(context, assetDir + "/" + file, targetFilePath);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("AssetUtil", "Error copying assets: ", e);
        }

        return targetDirectoryPath;  // 返回拷贝后的目标路径
    }

    /**
     * 复制单个文件
     */
    private static void copyFileFromAssets(Context context, String assetPath, String targetPath) {
        AssetManager assetManager = context.getAssets();
        try (InputStream in = assetManager.open(assetPath);
             OutputStream out = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Log.e("AssetUtil", "Error copying file: " + assetPath, e);
        }
    }

    public static JSONArray readAssetsJson(Context context, String fileName) {
        JSONArray jsonArray = null;
        InputStream inputStream = null;
        try {
            // 获取AssetManager
            AssetManager assetManager = context.getAssets();

            // 打开文件并读取内容
            inputStream = assetManager.open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);

            // 将字节数组转换为字符串
            String jsonString = new String(buffer, "UTF-8");

            // 将字符串转换为JSONArray
            jsonArray = new JSONArray(jsonString);
        } catch (IOException e) {
            Log.e("JsonUtils", "Error reading from assets", e);
        } catch (JSONException e) {
            Log.e("JsonUtils", "Error parsing JSON", e);
        } finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            inputStream = null;
        }
        return jsonArray;
    }


}

