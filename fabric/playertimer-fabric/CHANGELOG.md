# Changelog

All notable changes to PlayerTimer Fabric will be documented in this file.

## 1.4.0 - Minecraft 26.2

- Updated Fabric target to Minecraft 26.2
- Updated Fabric Loader requirement to 0.19.3 or newer
- Updated Fabric API dependency to 0.153.0+26.2
- Confirmed the Fabric module builds against the Minecraft 26.2 dependency set

## 26.1.x-1.2

- Timers are now persistent across server restarts and crashes
- Added admin commands (clear, clearall) gated behind op level 2
- Moved timer logic into `PlayerTimer` domain model
- Replaced anemic setters with domain operations (start, pause, resume, stop, reset)
- Introduced `TimerOperationResult` enum replacing exception-based control flow
- Extracted `PlayerTimerCommand` class from `PlayerTimerMod`
- Added `TimerStore` for JSON persistence via Gson
- Added periodic auto-save and save-on-state-change
- Consistent command source messaging via `TimerCommandContext`
- Player disconnect now removes timer from memory and triggers a save

## 26.1.x-1.1

- Added coloured timer support
- Added optional colour arguments to timer start commands

## 26.1.x-1.0

- Initial Fabric release
- Ported PlayerTimer from Paper to Fabric
- Added per-player timers
- Added countdown and countup modes
- Added timer state system
- Added visibility system
- Added Brigadier command support
- Added command block compatibility
- Added duration parsing with multiple formats
- Added action bar timer display
- Added countdown completion sounds
- Added Java 25 support
- Refactored timer logic into `PlayerTimerService`
