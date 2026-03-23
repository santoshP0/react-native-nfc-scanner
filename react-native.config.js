module.exports = {
  dependency: {
    platforms: {
      android: {
        packageImportPath: 'import com.spo.nfcscanner.nfc.NfcManagerPackage;',
        packageInstance: 'new NfcManagerPackage()',
      },
      ios: null, // Android only
    },
  },
};
