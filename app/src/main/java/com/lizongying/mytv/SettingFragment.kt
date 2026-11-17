package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.SettingBinding


class SettingFragment : DialogFragment() {

    private var _binding: SettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateManager: UpdateManager

    private fun videoHeightText(h: Int): String = when (h) {
        1080 -> "清晰度: 最高1080P"
        720 -> "清晰度: 最高720P"
        else -> "清晰度: 不限"
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext() // It‘s safe to get context here.
        _binding = SettingBinding.inflate(inflater, container, false)
        binding.versionName.text = "当前版本: v${context.appVersionName}"
        binding.version.text = "https://github.com/lizongying/my-tv"

        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, isChecked ->
                SP.bootStartup = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.videoHeight.text = videoHeightText(SP.videoHeight)
        binding.videoHeight.setOnClickListener {
            SP.videoHeight = when (SP.videoHeight) {
                0 -> 1080
                1080 -> 720
                else -> 0
            }
            binding.videoHeight.text = videoHeightText(SP.videoHeight)
            (activity as MainActivity).applyVideoHeight()
            (activity as MainActivity).settingDelayHide()
        }

        updateManager = UpdateManager(context, this, context.appVersionCode)
        binding.checkVersion.setOnClickListener(
            OnClickListenerCheckVersion(
                activity as MainActivity,
                updateManager
            )
        )

        binding.exit.setOnClickListener{
            requireActivity().finishAffinity()
        }

        return binding.root
    }

    fun setVersionName(versionName: String) {
        if (_binding != null) {
            binding.versionName.text = versionName
        }
    }

    internal class OnClickListenerCheckVersion(
        private val mainActivity: MainActivity,
        private val updateManager: UpdateManager
    ) :
        View.OnClickListener {
        override fun onClick(view: View?) {
            mainActivity.settingDelayHide()
            updateManager.checkAndUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

