package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.TVViewModel


class PlayerFragment : Fragment(), SurfaceHolder.Callback {

    private var _binding: PlayerBinding? = null
    private var playerView: PlayerView? = null
    private var tvViewModel: TVViewModel? = null
    private val aspectRatio = 16f / 9f


    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var exoPlayer: SimpleExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)

        if (Utils.isTmallDevice()) {
            _binding!!.playerView.visibility = View.GONE
            surfaceView = _binding!!.surfaceView
            surfaceHolder = surfaceView.holder
            surfaceHolder.addCallback(this)
        } else {
            _binding!!.surfaceView.visibility = View.GONE
            playerView = _binding!!.playerView
        }

        playerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                playerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                playerView!!.player = activity?.let { activityContext ->
                    // 大缓冲 + 宽松超时，抵抗免费源的网络抖动，减少误换源导致的黑屏卡顿
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)
                        .setAllowCrossProtocolRedirects(true)
                    val dataSourceFactory = DefaultDataSource.Factory(activityContext, httpFactory)
                    val loadControl = DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            45000,   // minBufferMs
                            90000,   // maxBufferMs
                            2500,    // bufferForPlaybackMs
                            5000     // bufferForPlaybackAfterRebufferMs
                        )
                        .setBackBuffer(25000, false)   // 回逰缓冲25秒，切台回看不重新加载
                        .build()
                    ExoPlayer.Builder(activityContext)
                        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                        .setLoadControl(loadControl)
                        .build()
                }
                playerView!!.player?.playWhenReady = true
                playerView!!.player?.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        val ratio = playerView?.measuredWidth?.div(playerView?.measuredHeight!!)
                        if (ratio != null) {
                            val layoutParams = playerView?.layoutParams
                            if (ratio < aspectRatio) {
                                layoutParams?.height =
                                    (playerView?.measuredWidth?.div(aspectRatio))?.toInt()
                                playerView?.layoutParams = layoutParams
                            } else if (ratio > aspectRatio) {
                                layoutParams?.width =
                                    (playerView?.measuredHeight?.times(aspectRatio))?.toInt()
                                playerView?.layoutParams = layoutParams
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)

                        Log.e(TAG, "PlaybackException $error")
                        tvViewModel?.retry()
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            (activity as MainActivity).isPlaying()
                        }
                    }
                })
            }
        })
        (activity as MainActivity).fragmentReady("PlayerFragment")
        return _binding!!.root
    }

    @OptIn(UnstableApi::class)
    fun play(tvViewModel: TVViewModel) {
        this.tvViewModel = tvViewModel
        playerView?.player?.run {
            setMediaItem(MediaItem.fromUri(tvViewModel.getVideoUrlCurrent()))
            prepare()
        }
        exoPlayer?.run {
            setMediaItem(com.google.android.exoplayer2.MediaItem.fromUri(tvViewModel.getVideoUrlCurrent()))
            prepare()
        }
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
        if (playerView != null && playerView!!.player?.isPlaying == false) {
            Log.i(TAG, "replay")
            playerView!!.player?.prepare()
            playerView!!.player?.play()
        }
        if (exoPlayer?.isPlaying == false) {
            Log.i(TAG, "replay")
            exoPlayer?.prepare()
            exoPlayer?.play()
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (playerView != null && playerView!!.player?.isPlaying == true) {
            playerView!!.player?.stop()
        }
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (playerView != null) {
            playerView!!.player?.release()
        }
        exoPlayer?.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        exoPlayer?.setVideoSurfaceHolder(surfaceHolder)
        exoPlayer?.playWhenReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}