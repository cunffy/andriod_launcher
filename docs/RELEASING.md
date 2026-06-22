# Releasing & self-update

The launcher can update itself from GitHub Releases. The `Release` workflow
(`.github/workflows/release.yml`) builds a signed APK and publishes it together with an
`update.json` manifest; the app polls that manifest and offers a one-tap update.

## One-time setup

### 1. Create a signing keystore (so every build shares one signature)

Self-update only works if each release is signed with the **same** key (Android refuses to
update an app whose signature changed). Create a keystore once:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias launcher
```

Then base64-encode it:

```bash
base64 -w0 release.jks   # macOS: base64 release.jks | tr -d '\n'
```

### 2. Add repository secrets

In GitHub → Settings → Secrets and variables → Actions, add:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the base64 string from step 1 |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `launcher` (or your alias) |
| `KEY_PASSWORD` | the key password |

(If you skip these, builds still succeed but use the debug key — fine for a first install,
but self-update across CI runs won't work because the debug key isn't stable.)

### 3. Point the launcher at the manifest

In the launcher: **Settings → Updates → Update manifest URL**, set:

```
https://github.com/<owner>/<repo>/releases/latest/download/update.json
```

(`releases/latest/...` always resolves to the newest published release, so this URL never
changes.)

## Cutting a release

Either push a tag:

```bash
git tag v0.2.0 && git push origin v0.2.0
```

or run the **Release** workflow manually (Actions → Release → Run workflow) and enter the
version name.

The workflow uses the GitHub run number as `versionCode`, so each run is newer than the last.
On the next launch (or via **Settings → Check for updates**) the app sees the new manifest,
downloads `launcher.apk`, and hands it to the installer.
