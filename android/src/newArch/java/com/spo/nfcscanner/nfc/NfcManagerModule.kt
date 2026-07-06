package com.spo.nfcscanner.nfc

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.spo.nfcscanner.NativeNfcManagerSpec

/**
 * Exposes NFC scanning controls to the React Native layer.
 *
 * Implements LifecycleEventListener and ActivityEventListener so the module
 * wires itself into the Activity lifecycle automatically — no MainActivity
 * or MainApplication changes are required from the consuming app.
 */
@ReactModule(name = NfcManagerModule.NAME)
class NfcManagerModule(context: ReactApplicationContext) :
    NativeNfcManagerSpec(context),
    LifecycleEventListener,
    ActivityEventListener {

    private val reactContext: ReactApplicationContext = context

    init {
        NfcController.init(context)
        context.addLifecycleEventListener(this)
        context.addActivityEventListener(this)
    }

    override fun getName(): String = NAME

    // ── LifecycleEventListener ────────────────────────────────────────────────

    override fun onHostResume() {
        // Activity came to foreground — enable reader mode for active scanning
        NfcController.enableReaderMode(currentActivity)
    }

    override fun onHostPause() {
        // Activity went to background — disable reader mode
        NfcController.disableReaderMode(currentActivity)
    }

    override fun onHostDestroy() {
        NfcController.disableDispatchHandling()
        NfcController.setScanListener(null)
    }

    // ── ActivityEventListener ─────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        NfcController.handleIntent(intent)
    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) { /* not used */ }

    // ── JS-facing methods ─────────────────────────────────────────────────────

    override fun startScanning(promise: Promise) {
        try {
            if (!NfcController.isSupported()) {
                promise.reject("E_NFC_UNSUPPORTED", "NFC is not available on this device.")
                return
            }
            NfcController.setScanListener(object : NfcController.ScanListener {
                override fun onScan(rawTag: String, rawNdef: String, payloads: List<String>) {
                    emitScanEvent(rawTag, rawNdef, payloads)
                }
            })
            NfcController.enableDispatchHandling()
            NfcController.enableReaderMode(currentActivity)
            promise.resolve(null)
        } catch (e: Exception) {
            NfcController.setScanListener(null)
            promise.reject("E_NFC_START", e)
        }
    }

    override fun stopScanning(promise: Promise) {
        try {
            NfcController.disableReaderMode(currentActivity)
            NfcController.disableDispatchHandling()
            NfcController.setScanListener(null)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("E_NFC_STOP", e)
        }
    }

    override fun isSupported(promise: Promise) {
        promise.resolve(NfcController.isSupported())
    }

    override fun isEnabled(promise: Promise) {
        promise.resolve(NfcController.isEnabled())
    }

    override fun goToNfcSetting(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("E_NO_ACTIVITY", "Activity is not available.")
            return
        }
        try {
            activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("E_NFC_SETTINGS", e)
        }
    }

    override fun preventDefaultNfcScreen(enabled: Boolean, promise: Promise) {
        try {
            NfcController.setPreventDefaultScreen(enabled)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("E_NFC_INTERCEPT", e)
        }
    }

    // Required by React Native for TurboModules that emit events
    override fun addListener(eventName: String) {}
    override fun removeListeners(count: Double) {}

    // ── Event emission ────────────────────────────────────────────────────────

    private fun emitScanEvent(rawTag: String, rawNdef: String, payloads: List<String>) {
        val map: WritableMap = Arguments.createMap()
        map.putString("tag", rawTag)
        map.putString("ndef", rawNdef)
        map.putArray("payloads", toWritableArray(payloads))
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(EVENT_TAG_SCANNED, map)
    }

    private fun toWritableArray(payloads: List<String>?): WritableArray {
        val array = Arguments.createArray()
        payloads?.forEach { array.pushString(it) }
        return array
    }

    companion object {
        const val NAME = "NfcManager"
        private const val EVENT_TAG_SCANNED = "NfcTagScanned"
    }
}
