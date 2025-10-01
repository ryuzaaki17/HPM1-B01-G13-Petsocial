package com.example.petsocial.ui.video

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment

class WebVideo : Fragment() {

    // ---------- Views ----------
    private lateinit var surfaceView: SurfaceView
    private lateinit var btnPickVideo: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnStop: Button
    private lateinit var btnRewind: Button
    private lateinit var btnForward: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var videoContainer: LinearLayout
    private lateinit var tvVideoStatus: TextView

    // ---------- Estado ----------
    private var currentVideoUri: Uri? = null
    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var isPrepared = false
    private var isSeekBarTracking = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // ---------- Colores UI ----------
    private val TEXT_DARK = Color.parseColor("#222222")
    private val LABEL_COLOR = Color.parseColor("#555555")
    private val HINT_COLOR = Color.parseColor("#9E9E9E")
    private val FIELD_BG = Color.parseColor("#F2F2F6")
    private val BG_WHITE = Color.parseColor("#FFFFFF")

    // Selector de video
    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            loadVideo(it)
        }
    }

    // ---------- UI ----------
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
            setBackgroundColor(BG_WHITE)
        }
        scroll.addView(content)

        // Título
        content.addView(TextView(requireContext()).apply {
            text = "Reproductor de Video"
            textSize = 15f
            setTextColor(LABEL_COLOR)
            setPadding(0, 0, 0, dp(16))
        })

        // ========== SECCIÓN DE VIDEO ==========
        content.addView(makeLabel("Video"))

        videoContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FIELD_BG)
            setPadding(dp(12))
        }
        content.addView(videoContainer, matchWrap())

        // SurfaceView para video
        surfaceView = SurfaceView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(250)
            )
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    surfaceHolder = holder
                    if (currentVideoUri != null && mediaPlayer != null) {
                        mediaPlayer?.setDisplay(holder)
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    surfaceHolder = null
                }
            })
        }
        videoContainer.addView(surfaceView)

        // Status del video
        tvVideoStatus = TextView(requireContext()).apply {
            text = "No hay video cargado"
            textSize = 14f
            setTextColor(HINT_COLOR)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        videoContainer.addView(tvVideoStatus)

        // SeekBar para progreso del video
        val seekBarContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        videoContainer.addView(seekBarContainer)

        tvCurrentTime = TextView(requireContext()).apply {
            text = "0:00"
            textSize = 12f
            setTextColor(LABEL_COLOR)
        }
        seekBarContainer.addView(tvCurrentTime)

        seekBar = SeekBar(requireContext()).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8)
                rightMargin = dp(8)
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && isPrepared) {
                        mediaPlayer?.let { mp ->
                            val duration = mp.duration
                            val newPosition = (duration * progress) / 100
                            tvCurrentTime.text = formatTime(newPosition)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarTracking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarTracking = false
                    mediaPlayer?.let { mp ->
                        if (isPrepared) {
                            val duration = mp.duration
                            val newPosition = (duration * seekBar!!.progress) / 100
                            mp.seekTo(newPosition)
                        }
                    }
                }
            })
        }
        seekBarContainer.addView(seekBar)

        tvTotalTime = TextView(requireContext()).apply {
            text = "0:00"
            textSize = 12f
            setTextColor(LABEL_COLOR)
        }
        seekBarContainer.addView(tvTotalTime)

        // Botones de control del video
        val controlsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        videoContainer.addView(controlsContainer)

        btnRewind = Button(requireContext()).apply {
            text = "⏪ -10s"
            isEnabled = false
            setOnClickListener { seekRelative(-10000) }
        }
        controlsContainer.addView(btnRewind)

        controlsContainer.addView(Space(requireContext()), LinearLayout.LayoutParams(dp(8), dp(8)))

        btnPlayPause = Button(requireContext()).apply {
            text = "▶ Play"
            isEnabled = false
            setOnClickListener { togglePlayPause() }
        }
        controlsContainer.addView(btnPlayPause)

        controlsContainer.addView(Space(requireContext()), LinearLayout.LayoutParams(dp(8), dp(8)))

        btnForward = Button(requireContext()).apply {
            text = "+10s ⏩"
            isEnabled = false
            setOnClickListener { seekRelative(10000) }
        }
        controlsContainer.addView(btnForward)

        // Botones de seleccionar y detener
        val extraControlsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        videoContainer.addView(extraControlsContainer)

        btnPickVideo = Button(requireContext()).apply {
            text = "Seleccionar Video"
            setOnClickListener {
                stopAndResetVideo()
                pickVideo.launch(arrayOf("video/*"))
            }
        }
        extraControlsContainer.addView(btnPickVideo)

        extraControlsContainer.addView(Space(requireContext()), LinearLayout.LayoutParams(dp(12), dp(12)))

        btnStop = Button(requireContext()).apply {
            text = "⏹ Detener"
            isEnabled = false
            setOnClickListener {
                if (isPrepared) {
                    stopVideo()
                } else {
                    Toast.makeText(requireContext(), "Espera a que el video termine de cargar", Toast.LENGTH_SHORT).show()
                }
            }
        }
        extraControlsContainer.addView(btnStop)

        return scroll
    }

    // ---------- Video Functions ----------
    private fun loadVideo(uri: Uri) {
        stopAndResetVideo()

        currentVideoUri = uri
        isPrepared = false
        isPlaying = false

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), uri)
                setDisplay(surfaceHolder)

                setOnPreparedListener { mp ->
                    isPrepared = true
                    btnPlayPause.isEnabled = true
                    btnStop.isEnabled = true
                    btnRewind.isEnabled = true
                    btnForward.isEnabled = true
                    tvVideoStatus.text = "Video listo ✓"
                    tvVideoStatus.setTextColor(Color.parseColor("#4CAF50"))

                    val duration = mp.duration
                    tvTotalTime.text = formatTime(duration)
                    seekBar.max = 100
                    updateProgress()

                    val videoWidth = mp.videoWidth
                    val videoHeight = mp.videoHeight
                    if (videoWidth > 0 && videoHeight > 0) {
                        val screenWidth = surfaceView.width
                        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                        val params = surfaceView.layoutParams
                        params.height = (screenWidth * aspectRatio).toInt()
                        surfaceView.layoutParams = params
                    }

                    Toast.makeText(requireContext(), "Video listo para reproducir", Toast.LENGTH_SHORT).show()
                }

                setOnCompletionListener {
                    this@WebVideo.isPlaying = false
                    btnPlayPause.text = "▶ Play"
                    tvVideoStatus.text = "Video finalizado"
                    seekTo(0)
                }

                setOnErrorListener { _, what, extra ->
                    if (what != MediaPlayer.MEDIA_ERROR_SERVER_DIED && what != -38) {
                        tvVideoStatus.text = "Error al cargar video"
                        tvVideoStatus.setTextColor(Color.RED)
                        btnPlayPause.isEnabled = false
                        Toast.makeText(requireContext(), "Error al reproducir video", Toast.LENGTH_SHORT).show()
                    }
                    what == MediaPlayer.MEDIA_ERROR_SERVER_DIED || what == -38
                }

                prepareAsync()
            }

            tvVideoStatus.text = "Cargando video..."
            tvVideoStatus.setTextColor(LABEL_COLOR)
            btnPlayPause.isEnabled = false
            btnStop.isEnabled = false
            btnRewind.isEnabled = false
            btnForward.isEnabled = false

        } catch (e: Exception) {
            tvVideoStatus.text = "Error: ${e.message}"
            tvVideoStatus.setTextColor(Color.RED)
            Toast.makeText(requireContext(), "Error al cargar video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (!isPrepared) {
                Toast.makeText(requireContext(), "Espera a que el video termine de cargar", Toast.LENGTH_SHORT).show()
                return
            }

            if (isPlaying) {
                mp.pause()
                btnPlayPause.text = "▶ Play"
                tvVideoStatus.text = "Video pausado"
                handler.removeCallbacksAndMessages(null)
            } else {
                mp.start()
                btnPlayPause.text = "⏸ Pause"
                tvVideoStatus.text = "Reproduciendo..."
                tvVideoStatus.setTextColor(LABEL_COLOR)
                updateProgress()
            }
            isPlaying = !isPlaying
        }
    }

    private fun stopVideo() {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                try {
                    if (isPlaying) {
                        mp.pause()
                    }
                    mp.seekTo(0)
                    isPlaying = false
                    btnPlayPause.text = "▶ Play"
                    tvVideoStatus.text = "Video detenido"
                    seekBar.progress = 0
                    tvCurrentTime.text = "0:00"
                    handler.removeCallbacksAndMessages(null)
                } catch (e: Exception) {}
            } else {
                tvVideoStatus.text = "Video cargando..."
                tvVideoStatus.setTextColor(HINT_COLOR)
            }
        }
    }

    private fun stopAndResetVideo() {
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            } catch (e: Exception) {}
        }
        mediaPlayer = null

        isPrepared = false
        isPlaying = false
        currentVideoUri = null

        seekBar.progress = 0
        tvCurrentTime.text = "0:00"
        tvTotalTime.text = "0:00"
        tvVideoStatus.text = "No hay video cargado"
        tvVideoStatus.setTextColor(HINT_COLOR)
        btnPlayPause.text = "▶ Play"
        btnPlayPause.isEnabled = false
        btnStop.isEnabled = false
        btnRewind.isEnabled = false
        btnForward.isEnabled = false
    }

    private fun seekRelative(milliseconds: Int) {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                val currentPosition = mp.currentPosition
                val newPosition = (currentPosition + milliseconds).coerceIn(0, mp.duration)
                mp.seekTo(newPosition)
                updateProgressNow()
            }
        }
    }

    private fun updateProgress() {
        if (!isPrepared) return

        handler.postDelayed({
            mediaPlayer?.let { mp ->
                if (isPlaying && !isSeekBarTracking) {
                    updateProgressNow()
                }
                if (isPlaying) {
                    updateProgress()
                }
            }
        }, 100)
    }

    private fun updateProgressNow() {
        mediaPlayer?.let { mp ->
            if (isPrepared) {
                val currentPosition = mp.currentPosition
                val duration = mp.duration
                if (duration > 0) {
                    val progress = (currentPosition * 100) / duration
                    seekBar.progress = progress
                    tvCurrentTime.text = formatTime(currentPosition)
                }
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun releaseMediaPlayer() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {}
        }
        mediaPlayer = null
        isPrepared = false
        isPlaying = false
        seekBar.progress = 0
        tvCurrentTime.text = "0:00"
        tvTotalTime.text = "0:00"
        btnPlayPause.isEnabled = false
        btnStop.isEnabled = false
        btnRewind.isEnabled = false
        btnForward.isEnabled = false
    }

    // ---------- Helpers UI ----------
    private fun makeLabel(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 14f
        setTextColor(LABEL_COLOR)
        setPadding(0, dp(12), 0, dp(6))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(4) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- Lifecycle ----------
    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            btnPlayPause.text = "▶ Play"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseMediaPlayer()
    }
}