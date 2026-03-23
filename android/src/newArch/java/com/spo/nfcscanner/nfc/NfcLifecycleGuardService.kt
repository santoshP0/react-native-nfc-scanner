package com.spo.nfcscanner.nfc

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Lightweight service whose sole purpose is to observe task removal events so NFC dispatch
 * can be disabled before the process is terminated.
 */
class NfcLifecycleGuardService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // We do not need to restart automatically if the system kills the process.
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        NfcController.disableDispatchHandling()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
