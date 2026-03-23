package com.spo.nfcscanner.nfc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Lightweight service whose sole purpose is to observe task removal events so NFC dispatch
 * can be disabled before the process is terminated.
 */
public class NfcLifecycleGuardService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We do not need to restart automatically if the system kills the process.
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        NfcController.getInstance().disableDispatchHandling();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
