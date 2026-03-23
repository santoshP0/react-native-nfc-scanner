package com.spo.nfcscanner.nfc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NfcController {

    public interface ScanListener {
        void onScan(String rawTag, String rawNdef, List<String> payloads);
    }

    private static final NfcController INSTANCE = new NfcController();
    private final AtomicBoolean dispatchEnabled = new AtomicBoolean(false);

    private NfcAdapter nfcAdapter;
    private Context appContext;
    private volatile ScanListener scanListener;

    private NfcController() {}

    public static NfcController getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        if (context == null) return;

        appContext = context.getApplicationContext();

        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(appContext);
        }
    }

    public boolean isSupported() {
        return nfcAdapter != null;
    }

    public boolean isEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    public boolean isDispatchEnabled() {
        return dispatchEnabled.get();
    }

    public void enableDispatchHandling() {
        dispatchEnabled.set(true);
        setDispatchComponentEnabled(true);
        startLifecycleGuard();
    }

    public void disableDispatchHandling() {
        dispatchEnabled.set(false);
        setDispatchComponentEnabled(false);
        stopLifecycleGuard();
    }

    private void startLifecycleGuard() {
        if (appContext == null) return;
        try {
            appContext.startService(new Intent(appContext, NfcLifecycleGuardService.class));
        } catch (Exception e) {
            Log.e("NfcController", "Failed to start NfcLifecycleGuardService", e);
        }
    }

    private void stopLifecycleGuard() {
        if (appContext == null) return;
        try {
            appContext.stopService(new Intent(appContext, NfcLifecycleGuardService.class));
        } catch (Exception e) {
            Log.e("NfcController", "Failed to stop NfcLifecycleGuardService", e);
        }
    }

    private void setDispatchComponentEnabled(boolean enabled) {
        if (appContext == null) return;
        try {
            PackageManager packageManager = appContext.getPackageManager();
            ComponentName componentName = new ComponentName(appContext, NfcDispatchActivity.class);
            int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
            Log.d("NfcController", "NfcDispatchActivity component state set to: " + (enabled ? "ENABLED" : "DISABLED"));
        } catch (Exception e) {
            Log.e("NfcController", "Error setting NfcDispatchActivity state", e);
        }
    }

    public void enableReaderMode(Activity activity) {
        if (activity == null || nfcAdapter == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle options = new Bundle();
            // Use this flag to suppress the system sounds and vibration
            int flags = NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V |
                        NfcAdapter.FLAG_READER_NFC_BARCODE |
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

            nfcAdapter.enableReaderMode(activity, tag -> {
                Log.d("NfcController", "Tag discovered via ReaderMode");
                handleTag(tag);
            }, flags, options);
        }
    }

    public void disableReaderMode(Activity activity) {
        if (activity == null || nfcAdapter == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            nfcAdapter.disableReaderMode(activity);
        }
    }

    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }

    /**
     * Always consume NFC intents.
     * Only process when dispatchEnabled = true.
     */
    public boolean handleIntent(Intent intent) {

        if (intent == null || !isSupported()) {
            return false;
        }

        // Silently consume when scanning is OFF
        if (!dispatchEnabled.get()) {
            return true;
        }

        String action = intent.getAction();
        if (action == null
                || (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))) {
            return false;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        handleTag(tag);

        return true;
    }

    public void handleTag(Tag tag) {
        if (tag == null) return;

        // Consume silently if dispatch is disabled
        if (!dispatchEnabled.get()) {
            Log.d("NfcController", "Tag received but dispatch disabled. Ignoring.");
            return;
        }

        Ndef ndef = Ndef.get(tag);
        NdefMessage ndefMessage = null;
        if (ndef != null) {
            ndefMessage = ndef.getCachedNdefMessage();
        }

        Parcelable[] rawMessages = null;
        if (ndefMessage != null) {
            rawMessages = new Parcelable[]{ndefMessage};
        }

        processTag(tag, rawMessages);
        triggerFeedback();
    }

    private void processTag(Tag tag, Parcelable[] rawMessages) {

        String rawTag = tag != null ? tag.toString() : "";
        String rawNdef = extractRawNdef(tag);
        List<String> payloads = extractPayloads(rawMessages);

        if (scanListener != null) {
            scanListener.onScan(rawTag, rawNdef, payloads);
        }
    }

    private List<String> extractPayloads(Parcelable[] rawMessages) {

        List<String> results = new ArrayList<>();
        if (rawMessages == null) return results;

        for (Parcelable message : rawMessages) {
            if (message instanceof NdefMessage) {

                NdefRecord[] records =
                        ((NdefMessage) message).getRecords();

                if (records == null) continue;

                for (NdefRecord record : records) {

                    try {
                        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                                && java.util.Arrays.equals(
                                record.getType(),
                                NdefRecord.RTD_TEXT)) {

                            byte[] payload = record.getPayload();
                            int langLen = payload[0] & 0x3F;
                            int textStart = 1 + langLen;

                            results.add(new String(
                                    payload,
                                    textStart,
                                    payload.length - textStart,
                                    StandardCharsets.UTF_8
                            ));
                        } else {
                            byte[] payload = record.getPayload();
                            results.add(payload != null
                                    ? new String(payload, StandardCharsets.UTF_8)
                                    : "");
                        }
                    } catch (Exception e) {
                        results.add("");
                    }
                }
            }
        }

        return results;
    }

    private String extractRawNdef(Tag tag) {
        Ndef ndef = tag != null ? Ndef.get(tag) : null;
        return ndef != null ? ndef.toString() : "";
    }

    private void triggerFeedback() {

        if (appContext == null) return;

        try {
            Vibrator vibrator =
                    (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator == null || !vibrator.hasVibrator()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(
                                150,
                                VibrationEffect.DEFAULT_AMPLITUDE
                        )
                );
            } else {
                vibrator.vibrate(150);
            }

        } catch (Exception ignored) {}
    }
}
