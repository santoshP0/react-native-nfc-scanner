package com.spo.nfcscanner.nfc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight NFC manager that keeps scanning active only while app lifecycle allows it.
 */
object NfcController {

    interface ScanListener {
        fun onScan(rawTag: String, rawNdef: String, payloads: List<String>)
    }

    private const val TAG = "OnCallNfcController"

    private val dispatchEnabled = AtomicBoolean(false)
    private var nfcAdapter: NfcAdapter? = null
    private var appContext: Context? = null

    @Volatile
    private var scanListener: ScanListener? = null

    /** Initialise the adapter reference using the application context. */
    fun init(context: Context?) {
        if (context == null) {
            return
        }
        appContext = context.applicationContext
        if (nfcAdapter == null) {
            val ctx = appContext ?: return
            nfcAdapter = NfcAdapter.getDefaultAdapter(ctx)
            if (nfcAdapter == null) {
                Log.w(TAG, "NFC adapter not available on this device")
            }
        }
    }

    fun isSupported(): Boolean = nfcAdapter != null

    /** Returns true if the adapter exists and NFC is enabled on the device. */
    fun isEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /** Enable NFC dispatch and start the guard service. */
    fun enableDispatchHandling() {
        if (dispatchEnabled.compareAndSet(false, true)) {
            setDispatchComponentEnabled(true)
            startLifecycleGuard()
        }
    }

    /** Disable NFC dispatch and stop the guard service. */
    fun disableDispatchHandling() {
        if (dispatchEnabled.compareAndSet(true, false)) {
            setDispatchComponentEnabled(false)
            stopLifecycleGuard()
        }
    }

    fun isDispatchEnabled(): Boolean = dispatchEnabled.get()

    /** Enable reader mode on the given activity to intercept NFC tags directly. */
    fun enableReaderMode(activity: Activity?) {
        if (activity == null || nfcAdapter == null) return
        if (!dispatchEnabled.get()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val options = Bundle()
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) NfcAdapter.FLAG_READER_NFC_BARCODE else 0) or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            val readerCallback = object : NfcAdapter.ReaderCallback {
                override fun onTagDiscovered(tag: Tag?) {
                    Log.d(TAG, "Tag discovered via ReaderMode")
                    handleTag(tag)
                }
            }
            nfcAdapter?.enableReaderMode(activity, readerCallback, flags, options)
        }
    }

    /** Disable reader mode on the given activity. */
    fun disableReaderMode(activity: Activity?) {
        if (activity == null || nfcAdapter == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            nfcAdapter?.disableReaderMode(activity)
        }
    }

    /** Handle an NFC intent delivered via [NfcDispatchActivity]. */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null || !isSupported()) {
            return false
        }

        // Silently consume when scanning is OFF
        if (!dispatchEnabled.get()) {
            return true
        }

        val action = intent.action
        if (action == null ||
            (action != NfcAdapter.ACTION_TAG_DISCOVERED &&
                action != NfcAdapter.ACTION_TECH_DISCOVERED &&
                action != NfcAdapter.ACTION_NDEF_DISCOVERED)
        ) {
            return false
        }

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        handleTag(tag)
        return true
    }

    /** Process a tag discovered either via dispatch intent or reader mode. */
    fun handleTag(tag: Tag?) {
        if (tag == null) return

        // Consume silently if dispatch is disabled
        if (!dispatchEnabled.get()) {
            Log.d(TAG, "Tag received but dispatch disabled. Ignoring.")
            return
        }

        val ndef = Ndef.get(tag)
        val ndefMessage = ndef?.cachedNdefMessage
        val rawMessages: Array<Parcelable>? = ndefMessage?.let { arrayOf(it) }

        processTag(tag, rawMessages)
        triggerFeedback()
    }

    private fun processTag(tag: Tag?, rawMessages: Array<Parcelable>?) {
        val tagId = tag?.id?.let { bytesToHex(it) } ?: "unknown"
        val payloads = extractPayloads(rawMessages)
        val rawTag = tag?.toString() ?: ""
        val rawNdef = extractRawNdef(tag)

        Log.i(TAG, "NFC tag scanned: $tagId")
        scanListener?.onScan(rawTag, rawNdef, payloads)
    }

    private fun extractPayloads(rawMessages: Array<Parcelable>?): List<String> {
        val results = mutableListOf<String>()
        if (rawMessages == null) {
            return results
        }
        rawMessages.forEach { message ->
            if (message is NdefMessage) {
                val records = message.records
                if (records == null || records.isEmpty()) {
                    results.add("")
                    return@forEach
                }
                records.forEach { record ->
                    var text = ""
                    try {
                        text = if (record != null &&
                            record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_TEXT)
                        ) {
                            val payload = record.payload
                            if (payload != null && payload.isNotEmpty()) {
                                val langLen = payload[0].toInt() and 0x3F
                                val textStart = 1 + langLen
                                if (textStart < payload.size) {
                                    String(
                                        payload,
                                        textStart,
                                        payload.size - textStart,
                                        StandardCharsets.UTF_8
                                    )
                                } else {
                                    ""
                                }
                            } else {
                                ""
                            }
                        } else if (record != null &&
                            record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_URI)
                        ) {
                            val payload = record.payload
                            if (payload != null && payload.size > 1) {
                                val uriPrefixes = arrayOf(
                                    "", "http://www.", "https://www.", "http://", "https://",
                                    "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
                                    "ftps://", "sftp://", "smb://", "nfs://", "ftp://",
                                    "dav://", "news:", "telnet://", "imap:", "rtsp://",
                                    "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://",
                                    "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://",
                                    "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:",
                                    "urn:epc:raw:", "urn:epc:", "urn:nfc:"
                                )
                                val prefixByte = payload[0].toInt() and 0xFF
                                val prefix = if (prefixByte < uriPrefixes.size) uriPrefixes[prefixByte] else ""
                                prefix + String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
                            } else {
                                record.payload?.let { String(it, StandardCharsets.UTF_8) } ?: ""
                            }
                        } else {
                            record?.payload?.let { String(it, StandardCharsets.UTF_8) } ?: ""
                        }
                    } catch (_: Exception) {
                        text = ""
                    }
                    results.add(text)
                }
            }
        }
        return results
    }

    private fun bytesToHex(input: ByteArray?): String {
        if (input == null || input.isEmpty()) {
            return "00"
        }
        val out = CharArray(input.size * 2)
        for (i in input.indices) {
            val value = input[i].toInt() and 0xFF
            out[i * 2] = Character.forDigit(value ushr 4, 16).uppercaseChar()
            out[i * 2 + 1] = Character.forDigit(value and 0x0F, 16).uppercaseChar()
        }
        return String(out).uppercase(Locale.US)
    }

    private fun extractRawNdef(tag: Tag?): String {
        val ndef = tag?.let { Ndef.get(it) }
        return ndef?.toString() ?: ""
    }

    private fun triggerFeedback() {
        val ctx = appContext ?: return
        try {
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!vibrator.hasVibrator()) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        150,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (vibrationError: Exception) {
            Log.w(TAG, "Unable to trigger vibration", vibrationError)
        }
    }

    private fun setDispatchComponentEnabled(enabled: Boolean) {
        val ctx = appContext ?: return
        try {
            val packageManager = ctx.packageManager
            val componentName = ComponentName(ctx, NfcDispatchActivity::class.java)
            val desiredState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val currentState = packageManager.getComponentEnabledSetting(componentName)
            if (currentState != desiredState) {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    desiredState,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (componentError: Exception) {
            Log.w(TAG, "Failed to change NFC dispatch component state", componentError)
        }
    }

    private fun startLifecycleGuard() {
        val ctx = appContext ?: return
        val serviceIntent = Intent(ctx, NfcLifecycleGuardService::class.java)
        try {
            ctx.startService(serviceIntent)
        } catch (serviceError: IllegalStateException) {
            Log.w(TAG, "Unable to start NFC lifecycle guard service", serviceError)
        }
    }

    private fun stopLifecycleGuard() {
        val ctx = appContext ?: return
        val serviceIntent = Intent(ctx, NfcLifecycleGuardService::class.java)
        try {
            ctx.stopService(serviceIntent)
        } catch (serviceError: Exception) {
            Log.w(TAG, "Unable to stop NFC lifecycle guard service", serviceError)
        }
    }

    fun setScanListener(listener: ScanListener?) {
        scanListener = listener
    }
}
