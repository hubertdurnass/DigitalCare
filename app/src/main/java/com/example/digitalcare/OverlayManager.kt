package com.example.digitalcare

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.view.WindowManager.LayoutParams
import java.lang.ref.WeakReference

object OverlayManager {

    private var overlayViewRef: WeakReference<View>? = null

    fun showOverlay(
        context: Context,
        name: String,
        number: String,
        onAnswer: () -> Unit,
        onDecline: () -> Unit
    ) {
        if (overlayViewRef?.get() != null) return

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.activity_incoming_call, null, false)

        val tvIncomingName = view.findViewById<TextView>(R.id.tvIncomingName)
        val tvIncomingNumber = view.findViewById<TextView>(R.id.tvIncomingNumber)
        val btnAnswer = view.findViewById<Button>(R.id.btnAnswer)
        val btnDecline = view.findViewById<Button>(R.id.btnDecline)

        tvIncomingName.text = name
        tvIncomingNumber.text = number

        btnAnswer.setOnClickListener {
            removeOverlay(context)
            onAnswer()
        }

        btnDecline.setOnClickListener {
            removeOverlay(context)
            onDecline()
        }

        val windowType = LayoutParams.TYPE_APPLICATION_OVERLAY
        val windowFlags = (
                LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        LayoutParams.FLAG_DISMISS_KEYGUARD or
                        LayoutParams.FLAG_TURN_SCREEN_ON or
                        LayoutParams.FLAG_KEEP_SCREEN_ON or
                        LayoutParams.FLAG_LAYOUT_IN_SCREEN
                )
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            windowType,
            windowFlags,
            PixelFormat.TRANSLUCENT
        )

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(view, params)

        overlayViewRef = WeakReference(view)
    }

    fun removeOverlay(context: Context? = null) {
        overlayViewRef?.get()?.let { view ->
            try {
                val wm = context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeView(view)
            } catch (_: Exception) {
            } finally {
                overlayViewRef?.clear()
                overlayViewRef = null
            }
        }
    }
}