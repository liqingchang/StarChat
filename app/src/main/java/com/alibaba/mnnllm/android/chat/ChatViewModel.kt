package com.alibaba.mnnllm.android.chat

import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.alibaba.mnnllm.android.ChatService
import com.alibaba.mnnllm.android.ChatSession
import com.alibaba.mnnllm.android.R
import com.alibaba.mnnllm.android.utils.ModelUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


class ChatViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * true 成功开启新Session
     * false 模型正在生成内容，无法开启新Session
     */
    public var newSession: MutableLiveData<Boolean> = MutableLiveData()
    public var isLoading: MutableLiveData<Boolean> = MutableLiveData()
    public var isScrolling: MutableLiveData<Boolean> = MutableLiveData()
    public var isGenerating: MutableLiveData<Boolean> = MutableLiveData()
    public var stopGernerating: MutableLiveData<Boolean> = MutableLiveData()
    public var modelName: MutableLiveData<String> = MutableLiveData()
    public var dateFormat: DateFormat? = null
    private var openingFormat: String? = null

    private var chatSession: ChatSession? = null

    // todo: init
    public var chatSessionId: String? = null

    private var chatDataManager: ChatDataManager? = null
    public var sessionName: String? = null

    private var isDiffusionModel: Boolean? = false

    private var path: String? = null

    private var chatExecutor: ScheduledExecutorService? = null

    public var currentUserMessage: MutableLiveData<ChatDataItem> = MutableLiveData()
    public var appName: String? = null
    private var prompt: String? = null

    fun init(
        context: Context?,
        modelName: String?,
        path: String?,
        chatSessionId: String?,
        prompt: String?
    ) {
        Log.i("ChatViewModel", path + "")
        Log.i("ChatViewModel", modelName + "")
        this.modelName.value = modelName
        this.path = path
        this.dateFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        this.appName = context?.getString(R.string.app_name)
        this.chatSessionId = chatSessionId
        this.prompt = prompt
        isGenerating.value = false
        isDiffusionModel = ModelUtils.isDiffusionModel(modelName)
        openingFormat =
            context?.getString(if (ModelUtils.isDiffusionModel(modelName)) R.string.model_hello_prompt_diffusion else R.string.model_hello_prompt)
        if(prompt != null) {
            openingFormat = prompt
        }
        chatDataManager = ChatDataManager.getInstance(context)
        chatExecutor = Executors.newScheduledThreadPool(1)
        setupSession()
    }


    fun initData(): MutableList<ChatDataItem> {
        val data: MutableList<ChatDataItem> = ArrayList()
        data.add(ChatDataItem(dateFormat?.format(Date()), ChatViewHolders.USER, openingFormat))
//        data.add(ChatDataItem(dateFormat?.format(Date()), ChatViewHolders.ASSISTANT, "好的"))
        val savedHistory: List<ChatDataItem>? = chatSession?.getSavedHistory()
        if (!savedHistory.isNullOrEmpty()) {
            data.addAll(savedHistory)
        }
        return data
    }

    private fun setupSession() {
        val chatService: ChatService = ChatService.provide()
        val chatDataItemList: List<ChatDataItem>?
        if (!TextUtils.isEmpty(chatSessionId)) {
            chatDataItemList = chatDataManager?.getChatDataBySession(chatSessionId)
            if (!chatDataItemList.isNullOrEmpty()) {
                sessionName = chatDataItemList[0].text
            }
        } else {
            chatDataItemList = null
        }
        if (ModelUtils.isDiffusionModel(modelName.value)) {
            chatSession = chatService.createDiffusionSession(path, chatSessionId, chatDataItemList)
        } else {
            chatSession = chatService.createSession(path, true, chatSessionId, chatDataItemList, prompt)
        }
        chatSessionId = chatSession?.sessionId
        chatSession?.setKeepHistory(
            !ModelUtils.isVisualModel(modelName.value) && !ModelUtils.isAudioModel(
                modelName.value
            )
        )
        chatExecutor?.submit(java.lang.Runnable {
            isLoading.postValue(true)
            chatSession?.load()
            isLoading.postValue(false)
        })
    }

    fun sendUserMessage(
        inputString: String,
        adapter: ChatRecyclerViewAdapter,
        callback: SendUserMessageCallback
    ) {
        if (currentUserMessage.value == null) {
            currentUserMessage.value = ChatDataItem(ChatViewHolders.USER)
        }
        currentUserMessage.value?.text = inputString
        currentUserMessage.value?.time = dateFormat?.format(Date())
        isGenerating.postValue(true)
        callback.callbackSec1(currentUserMessage.value)
        // 这里暂时用这种方式，注意是callback的Sec1中在adapter增加数据后再在内部重新获取Item
//        val recentItem = adapter.recentItem;
        var input = ""
        val hasSessionName = !TextUtils.isEmpty(sessionName)
        var sessionName: String? = null
        input = currentUserMessage.value?.text.toString()
        if (!hasSessionName) {
            sessionName = currentUserMessage.value?.text
        }
        if (!hasSessionName) {
            chatDataManager?.addOrUpdateSession(chatSessionId, modelName.value, prompt)
            sessionName =
                if (sessionName!!.length > 100) sessionName!!.substring(0, 100) else sessionName
            chatDataManager?.updateSessionName(this.chatSessionId, sessionName)
        }
        if (isDiffusionModel == true) {
            chatExecutor?.execute(Runnable {
                val recentData = adapter.recentItem
                submitRequest(input, recentData)
            })
        } else {
            chatExecutor?.execute(Runnable {
                val recentData = adapter.recentItem
                submitRequest(input, recentData)
            })
        }
        chatDataManager?.addChatData(chatSessionId, currentUserMessage.value)
        callback.callbackSec2()
        currentUserMessage.value = null
    }

    private fun submitRequest(input: String, chatDataItem: ChatDataItem) {
        isScrolling.postValue(false)
        stopGernerating.postValue(false)
        val stringBuilder = StringBuilder()
        val benchMarkResult =
            chatSession?.generate(input, object : ChatSession.GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    if (progress != null) {
                        stringBuilder.append(progress)
                        chatDataItem.text = stringBuilder.toString()
                        currentUserMessage.postValue(chatDataItem)
                    }
                    return stopGernerating.value == true
                }
            })

        val finalBenchMarkResult = benchMarkResult!!
        chatDataItem.benchmarkInfo = ModelUtils.generateBenchMarkString(finalBenchMarkResult)
        currentUserMessage.postValue(chatDataItem)
        chatDataManager?.addChatData(chatSessionId, chatDataItem)
        isGenerating.postValue(false)
    }

    public fun handleNewSession() {
        if (isGenerating.value == false) {
            currentUserMessage.postValue(null)
            chatSessionId = chatSession!!.generateNewSession()
            sessionName = null
            chatExecutor!!.execute { chatSession!!.reset() }
            chatDataManager!!.deleteAllChatData(chatSessionId)
            newSession.postValue(true)
        } else {
            newSession.postValue(false)
        }
    }

    public fun release() {
        chatExecutor?.submit {
            chatSession?.reset()
            chatSession?.release()
            chatExecutor?.shutdownNow()
        }
    }

    interface SendUserMessageCallback {
        fun callbackSec1(userData: ChatDataItem?)
        fun callbackSec2()
    }

}