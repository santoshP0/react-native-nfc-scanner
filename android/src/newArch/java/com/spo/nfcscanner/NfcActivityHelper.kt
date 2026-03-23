package com.spo.nfcscanner

import android.app.Activity
import android.content.Intent
import com.spo.nfcscanner.nfc.NfcController

/**
 * Public helper for wiring NFC into your MainActivity lifecycle.
 *
 * Add calls to each override in your MainActivity — see the README for details.
 */
object NfcActivityHelper {

    /** Call from MainActivity.onCreate() */
    fun onCreate(activity: Activity) {
        NfcController.init(activity)
    }

    /** Call from MainActivity.onResume() — enables foreground Reader Mode */
    fun onResume(activity: Activity) {
        NfcController.enableReaderMode(activity)
    }

    /** Call from MainActivity.onPause() — disables foreground Reader Mode */
    fun onPause(activity: Activity) {
        NfcController.disableReaderMode(activity)
    }

    /** Call from MainActivity.onStop() — cleans up dispatch when app is closing */
    fun onStop(activity: Activity) {
        if (activity.isFinishing || activity.isChangingConfigurations) {
            NfcController.disableDispatchHandling()
        }
    }

    /** Call from MainActivity.onNewIntent() — routes background NFC intents */
    fun onNewIntent(intent: Intent?) {
        NfcController.handleIntent(intent)
    }
}
