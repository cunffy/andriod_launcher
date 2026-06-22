# Launcher

A custom Android home-screen launcher — the foundation for a personal "OS" blending
**Smart Launcher** (auto-categorized app drawer + category sidebar, gesture-driven) with
**Pixel Launcher** (Material You dynamic theming + universal search).

## What's in this foundation

- **Home screen** drawn over the live wallpaper, with a clock, a search pill, and a dock.
- **Slide-up app drawer** — swipe up on home (or tap the search pill) to open an
  anchored sheet; drag the handle down / press Back / press Home to close.
- **Universal search** — the marquee feature. Typing in the drawer searches, in ranked order:
  - **Apps** (installed app labels)
  - **System settings** (curated table → Wi-Fi, Bluetooth, Display, Battery, …)
  - **Files** (on-device media via `MediaStore`, once the media permission is granted)
  - **Google** — an always-present "Search Google for …" row, and pressing **Enter with
    nothing selected** runs a Google search for the typed text (the Pixel behavior).
- **Category sidebar** — Smart Launcher-style auto-grouping of apps (Social, Music, Games, …)
  driven by the OS app category with a package-name heuristic fallback.
- **Gestures** — swipe up = drawer, swipe down on home = expand the notification shade.

## Also included (expanded build)

- **Universal search add-ons**: inline calculator, offline unit conversions, contacts,
  a command palette (run launcher actions), and Play/YouTube/Maps hand-off chips.
- **Home grid editor**: long-press the home screen to enter edit mode — drag app shortcuts,
  drop one onto another to make a **folder**, add **widgets** (AppWidget hosting), and remove
  items. Backed by Room.
- **Per-app customization**: long-press an app in the drawer to rename it, change its
  category, **hide** it, **lock** it behind biometrics, add it to home, or uninstall.
- **At a Glance**: date + next calendar event (and weather via a pluggable provider).
- **Themed icons & icon packs**: Material You monochrome theming or third-party icon packs.
- **Notification badges**: dots on icons via a NotificationListenerService.
- **Gesture editor**: bind swipe-up / swipe-down / double-tap to launcher actions.
- **Backup & restore**: export/import the whole layout to a JSON file (Settings).

A **Settings** screen (separate activity) hosts appearance, gestures, badges, hidden apps,
and backup. Reach it from the home edit-mode toolbar, the command palette, or a gesture.

## Tech stack

Kotlin · Jetpack Compose (Material 3 / Material You) · MVVM (`ViewModel` + `StateFlow`) ·
Hilt DI · DataStore + **Room** · `LauncherApps`/`MediaStore`/`AppWidgetHost` · Biometric ·
kotlinx.serialization. Min SDK 31, target/compile SDK 35.

## Project layout

```
app/src/main/java/com/cunffy/launcher/
  LauncherActivity.kt          HOME-intent host; routes Home button to drawer collapse
  ui/LauncherRoot.kt           slide-up drawer overlay + gesture handling
  ui/home/                     HomeScreen + HomeViewModel (clock, dock)
  ui/drawer/                   AppDrawerScreen, CategorySidebar, AppDrawerViewModel
  ui/search/                   SearchBar/SearchPill, SearchResultsList, SearchViewModel
  ui/components/               AppIcon, Dock
  data/apps/                   AppRepository (LauncherApps), AppCategorizer
  data/search/                 SearchRepository + SearchProvider interface
  data/search/providers/       App / Settings / File / Web search providers
  data/prefs/                  LauncherPreferences (DataStore)
  gesture/NotificationShade.kt expand the status bar shade
```

To add a new search source, implement `SearchProvider` and add it to the provider list in
`data/search/SearchRepository.kt`.

## Build & run

Requires Android Studio (Ladybug or newer) with the Android SDK; min device/emulator API 31.

```bash
./gradlew assembleDebug      # build the APK
./gradlew installDebug       # install on a connected device/emulator
```

Then press the Home button and pick this launcher in the default-launcher chooser.

To enable file search, grant the media permission when prompted (or via
Settings → Apps → Launcher → Permissions).

## Roadmap

See `roadmap` ideas in the plan: At-a-Glance widget, themed icons, AppWidget hosting,
icon packs, folders, gesture editor, command palette, hidden/locked apps, layout editor
(Room-backed), and backup/restore.

> Note: expanding the notification shade uses a reflective `StatusBarManager` call
> (best-effort); a future version may move this to an AccessibilityService.
