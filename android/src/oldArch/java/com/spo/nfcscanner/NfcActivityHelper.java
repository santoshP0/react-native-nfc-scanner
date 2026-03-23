package com.spo.nfcscanner;

import android.app.Activity;
import android.content.Intent;
import com.spo.nfcscanner.nfc.NfcController;

/**
 * Public helper for wiring NFC into your MainActivity lifecycle.
 *
 * Add calls to each override in your MainActivity — see the README for details.
 */
public class NfcActivityHelper {

    /** Call from MainActivity.onCreate() */
    public static void onCreate(Activity activity) {
        NfcController.getInstance().init(activity);
    }

    /** Call from MainActivity.onResume() — enables foreground Reader Mode */
    public static void onResume(Activity activity) {
        NfcController.getInstance().enableReaderMode(activity);
    }

    /** Call from MainActivity.onPause() — disables foreground Reader Mode */
    public static void onPause(Activity activity) {
        NfcController.getInstance().disableReaderMode(activity);
    }

    /** Call from MainActivity.onStop() — cleans up dispatch when app is closing */
    public static void onStop(Activity activity) {
        if (activity.isFinishing() || activity.isChangingConfigurations()) {
            NfcController.getInstance().disableDispatchHandling();
        }
    }

    /** Call from MainActivity.onNewIntent() — routes background NFC intents */
    public static void onNewIntent(Intent intent) {
        if (intent != null) {
            NfcController.getInstance().handleIntent(intent);
        }
    }
}
