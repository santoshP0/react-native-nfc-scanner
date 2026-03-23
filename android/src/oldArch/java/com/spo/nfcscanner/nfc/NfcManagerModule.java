package com.spo.nfcscanner.nfc;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Exposes start/stop controls to the React Native layer so scanning can be gated by login state.
 * Emits {@code NfcTagScanned} events when tags are read.
 */
public class NfcManagerModule extends ReactContextBaseJavaModule {

    private static final String EVENT_TAG_SCANNED = "NfcTagScanned";

    private final ReactApplicationContext reactContext;
    private final NfcController controller;

    public NfcManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.controller = NfcController.getInstance();
        this.controller.init(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "NfcManager";
    }

    @ReactMethod
    public void startScanning(Promise promise) {
        try {
            if (!controller.isSupported()) {
                if (promise != null) {
                    promise.reject("E_NFC_UNSUPPORTED", "NFC adapter not available on this device.");
                }
                return;
            }
            controller.setScanListener(this::emitScanEvent);
            controller.enableDispatchHandling();
            if (promise != null) {
                promise.resolve(null);
            }
        } catch (Exception error) {
            controller.setScanListener(null);
            if (promise != null) {
                promise.reject("E_NFC_START", error);
            }
        }
    }

    @ReactMethod
    public void isSupported(Promise promise) {
        if (promise != null) {
            promise.resolve(controller.isSupported());
        }
    }

    @ReactMethod
    public void isEnabled(Promise promise) {
        if (promise != null) {
            promise.resolve(controller.isEnabled());
        }
    }

    @ReactMethod
    public void goToNfcSetting(Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            if (promise != null) {
                promise.reject("E_NO_ACTIVITY", "Activity not available");
            }
            return;
        }
        try {
            activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            if (promise != null) {
                promise.resolve(true);
            }
        } catch (Exception error) {
            if (promise != null) {
                promise.reject("E_NFC_SETTINGS", error);
            }
        }
    }

    @ReactMethod
    public void stopScanning(Promise promise) {
        try {
            controller.disableDispatchHandling();
            controller.setScanListener(null);
            if (promise != null) {
                promise.resolve(null);
            }
        } catch (Exception error) {
            if (promise != null) {
                promise.reject("E_NFC_STOP", error);
            }
        }
    }

    private void emitScanEvent(String rawTag, String rawNdef, java.util.List<String> payloads) {
        WritableMap map = Arguments.createMap();
        map.putString("tag", rawTag);
        map.putString("ndef", rawNdef);
        map.putArray("payloads", toWritableArray(payloads));
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(EVENT_TAG_SCANNED, map);
    }
    private WritableArray toWritableArray(java.util.List<String> payloads) {
        final WritableArray array = Arguments.createArray();
        if (payloads != null) {
            for (String payload : payloads) {
                array.pushString(payload);
            }
        }
        return array;
    }

}
