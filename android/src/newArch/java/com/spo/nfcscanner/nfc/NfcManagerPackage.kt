package com.spo.nfcscanner.nfc

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

/**
 * Registers [NfcManagerModule] with the React Native runtime.
 */
class NfcManagerPackage : BaseReactPackage() {

    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
        return if (name == NfcManagerModule.NAME) {
            NfcManagerModule(reactContext)
        } else {
            null
        }
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        return ReactModuleInfoProvider {
            mapOf(
                NfcManagerModule.NAME to ReactModuleInfo(
                    NfcManagerModule.NAME,
                    NfcManagerModule.NAME,
                    false,
                    false,
                    false,
                    false,
                    true // isTurboModule
                )
            )
        }
    }
}
