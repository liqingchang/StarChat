package com.alibaba.mnnllm.android.widgets;


import android.graphics.Color;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;

public class ThinkTagPlugin extends AbstractMarkwonPlugin {

    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("<think>(.*?)</think>");

    @Override
    public void afterSetText(@NonNull TextView textView) {
        final Spannable spannable = (Spannable) textView.getText();
        final String text = spannable.toString();

        // 使用正则表达式查找所有<think>标签
        Matcher matcher = THINK_TAG_PATTERN.matcher(text);
        while (matcher.find()) {
            String innerText = matcher.group(1); // 获取<think>标签中的内容
            int start = matcher.start(1);  // <think>标签中的内容起始位置
            int end = matcher.end(1);      // <think>标签中的内容结束位置

            // 应用缩小文字和变色的span
            spannable.setSpan(new AbsoluteSizeSpan(16, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(Color.GRAY), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
        // 这里可以进行其他插件配置
    }
}
