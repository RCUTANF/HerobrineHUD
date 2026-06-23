# Changelog

## v0.1.2 - 2026-06-23

### Added
- Added official support for Minecraft `1.21.11`
- Added split Fabric build pipelines:
  - `:fabric` for `26.1+`
  - `:fabric-remap` for `1.21.x`
- Added layered Mojang official mappings + Parchment support for `1.21.1`, `1.21.4`, and `1.21.11`
- Added version-aware main/client source layout under `fabric/src/mc<version>/main` and `fabric/src/mc<version>/client`
- Added version bridges for networking, HUD render registration, and keybinding registration
- Added a HUD provider registry and public provider API for registering alternate HUD implementations
- Added a built-in classic HUD provider and provider selection support in the management screen
- Added a read-only HerobrineHUD client data API for teams, players, placements, hotkeys, spectating state, and HUD settings
- Added serializable HUD data snapshots and JSON snapshot export for external HUD/UI consumers

### Changed
- Reorganized Fabric multi-version source loading to support separate versioned `main` and `client` code
- Split GUI and HUD implementation where Minecraft/Fabric APIs differ between `26.1` and `1.21.x`
- Split chat mixin implementations by Minecraft version
- Updated Gradle version routing, debug tasks, and wrapper/toolchain expectations for the new Loom split
- Moved classic HUD rendering behind the provider system
- Updated the classic HUD renderer to consume the public data API instead of internal client state directly
- Relaxed the Fabric metadata Minecraft dependency for `26.1` builds to allow patch-compatible `26.1.x` versions

### Fixed
- Fixed `1.21.11` build and runtime compatibility issues across Fabric networking, GUI, and mixin targets
- Fixed shared player avatar rendering state extraction regression that caused the 3D player model to render too high in player cards
- Fixed `26.1.2` being rejected by Fabric Loader even though the `26.1` build is patch-compatible

### Notes
- NeoForge remains stubbed and is not part of this completed migration
- PowerShell users should quote version overrides such as `"-PdebugVersion=1.21.11"`
