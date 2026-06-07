# Changelog

## Unreleased

### Added
- Added official support for Minecraft `1.21.11`
- Added split Fabric build pipelines:
  - `:fabric` for `26.1+`
  - `:fabric-remap` for `1.21.x`
- Added layered Mojang official mappings + Parchment support for `1.21.1`, `1.21.4`, and `1.21.11`
- Added version-aware main/client source layout under `fabric/src/mc<version>/main` and `fabric/src/mc<version>/client`
- Added version bridges for networking, HUD render registration, and keybinding registration

### Changed
- Reorganized Fabric multi-version source loading to support separate versioned `main` and `client` code
- Split GUI and HUD implementation where Minecraft/Fabric APIs differ between `26.1` and `1.21.x`
- Split chat mixin implementations by Minecraft version
- Updated Gradle version routing, debug tasks, and wrapper/toolchain expectations for the new Loom split

### Fixed
- Fixed `1.21.11` build and runtime compatibility issues across Fabric networking, GUI, and mixin targets
- Fixed shared player avatar rendering state extraction regression that caused the 3D player model to render too high in player cards

### Notes
- NeoForge remains stubbed and is not part of this completed migration
- PowerShell users should quote version overrides such as `"-PdebugVersion=1.21.11"`
