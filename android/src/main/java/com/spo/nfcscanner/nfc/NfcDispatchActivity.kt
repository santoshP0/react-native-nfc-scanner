package com.spo.nfcscanner.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * Headless activity that receives NFC intents while the app is backgrounded.
 * It forwards the work to [NfcController] and immediately finishes to avoid UI flicker.
 */
class NfcDispatchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NfcController.init(this)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Forward the tag intent to the controller if dispatch is active.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        if (!NfcController.isDispatchEnabled()) {
            Log.d(TAG, "Dispatch disabled; ignoring tag intent.")
            finish()
            return
        }
        val handled = NfcController.handleIntent(intent)
        if (!handled) {
            Log.d(TAG, "Intent ignored: ${intent.action}")
        }
        finish()
    }

    companion object {
        private const val TAG = "OnCallNfcDispatch"
    }
}
