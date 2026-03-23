# @spo/react-native-nfc-scanner

Android NFC scanning for React Native. Supports foreground and background scanning, works with both old and new React Native architecture.

> **Android only.**

[![npm version](https://img.shields.io/npm/v/@spo/react-native-nfc-scanner)](https://www.npmjs.com/package/@spo/react-native-nfc-scanner)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/guide/topics/connectivity/nfc)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Installation

```sh
npm install @spo/react-native-nfc-scanner
```

---

## Setup

### 1. Register the package

**Kotlin** (`MainApplication.kt`):
```kotlin
import com.spo.nfcscanner.nfc.NfcManagerPackage

add(NfcManagerPackage())
```

**Java** (`MainApplication.java`):
```java
import com.spo.nfcscanner.nfc.NfcManagerPackage;

packages.add(new NfcManagerPackage());
```

---

### 2. Add lifecycle hooks to `MainActivity`

**Kotlin** (`MainActivity.kt`):
```kotlin
import android.content.Intent
import android.os.Bundle
import com.spo.nfcscanner.NfcActivityHelper

override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(null)
  NfcActivityHelper.onCreate(this)
}

override fun onResume() {
  super.onResume()
  NfcActivityHelper.onResume(this)
}

override fun onPause() {
  super.onPause()
  NfcActivityHelper.onPause(this)
}

override fun onStop() {
  super.onStop()
  NfcActivityHelper.onStop(this)
}

override fun onNewIntent(intent: Intent) {
  super.onNewIntent(intent)
  setIntent(intent)
  NfcActivityHelper.onNewIntent(intent)
}
```

**Java** (`MainActivity.java`):
```java
import android.content.Intent;
import android.os.Bundle;
import com.spo.nfcscanner.NfcActivityHelper;

@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(null);
  NfcActivityHelper.onCreate(this);
}

@Override
protected void onResume() {
  super.onResume();
  NfcActivityHelper.onResume(this);
}

@Override
protected void onPause() {
  super.onPause();
  NfcActivityHelper.onPause(this);
}

@Override
protected void onStop() {
  super.onStop();
  NfcActivityHelper.onStop(this);
}

@Override
public void onNewIntent(Intent intent) {
  super.onNewIntent(intent);
  NfcActivityHelper.onNewIntent(intent);
}
```

> **Note:** NFC permissions and background components are merged into your app automatically via Android manifest merging. No manual `AndroidManifest.xml` changes needed.

---

## Usage

```tsx
import React, { useEffect } from 'react';
import { Platform } from 'react-native';
import {
  startScanning,
  stopScanning,
  isSupported,
  isEnabled,
  goToNfcSetting,
  addNfcListener,
} from '@spo/react-native-nfc-scanner';

export default function App() {
  useEffect(() => {
    if (Platform.OS !== 'android') return;

    async function setup() {
      if (!(await isSupported())) return;
      if (!(await isEnabled())) { goToNfcSetting(); return; }

      const subscription = addNfcListener('NfcTagScanned', (event) => {
        console.log('Payloads:', event.payloads);
      });

      await startScanning();

      return () => {
        stopScanning();
        subscription.remove();
      };
    }

    const cleanup = setup();
    return () => { cleanup.then(fn => fn?.()); };
  }, []);
}
```

---

## API

| Method | Returns | Description |
|---|---|---|
| `startScanning()` | `Promise<void>` | Start listening for NFC tags |
| `stopScanning()` | `Promise<void>` | Stop listening for NFC tags |
| `isSupported()` | `Promise<boolean>` | Check if device has NFC hardware |
| `isEnabled()` | `Promise<boolean>` | Check if NFC is enabled in settings |
| `goToNfcSetting()` | `Promise<boolean>` | Open Android NFC settings screen |
| `addNfcListener(event, cb)` | `EmitterSubscription` | Subscribe to NFC events |

### Events

| Event | Payload | Description |
|---|---|---|
| `NfcTagScanned` | `NfcTagEvent` | Fired when a tag is scanned |

### Types

```typescript
interface NfcTagEvent {
  tag: string;        // tag identifier string from Android
  ndef: string;       // NDEF data string, empty if not present
  payloads: string[]; // decoded text content from NDEF records
}
```

---

## Requirements

- React Native **0.70+**
- Android **API 21+**
- Physical Android device with NFC (NFC does not work on emulators)

---

## License

MIT
