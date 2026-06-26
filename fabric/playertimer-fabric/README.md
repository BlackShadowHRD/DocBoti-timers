# PlayerTimer Fabric

A Fabric mod providing independent per-player timers for Minecraft servers and singleplayer worlds.

Current Fabric build:
- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API `0.153.0+26.2`
- Java 25 or newer

Each player has their own timer which can:
- count up
- count down
- be paused/resumed
- be hidden or shown independently
- be triggered by commands or command blocks

Designed for:
- minigames
- escape rooms
- timed challenges
- parkour
- speedrunning
- multiplayer events
- adventure maps

---

# Features

- Independent timer per player
- Countup and countdown modes
- Brigadier command support
- Pause and resume support
- Timer visibility control
- Countdown completion notification
- Sound effects on completion
- Multiple duration input formats
- Command block compatible using `/execute as`
- Admin commands for timer management
- Persistent timers across restarts and crashes
- Built for Fabric
- Java 25 support

---

# Commands

| Command | Description |
|---|---|
| `/playertimer startcountup` | Start a countup timer using the configured default colour |
| `/playertimer startcountup <color>` | Start a coloured countup timer |
| `/playertimer startcountdown` | Start a 5 minute countdown using the configured default colour |
| `/playertimer startcountdown <duration>` | Start a countdown with custom duration using the configured default colour |
| `/playertimer startcountdown <duration> <color>` | Start a coloured countdown |
| `/playertimer pause` | Pause the timer |
| `/playertimer resume` | Resume a paused timer |
| `/playertimer stop` | Stop the timer and reset it to `00:00` |
| `/playertimer reset` | Reset timer to `00:00` |
| `/playertimer hide` | Hide the timer display |
| `/playertimer show` | Show the timer display |
| `/playertimer admin clear <player>` | Clear the timer for player `<player>` (op level 2+) |
| `/playertimer admin clearall` | Clear the timer for all players (op level 2+) |

---

# Duration Formats

The countdown command accepts multiple input formats.

## 1. Seconds

```text
/playertimer startcountdown 300
```

Starts a 300 second countdown.

---

## 2. mm:ss

```text
/playertimer startcountdown 05:30
```

Starts a 5 minute 30 second countdown.

Rules:
- `mm` must be between `0` and `59`
- `ss` must be between `0` and `59`

Examples:

```text
05:30   ✓ valid
59:59   ✓ valid
60:00   ✗ invalid
05:60   ✗ invalid
99:99   ✗ invalid
```

---

## 3. hh:mm:ss

```text
/playertimer startcountdown 01:40:05
```

Starts a 1 hour 40 minute 5 second countdown.

Rules:
- `mm` must be between `0` and `59`
- `ss` must be between `0` and `59`

Examples:

```text
01:40:05   ✓ valid
10:59:59   ✓ valid
01:60:05   ✗ invalid
01:40:60   ✗ invalid
```

---

## 4. Text format

```text
/playertimer startcountdown 1h40m5s
/playertimer startcountdown 10m
/playertimer startcountdown 45s
/playertimer startcountdown 2h
/playertimer startcountdown 1h5s
```

Supports:
- hours (`h`)
- minutes (`m`)
- seconds (`s`)

Any combination may be used.

Rules:
- `m` must be between `0` and `59`
- `s` must be between `0` and `59`

Examples:

```text
1h40m5s   ✓ valid
10m30s    ✓ valid
59m59s    ✓ valid
1h60m     ✗ invalid
10m90s    ✗ invalid
10s49m    ✗ invalid
```

---

# Maximum Duration

The maximum allowed duration is:

```text
2147483647 seconds
```

which is approximately:

```text
68 years
```

This limit exists because the timer internally uses Java `int` values.

---

# Timer Colours

Timers may optionally be started with a colour argument.

## Supported colours

| Colour |
|---|
| black |
| dark_blue |
| dark_green |
| dark_aqua |
| dark_red |
| dark_purple |
| gold |
| gray |
| dark_gray |
| blue |
| green |
| aqua |
| red |
| light_purple |
| yellow |
| white |

Examples:

```text
/playertimer startcountup red
/playertimer startcountdown 5m gold
/playertimer startcountdown 01:30 dark_blue
```

---

# Timer States

Internally the mod tracks timer state using a finite state model.

| State | Meaning |
|---|---|
| `READY` | Timer exists but has not started |
| `RUNNING` | Timer is actively counting |
| `PAUSED` | Timer is temporarily halted |
| `STOPPED` | Timer was manually stopped |
| `FINISHED` | Countdown reached zero |

This prevents invalid operations such as:
- resuming finished timers
- starting multiple timers simultaneously
- resuming timers that were stopped

---

# Visibility Behaviour

Timer visibility is independent of timer state.

| Action | Result |
|---|---|
| Start timer | Timer becomes visible |
| Pause timer | Timer remains visible |
| Resume timer | Timer becomes visible |
| Stop timer | Timer remains unchanged |
| Reset timer | Timer remains visible |
| Hide command | Timer becomes hidden |
| Show command | Timer becomes visible |

---

# Countdown Completion

When a countdown reaches zero:

- the timer stops automatically
- the timer enters the `FINISHED` state
- the player receives a notification
- a bell sound is played

---

# Command Block Support

Commands can be triggered via command blocks using Minecraft's `execute` command.

Example:

```mcfunction
execute as @p run playertimer startcountdown 5m
```

This allows:
- buttons
- pressure plates
- redstone systems
- region triggers
- adventure map integration

---

# Persistence

All timers are saved to disk and restored on server restart or crash recovery. Timer data is stored in:

```text
config/playertimer/timers.json
```

Configuration is stored in:

```text
config/playertimer/config.toml
```

The current configuration supports:

```toml
defaultColor = "WHITE"
```

Saves occur:
- on every timer state change (start, pause, resume, stop, reset, hide, show, finish)
- every 30 seconds automatically
- when a player disconnects
- when the server stops

When a timer is restored after a restart, its state is set to `PAUSED` regardless of whether it was running before — the server was not ticking so the timer was not advancing. If the timer was started by the player directly, they are responsible for resuming it. If the timer was started by a minigame or adventure map, the game is responsible for resuming it.

## Failure Behaviour

**On load:** if a timer entry has a corrupt UUID it is skipped and a warning is logged. If the file cannot be read at all, a `RuntimeException` is thrown which prevents the mod from initialising.

**On save:** if the file cannot be written, an error is logged and a `RuntimeException` is thrown. Because the timer is considered critical infrastructure (e.g. for minigames), this will cause the server to crash rather than continue with unsaved state.

---

# Building

## Requirements

- Java 25
- Gradle
- Fabric Loader 0.19.3 or newer
- Fabric API 0.153.0+26.2

---

## Build

```bash
./gradlew build
```

Compiled jars will appear in:

```text
build/libs/
```

---

# Versioning

PlayerTimer uses the following versioning scheme:

```text
[mod-version]
```

Example:

```text
1.4.0
```

Minecraft compatibility is tracked in `fabric.mod.json`, `gradle.properties`, and the changelog entry for each release.

---

# Planned Changes

- Configuration options for:
  - default countdown duration
  - audible alarm on/off at end of countdown timer
  - pre countdown finish beep per second on/off
  - duration of pre countdown finish beeps
  - failure behaviour on save (critical → server crash, non-critical → log and continue)
- API — an interface to allow developers of games and adventure maps to access timer functions directly from their code

---

# Changelog

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

---

# License

MIT License
