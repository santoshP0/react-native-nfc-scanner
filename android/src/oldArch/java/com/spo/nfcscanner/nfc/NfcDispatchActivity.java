package com.spo.nfcscanner.nfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

    /**
     * Headless activity that receives NFC intents while the app is backgrounded.
     * It forwards the work to {@link NfcController} and immediately finishes to avoid UI flicker.
     */
public class NfcDispatchActivity extends Activity {

    private static final String TAG = "OnCallNfcDispatch";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NfcController.getInstance().init(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Forward the tag intent to the controller if dispatch is active.
     */
    private void handleIntent(@Nullable final Intent intent) {
        if (intent == null) {
            finish();
            return;
        }
        if (!NfcController.getInstance().isDispatchEnabled()) {
            Log.d(TAG, "Dispatch disabled; ignoring tag intent.");
            finish();
            return;
        }
        final boolean handled = NfcController.getInstance().handleIntent(intent);
        if (!handled) {
            Log.d(TAG, "Intent ignored: " + intent.getAction());
        }
        finish();
    }
}
