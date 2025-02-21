package com.alibaba.mnnllm.android.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.mnnllm.android.R
import com.alibaba.mnnllm.android.utils.KeyboardUtils
import com.alibaba.mnnllm.android.utils.ModelUtils
import java.util.Date
import kotlin.math.abs

class ChatFragment : Fragment() {

    private var layoutModelLoading: View? = null
    private var recyclerView: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    public var adapter: ChatRecyclerViewAdapter? = null
    private var modelName: String? = null
    private var path: String? = null
    private var viewModel: ChatViewModel? = null
    private var editUserMessage: EditText? = null
    private var buttonSend: ImageView? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var buttonSwitchVoice: View? = null
    public var chatSessionId: String? = null
    private var prompt:String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_chat_content, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        editUserMessage = view.findViewById(R.id.et_message)
        buttonSend = view.findViewById(R.id.bt_send)
        modelName = arguments?.getString("modelName")
        chatSessionId = arguments?.getString("chatSessionId")
        layoutModelLoading = view.findViewById(R.id.layout_model_loading);
        path = arguments?.getString("path")
        prompt = arguments?.getString("prompt")
        buttonSwitchVoice = view.findViewById(R.id.bt_switch_audio)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        viewModel?.init(context, modelName, path, chatSessionId, prompt)
        recyclerView?.setItemAnimator(null)
        linearLayoutManager = LinearLayoutManager(context)
        recyclerView?.setLayoutManager(linearLayoutManager)
        adapter = ChatRecyclerViewAdapter(context, viewModel?.initData(), modelName)
        recyclerView?.adapter = adapter

        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (abs(dy.toDouble()) > 0) {
                    viewModel?.isScrolling?.postValue(true)
                }
            }

            fun isUserScrolling(): Boolean {
                return viewModel?.isScrolling?.value == true
            }

        })

        editUserMessage!!.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                Log.d(
                    ChatActivity.TAG,
                    "onEditorAction" + actionId + "  getAction: " + event.action + "code: " + event.keyCode
                )
                sendUserMessage()
                return@setOnEditorActionListener true
            }
            false
        }
        editUserMessage!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                updateSenderButton()
                updateVoiceButtonVisibility()
            }
        })

        buttonSend?.isEnabled = false
        buttonSend?.setOnClickListener { view: View? -> handleSendClick() }

        viewModel?.currentUserMessage?.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                uiHandler.post {
                    updateAssistantResponse(it)
                }
            }
        })

        viewModel?.isLoading?.observe(viewLifecycleOwner) {
            uiHandler.post {
                updateSenderButton()
                layoutModelLoading?.setVisibility(if (it) View.VISIBLE else View.GONE)
            }
        }

        viewModel?.newSession?.observe(viewLifecycleOwner) {
            if(it == true) {
               adapter?.reset()
               Toast.makeText(context, R.string.new_conversation_started, Toast.LENGTH_LONG).show()
            } else {
               Toast.makeText(context, "Cannot Reset when generating", Toast.LENGTH_LONG).show()
            }
        }

        viewModel?.isGenerating?.observe(viewLifecycleOwner) {
            updateSenderButton()
        }

        smoothScrollToBottom()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel?.release()
    }

    private fun updateAssistantResponse(chatDataItem: ChatDataItem) {
        adapter!!.updateRecentItem(chatDataItem)
        if (viewModel?.isGenerating?.value == true) {
            scrollToEnd()
        }
    }

    private fun scrollToEnd() {
        recyclerView!!.postDelayed({
            val position = adapter!!.itemCount - 1
            linearLayoutManager!!.scrollToPositionWithOffset(position, -9999)
        }, 100)
    }

    fun sendUserMessage() {
        if (buttonSend?.isEnabled == false) {
            return
        }
        val inputString = editUserMessage?.text.toString().trim { it <= ' ' }
        viewModel?.sendUserMessage(inputString, adapter!!, object : ChatViewModel.SendUserMessageCallback {
            override fun callbackSec1(userData: ChatDataItem?) {
                updateSenderButton()
                editUserMessage?.setText("")
                adapter?.addItem(userData)
                addResponsePlaceholder()
            }

            override fun callbackSec2() {
                smoothScrollToBottom()
                KeyboardUtils.hideKeyboard(editUserMessage)
            }
        })
    }

    private fun handleSendClick() {
        if (viewModel?.isGenerating?.value == true) {
            viewModel?.stopGernerating?.postValue(true)
        } else {
            if ("" != editUserMessage?.text.toString()) {
                sendUserMessage()
            }
        }
    }

    private fun updateSenderButton() {
        var enabled = true
        if (viewModel?.isLoading?.value == true) {
            enabled = false
        } else if (viewModel?.currentUserMessage == null && TextUtils.isEmpty(editUserMessage!!.text.toString())) {
            enabled = false
        }
        if (viewModel?.isGenerating?.value == true) {
            enabled = true
        }
        buttonSend?.isEnabled = enabled
        buttonSend?.setImageResource(if (viewModel?.isGenerating?.value == false) R.drawable.button_send else R.drawable.ic_stop)
    }

    private fun addResponsePlaceholder() {
        adapter?.addItem(
            ChatDataItem(
                viewModel?.dateFormat?.format(Date()),
                ChatViewHolders.ASSISTANT,
                ""
            )
        )
        smoothScrollToBottom()
    }

    private fun smoothScrollToBottom() {
        Log.d(ChatActivity.TAG, "smoothScrollToBottom")
        recyclerView?.post {
            val position = if (adapter == null) 0 else adapter!!.itemCount - 1
            recyclerView?.scrollToPosition(position)
            recyclerView?.post { recyclerView?.scrollToPosition(position) }
        }
    }

    private fun updateVoiceButtonVisibility() {
        var visible = true
        if (!ModelUtils.isAudioModel(modelName)) {
            visible = false
        } else if (viewModel?.isGenerating?.value == true) {
            visible = false
        } else if (viewModel?.currentUserMessage?.value != null) {
            visible = false
        } else if (!TextUtils.isEmpty(editUserMessage!!.text.toString())) {
            visible = false
        }
        buttonSwitchVoice?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    public fun newSession(){
        viewModel?.handleNewSession()
    }

    // 伴生对象提供实例化方法
    companion object {
        fun newInstance(modelName: String, path: String, sessionId:String?, prompt:String?): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString("modelName", modelName)
            args.putString("path", path)
            if(sessionId != null) {
                args.putString("chatSessionId", sessionId)
            }
            if(prompt != null) {
                args.putString("prompt", prompt)
            }
            fragment.arguments = args
            return fragment
        }
    }


}