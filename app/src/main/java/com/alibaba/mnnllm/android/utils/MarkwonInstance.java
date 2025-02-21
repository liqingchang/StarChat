package com.alibaba.mnnllm.android.utils;

import android.content.Context;

import com.alibaba.mnnllm.android.widgets.ThinkTagPlugin;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.html.HtmlPlugin;

public class MarkwonInstance {
    private static MarkwonInstance instance = new MarkwonInstance();
    private Markwon markwon;

    public void init(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(new ThinkTagPlugin())
                .build();
    }

    public static MarkwonInstance getInstance() {
        return instance;
    }

    public Markwon getMarkwon(){
        return markwon;
    }


}
