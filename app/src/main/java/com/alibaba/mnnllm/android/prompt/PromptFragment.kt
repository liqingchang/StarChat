package com.alibaba.mnnllm.android.prompt

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.mnnllm.android.R
import com.alibaba.mnnllm.android.utils.FileUtils

class PromptFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private lateinit var listener:OnPromptClick
    private var btnStartChat: Button? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 确保 Activity 实现了接口
        if (context is OnPromptClick) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnItemClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_prompt, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        btnStartChat = view.findViewById(R.id.btn_startchat)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val promptJsonArray = FileUtils.readAssetsJson(context, "prompts-zh.json")
        val items = mutableListOf<PromptItem>()
        if (promptJsonArray != null) {
            for (i in 0 until promptJsonArray.length()) {
                val jsonObject = promptJsonArray.getJSONObject(i)
                val act= jsonObject.getString("act")
                val prompt= jsonObject.getString("prompt")
                items.add(PromptItem(act, prompt))
            }
        }
        recyclerView?.layoutManager = GridLayoutManager(context, 2)
        recyclerView?.adapter = PromptAdapter(items) {
            listener.onPromptClick(it.prompt)
        }

        btnStartChat?.setOnClickListener {
            listener.onPromptClick(null)
        }
    }

    interface OnPromptClick {
        fun onPromptClick(prompt:String?)
    }

}