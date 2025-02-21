// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.alibaba.mnnllm.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.mls.api.download.ModelDownloadManager;
import com.alibaba.mnnllm.android.chat.ChatActivity;
import com.alibaba.mnnllm.android.utils.FileUtils;
import com.alibaba.mnnllm.android.utils.ModelUtils;
import com.techiness.progressdialoglibrary.ProgressDialog;

import java.io.File;

/**
 * 判断模型是否已经成功拷贝到目标文件夹
 * 如果模型已经拷贝，直接跳转聊天界面
 * 如果模型未拷贝，执行拷贝逻辑，显示拷贝中的UI
 * 拷贝完成后跳转到聊天界面
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        String copiedPath = FileUtils.copyAssetsToAppStorage(this, "model");
        // 打印目标路径
        Log.d("FileUtil", "Assets copied to: " + copiedPath);
        runModel(copiedPath, "model", null);

    }


    public void runModel(String destModelDir, String modelName, String sessionId) {
        ModelDownloadManager.getInstance(this).pauseAllDownloads();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.model_loading));
        progressDialog.show();
        if (destModelDir == null) {
            destModelDir = ModelDownloadManager.getInstance(this).getDownloadPath(modelName).getAbsolutePath();
        }
        boolean isDiffusion = ModelUtils.isDiffusionModel(modelName);
        String configFilePath = null;
        if (!isDiffusion) {
            String configFileName = "config.json";
            configFilePath = destModelDir + "/" + configFileName;
            boolean configFileExists = new File(configFilePath).exists();
            if (!configFileExists) {
                Toast.makeText(this, getString(R.string.config_file_not_found, configFilePath), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                return;
            }
        }
        progressDialog.dismiss();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatSessionId", sessionId);
        if (isDiffusion) {
            intent.putExtra("diffusionDir", destModelDir);
        } else {
            intent.putExtra("configFilePath", configFilePath);
        }
        intent.putExtra("modelName", modelName);
        startActivity(intent);
        this.finish();
    }

}