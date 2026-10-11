<!-- markdownlint-disable MD033 -->
# Nuvio

<div align="center">

  <img
    src="https://nuvio.tv/assets/nuvio-app-logo-wordmark.webp"
    alt="Nuvio Logo"
    width="320"
  />

  <p>
    A free, open-source media app for your phone, desktop, and TV.<br />
    Bring your own sources. Nuvio turns them into a library with artwork,
    ratings, subtitles, and your place saved on every screen.
  </p>

  <p>
    <a href="https://nuvio.tv">Website</a> &middot;
    <a href="https://github.com/NuvioMedia/NuvioMobile/releases/latest">
      GitHub releases
    </a> &middot;
    <a href="https://nuvio.tv/support">Support Nuvio</a>
  </p>

</div>

## Get Nuvio Mobile

- [Android on Google Play][google-play]
- [Android APK][github-releases]
- iOS via AltStore or SideStore: add [this source URL][altstore-source] in the
  app's Sources section, then install Nuvio.

## Build from source

```bash
git clone https://github.com/NuvioMedia/NuvioMobile.git
cd NuvioMobile
```

### Android

Android development requires Android Studio and the Android SDK.

```bash
./gradlew :androidApp:assembleFullDebug
```

### iOS

iOS development requires macOS and Xcode.

```bash
env NUVIO_IOS_DISTRIBUTION=full xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build/ios-derived-full-simulator \
  CODE_SIGNING_ALLOWED=NO \
  build
```

The shared app is built with Kotlin Multiplatform and Compose Multiplatform.

## License

[GNU General Public License v3.0](./LICENSE)

[google-play]: https://play.google.com/store/apps/details?id=com.nuvio.app
[github-releases]: https://github.com/NuvioMedia/NuvioMobile/releases/latest
[altstore-source]: https://raw.githubusercontent.com/NuvioMedia/NuvioMobile/cmp-rewrite/store.json
