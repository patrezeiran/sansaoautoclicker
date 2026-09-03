package com.sansao.autoclicker

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var panelView: View
    private var overlayRecorderView: View? = null

    private lateinit var btnRec: Button
    private lateinit var btnStop: Button
    private lateinit var btnPlay: Button

    private val recordedSteps = mutableListOf<ClickStep>()
    private var lastEventTime = 0L
    private var isRecording = false
    private var isPlaying = false

    private val playHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingPanel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.layout_floating_panel, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val btnDrag = panelView.findViewById<TextView>(R.id.btnDrag)
        btnRec = panelView.findViewById(R.id.btnRec)
        btnStop = panelView.findViewById(R.id.btnStop)
        btnPlay = panelView.findViewById(R.id.btnPlay)
        val btnClose = panelView.findViewById<TextView>(R.id.btnClose)

        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        btnDrag.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchStartX).toInt()
                    params.y = initialY + (event.rawY - touchStartY).toInt()
                    windowManager.updateViewLayout(panelView, params)
                    true
                }
                else -> false
            }
        }

        btnRec.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopActions() }
        btnPlay.setOnClickListener { startPlayback() }
        btnClose.setOnClickListener { stopSelf() }

        windowManager.addView(panelView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startRecording() {
        isRecording = true
        recordedSteps.clear()
        lastEventTime = System.currentTimeMillis()

        btnRec.isEnabled = false
        btnStop.isEnabled = true
        btnPlay.isEnabled = false

        Toast.makeText(this, "Gravando toques... Execute suas ações na tela.", Toast.LENGTH_SHORT).show()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Cria uma tela transparente para capturar os toques
        val recordParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayRecorderView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    val delay = if (recordedSteps.isEmpty()) 0L else (now - lastEventTime)
                    lastEventTime = now

                    recordedSteps.add(ClickStep(event.rawX, event.rawY, delay))
                }
                false
            }
        }

        windowManager.addView(overlayRecorderView, recordParams)
    }

    private fun stopActions() {
        if (isRecording) {
            isRecording = false
            overlayRecorderView?.let {
                windowManager.removeView(it)
                overlayRecorderView = null
            }
            Toast.makeText(this, "Gravação parada! Toques gravados: ${recordedSteps.size}", Toast.LENGTH_SHORT).show()
        }

        if (isPlaying) {
            isPlaying = false
            playHandler.removeCallbacksAndMessages(null)
            Toast.makeText(this, "Reprodução interrompida.", Toast.LENGTH_SHORT).show()
        }

        btnRec.isEnabled = true
        btnStop.isEnabled = false
        btnPlay.isEnabled = recordedSteps.isNotEmpty()
    }

    private fun startPlayback() {
        if (recordedSteps.isEmpty()) return

        isPlaying = true
        btnRec.isEnabled = false
        btnStop.isEnabled = true
        btnPlay.isEnabled = false

        Toast.makeText(this, "Iniciando reprodução...", Toast.LENGTH_SHORT).show()
        executeStep(0)
    }

    private fun executeStep(index: Int) {
        if (!isPlaying) return

        if (index >= recordedSteps.size) {
            stopActions()
            Toast.makeText(this, "Reprodução finalizada!", Toast.LENGTH_SHORT).show()
            return
        }

        val step = recordedSteps[index]
        playHandler.postDelayed({
            if (!isPlaying) return@postDelayed

            AutoClickerService.instance?.dispatchClick(step.x, step.y)
            executeStep(index + 1)
        }, step.delayBeforeMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopActions()
        if (::panelView.isInitialized) {
            windowManager.removeView(panelView)
        }
    }
}
