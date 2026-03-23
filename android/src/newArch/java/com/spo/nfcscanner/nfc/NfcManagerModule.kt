package com.spo.nfcscanner.nfc

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.spo.nfcscanner.NativeNfcManagerSpec

/**
 * Exposes start/stop controls to the React Native layer so scanning can be gated by login state.
 * Emits `NfcTagScanned` events when tags are read.
 *
 * Extends NativeNfcManagerSpec (Codegen-generated) so it works on both:
 *   - New architecture (TurboModules / JSI)
 *   - Old architecture (legacy bridge) — via TurboReactPackage fallback
 */
@ReactModule(name = NfcManagerModule.NAME)
class NfcManagerModule(
    context: ReactApplicationContext
) : NativeNfcManagerSpec(context) {

    private val reactContext: ReactApplicationContext = context
    private val controller = NfcController

    init {
        controller.init(context)
    }

    override fun getName(): String = NAME

    override fun startScanning(promise: Promise) {
        try {
            if (!controller.isSupported()) {
                promise.reject("E_NFC_UNSUPPORTED", "NFC adapter not available on this device.")
                return
            }
            controller.setScanListener(
                object : NfcController.ScanListener {
                    override fun onScan(rawTag: String, rawNdef: String, payloads: List<String>) {
                        emitScanEvent(rawTag, rawNdef, payloads)
                    }
                }
            )
            controller.enableDispatchHandling()
            promise.resolve(null)
        } catch (error: Exception) {
            controller.setScanListener(null)
            promise.reject("E_NFC_START", error)
        }
    }

    override fun isSupported(promise: Promise) {
        promise.resolve(controller.isSupported())
    }

    override fun isEnabled(promise: Promise) {
        promise.resolve(controller.isEnabled())
    }

    override fun goToNfcSetting(promise: Promise) {
        val activity: Activity? = currentActivity
        if (activity == null) {
            promise.reject("E_NO_ACTIVITY", "Activity not available")
            return
        }
        try {
            activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            promise.resolve(true)
        } catch (error: Exception) {
            promise.reject("E_NFC_SETTINGS", error)
        }
    }

    override fun stopScanning(promise: Promise) {
        try {
            controller.disableDispatchHandling()
            controller.setScanListener(null)
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject("E_NFC_STOP", error)
        }
    }

    // Required by React Native's event emitter system for TurboModules
    override fun addListener(eventName: String) {}
    override fun removeListeners(count: Double) {}

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
        payloads?.forEach { payload -> array.pushString(payload) }
        return array
    }

    companion object {
        const val NAME = "NfcManager"
        private const val EVENT_TAG_SCANNED = "NfcTagScanned"
    }
}
