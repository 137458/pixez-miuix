# PixEz Flutter v1 Archive

Flutter v1 was frozen when PixEz MIUIX moved its active development and release pipeline to Compose Multiplatform in `../../compose-miuix/`.

This directory is retained as a **read-only historical snapshot** for source history, user-data migration research, and exceptional security fixes. New features, desktop releases, and regular CI work belong to the Compose project.

## Archived project

The archived Flutter application remains self-contained in this directory:

```bash
cd archive/flutter-v1
flutter pub get
flutter build windows
```

The old GitHub Actions definitions are stored in `workflows/` as historical references and are intentionally not active workflows. Flutter assets, local plugins, platform runners, and its Gradle wrapper stay beside the archived `pubspec.yaml` so the snapshot can still be built independently.

## Migration notes

- Compose Desktop continues to read the historical Windows/macOS data locations where possible.
- Android Flutter (`com.perol.pixez`) and Compose Android (`com.perol.pixez.miuix`) are separate sandboxes. Users need an explicit export/import path; installing Compose does not overwrite Flutter v1.
- Do not add new product work here. Please target `compose-miuix/` and its Compose Desktop/Android build pipeline.
