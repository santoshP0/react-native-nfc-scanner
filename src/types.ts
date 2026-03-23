/**
 * The payload emitted with every 'NfcTagScanned' event.
 *
 * - tag:      raw Tag.toString() from Android
 * - ndef:     raw Ndef.toString() if the tag has NDEF data, otherwise ""
 * - payloads: decoded text payload(s) from NDEF records (UTF-8 strings)
 */
export interface NfcTagEvent {
  tag: string;
  ndef: string;
  payloads: string[];
}

/** Event names emitted by the native NfcManager module */
export type NfcEvent = 'NfcTagScanned';
