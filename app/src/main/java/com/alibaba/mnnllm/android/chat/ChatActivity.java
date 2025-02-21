// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.alibaba.mnnllm.android.chat;

import static com.alibaba.mnnllm.android.chat.VoiceRecordingModule.REQUEST_RECORD_AUDIO_PERMISSION;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.alibaba.mls.api.download.ModelDownloadManager;
import com.alibaba.mnnllm.android.R;
import com.alibaba.mnnllm.android.history.ChatHistoryFragment;
import com.alibaba.mnnllm.android.prompt.PromptFragment;
import com.alibaba.mnnllm.android.utils.ModelUtils;
import com.alibaba.mnnllm.android.utils.PreferenceUtils;
import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class ChatActivity extends AppCompatActivity implements PromptFragment.OnPromptClick {

    private DrawerLayout drawerLayout;
    private ImageView imageMore;

    public static final String TAG = "ChatActivity";
    private boolean isAudioModel = false;
    private AttachmentPickerModule attachmentPickerModule;
    private View buttonSwitchVoice;

    private ChatDataItem currentUserMessage;

    private boolean isGenerating = false;
    private boolean isLoading = false;
    private String sessionName;
    private boolean stopGenerating = false;

    private ChatHistoryFragment chatHistoryFragment;

    private ActionBarDrawerToggle toggle;

    private ChatFragment chatFragment;

    private String modelName;

    private String appName;

    private PromptFragment promptFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Set up ActionBar toggle
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                toolbar,
                R.string.nav_open,
                R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                if (chatHistoryFragment != null) {
                    chatHistoryFragment.onLoad();
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        toggle.syncState();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.history_fragment_container,
                        getChatHistoryFragment())
                .commit();

        modelName = getIntent().getStringExtra("modelName");
        appName = getString(R.string.app_name);

        boolean isHistory = getIntent().getBooleanExtra("isHistory", false);
        String prompt = getIntent().getStringExtra("prompt");

        if (isHistory) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_chat,
                            getChatFragment(prompt))
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_chat,
                            getPromptFragment())
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.start_new_chat) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container_chat);
            if(currentFragment instanceof PromptFragment) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_chat,
                                getChatFragment(null))
                        .commit();
            }
            if (chatFragment != null) {
                chatFragment.newSession();
            }
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.prompt_home) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_chat,
                            getPromptFragment())
                    .commit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                voiceRecordingModule.handlePermissionAllowed();
//            } else {
//                voiceRecordingModule.handlePermissionDenied();
//            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (attachmentPickerModule != null && attachmentPickerModule.canHandleResult(requestCode)) {
            attachmentPickerModule.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public String getSessionId() {
        return "";
    }

    public String getModelName() {
        return modelName;
    }

    private Fragment getChatHistoryFragment() {
        if (chatHistoryFragment == null) {
            chatHistoryFragment = new ChatHistoryFragment();
        }
        return chatHistoryFragment;
    }

    public void runModel(String destModelDir, String modelName, String sessionId, String prompt) {
        ModelDownloadManager.getInstance(this).pauseAllDownloads();
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
                return;
            }
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatSessionId", sessionId);
        if (isDiffusion) {
            intent.putExtra("diffusionDir", destModelDir);
        } else {
            intent.putExtra("configFilePath", configFilePath);
        }
        intent.putExtra("modelName", modelName);
        intent.putExtra("isHistory", true);
        if(prompt != null) {
            intent.putExtra("prompt", prompt);
        }
        startActivity(intent);
        this.finish();
    }

    private Fragment getChatFragment(String prompt) {
        String path = "";
        if (ModelUtils.isDiffusionModel(modelName)) {
            path = getIntent().getStringExtra("diffusionDir");
        } else {
            path = getIntent().getStringExtra("configFilePath");
        }
        String chatSessionId = getIntent().getStringExtra("chatSessionId");
        chatFragment = ChatFragment.Companion.newInstance(modelName, path, chatSessionId, prompt);
        return chatFragment;
    }

    private Fragment getPromptFragment() {
        if (promptFragment == null) {
            promptFragment = new PromptFragment();
        }
        return promptFragment;
    }

    @Override
    public void onPromptClick(@NonNull String prompt) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_chat,
                        getChatFragment(prompt))
                .commit();
    }
}