package com.example.digitalcare

import android.telecom.Call

object CallManager {
    var currentCall: Call? = null

    fun updateCall(call: Call?) {
        currentCall = call
    }

    fun answer() {
        currentCall?.answer(0)
    }

    fun hangup() {
        if (currentCall?.state == Call.STATE_RINGING) {
            currentCall?.reject(false, null)
        } else {
            currentCall?.disconnect()
        }
    }
}