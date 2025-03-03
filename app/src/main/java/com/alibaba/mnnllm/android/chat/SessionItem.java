// Created by ruoyi.sjd on 2025/1/13.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.alibaba.mnnllm.android.chat;

public class SessionItem {
    private final String sessionId;
    private final String modelId;
    private String title;

    private String prompt;

    public SessionItem(String sessionId, String modelId, String title, String prompt) {
        this.sessionId = sessionId;
        this.modelId = modelId;
        this.title = title;
        this.prompt = prompt;
    }


    public String getPrompt() {
        return prompt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getModelId() {
        return modelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}