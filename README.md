<div align="center">

# NexusWarp

**A lightweight, high-performance teleportation, economy, and messaging plugin for Paper, Purpur, Spigot, and Folia.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.2-2b2b2b.svg?style=flat-square)](https://github.com/NothingFlare/NexusWarp/releases)
[![License](https://img.shields.io/badge/License-MIT-2b2b2b.svg?style=flat-square)](LICENSE)

[Overview](#overview) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Localization](#localization) • [Installation](#installation) • [Compiling from Source](#compiling-from-source)

</div>

---

## Overview

NexusWarp is an optimized, all-in-one teleportation, messaging, and economy solution designed specifically for modern Minecraft server software. Standard Bukkit/Spigot implementations often suffer from synchronous chunk loading spikes and thread safety violations on multi-threaded servers. NexusWarp addresses these architectural limitations by fully supporting Folia's regional schedulers alongside Paper's asynchronous chunk loading pipelines.

Starting with version 1.2, NexusWarp incorporates a full Essentials-grade Economy engine with official Vault API integration, complete player and administrative teleportation mechanics (`/tpa`, `/tp`, `/tphere`), private messaging (`/msg`, `/reply`), destination safety validation, world and dimension restrictions, and an automated GitHub update checker.

---

## Key Features

- **Native Folia and Paper Concurrency**
  - Fully compatible with Folia's multi-threaded regionized server model using `EntityScheduler`, `GlobalRegionScheduler`, and `AsyncScheduler`.
  - Non-blocking asynchronous chunk loading and teleportation via `teleportAsync`.
  - Flags `folia-supported: true` in the plugin manifest.

- **Complete Essentials-Grade Economy System**
  - High-precision arithmetic via `BigDecimal` avoiding floating-point inaccuracies.
  - Thread-safe balance cache with asynchronous disk persistence (`plugins/NexusWarp/userdata/{uuid}.yml`).
  - Standard Vault Economy Provider (`VaultEconomyProvider`) registered directly with Bukkit's ServicesManager for seamless compatibility with plugins like ChestShop, QuickShop, and AuctionHouse.
  - Full player command suite: `/balance`, `/pay`, and a cached top balance leaderboard (`/balancetop`).
  - Powerful administrative command: `/eco <give|take|set|reset> <player|*|**> [amount[%]]` supporting bulk actions and percentages.
  - Custom exception handling: `NoLoanPermittedException`, `UserDoesNotExistException`, and `MaxMoneyException`.

- **Comprehensive Teleportation Engine**
  - Interactive TPA requests with clickable chat buttons (`[Accept]`, `[Cancel]`) and tooltips.
  - Direct administrative teleportation (`/tp <player>`, `/tp <player> <target>`, `/tp <x> <y> <z>`, `/tp <player> <x> <y> <z>`).
  - Player summoning teleportation (`/tphere <player>`, aliases `/s`, `/tph`).
  - Independent `/spawn` and `/hub` (alias `/lobby`) commands with configurable world, coordinates, yaw, and pitch. Both commands are disabled by default (`enabled: false`) in `config.yml`.
  - Administrative setup commands (`/setspawn`, `/sethub`) to instantly configure destinations in-game.
  - All administrative teleportation permissions (`nexuswarp.tp`, `nexuswarp.tphere`) are disabled by default (`default: false`) for explicit management via LuckPerms.

- **Private Messaging System (`/msg` & `/reply`)**
  - Thread-safe direct messaging between players and console.
  - Quick reply functionality (`/r <message>`) tracking active conversation partners.
  - Configurable message formatting and tab-completion.
  - Enabled for all players by default (`default: true`).

- **Automated Update Checker**
  - Asynchronously queries GitHub API (`https://github.com/NothingFlare/NexusWarp/`) upon startup.
  - Compares semantic versioning tags against running binary and alerts administrators in the console when new releases are available.

- **Safe Landing Validation**
  - Scans destination environments prior to teleportation.
  - Prevents teleportation into lava, the void, or solid suffocating blocks.
  - Scans adjacent horizontal blocks to locate a safe alternate ground position.

- **World and Dimension Restrictions**
  - Restrict teleportation by specific world name or dimension type (`NORMAL`, `NETHER`, `THE_END`).
  - Includes an `ignore-bypass` directive to ensure restrictions apply to server operators during testing and production.

- **Warmup and Interruption Handling**
  - Configurable teleportation warmup timer.
  - Detects player movement and cancels teleportation while ignoring head rotation (yaw and pitch).
  - Automatically aborts teleportation upon receiving damage during warmup.

- **Comprehensive Localization**
  - Bundled with 13 built-in language files (`en`, `ru`, `zh`, `de`, `fr`, `es`, `pt`, `ja`, `uk`, `it`, `pl`, `tr`, `ko`).

---

## Compatibility

| Platform | Supported Versions | Implementation Details |
| :--- | :--- | :--- |
| **Folia** | 1.20 – 1.20.6+ | Native regional entity and async schedulers |
| **Paper** | 1.20 – 1.20.6+ | Asynchronous chunk loading via Paper API |
| **Purpur** | 1.20 – 1.20.6+ | Fully supported |
| **Spigot** | 1.20 – 1.20.6+ | Supported with standard Bukkit scheduler fallback |

---

## Commands and Permissions

### Teleportation and Messaging Commands

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/tpa <player>` | None | Sends a teleportation request to the specified player | `nexuswarp.use` (`true`) |
| `/tpaccept [player]` | `/tpyes` | Accepts an active incoming teleportation request | `nexuswarp.use` (`true`) |
| `/tpacancel [player]` | `/tpdeny`, `/tpcancel`, `/tpno` | Cancels an active request (usable by sender or recipient) | `nexuswarp.use` (`true`) |
| `/tp <player> [target]` | None | Teleports to a player, target to target, or to coordinates | `nexuswarp.tp` (`false`) |
| `/tphere <player>` | `/s`, `/tph` | Teleports a player to your location | `nexuswarp.tphere` (`false`) |
| `/spawn [player]` | None | Teleports to the spawn location | `nexuswarp.spawn` (`true`) |
| `/hub [player]` | `/lobby` | Teleports to the hub location | `nexuswarp.hub` (`true`) |
| `/setspawn` | None | Sets the spawn point to your current position | `nexuswarp.setspawn` (`op`) |
| `/sethub` | `/setlobby` | Sets the hub point to your current position | `nexuswarp.sethub` (`op`) |
| `/msg <player> <message>` | `/w`, `/tell`, `/m`, `/whisper` | Sends a private message to a player | `nexuswarp.msg` (`true`) |
| `/reply <message>` | `/r` | Replies to the last player who messaged you | `nexuswarp.msg` (`true`) |
| `/nexuswarp reload` | `/nw`, `/tpreload` | Asynchronously reloads configuration and language files | `nexuswarp.admin` (`op`) |

### Economy Commands

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/balance [player]` | `/bal`, `/money` | View your balance or the balance of another player | `nexuswarp.balance` (`true`) |
| `/pay <player> <amount>` | None | Transfers money from your balance to another player | `nexuswarp.pay` (`true`) |
| `/balancetop [page]` | `/baltop` | Displays the top balances on the server | `nexuswarp.balancetop` (`true`) |
| `/eco <give\|take\|set\|reset> <player\|*\|**> [amount[%]]` | None | Administrative balance manipulation | `nexuswarp.eco` (`op`) |

### Permissions Matrix

| Node | Description | Default Assignment |
| :--- | :--- | :--- |
| `nexuswarp.use` | Grants access to standard player TPA commands (`/tpa`, `/tpaccept`, `/tpacancel`) | `true` (all players) |
| `nexuswarp.msg` | Grants access to private messaging (`/msg`, `/reply`) | `true` (all players) |
| `nexuswarp.tp` | Grants access to direct teleportation (`/tp`) | `false` (configure via LuckPerms) |
| `nexuswarp.tphere` | Grants access to summoning teleportation (`/tphere`, `/s`, `/tph`) | `false` (configure via LuckPerms) |
| `nexuswarp.spawn` | Allows using the `/spawn` command | `true` (all players) |
| `nexuswarp.spawn.others` | Allows sending other players to spawn | `op` |
| `nexuswarp.hub` | Allows using the `/hub` command | `true` (all players) |
| `nexuswarp.hub.others` | Allows sending other players to hub | `op` |
| `nexuswarp.setspawn` | Allows setting the spawn location via `/setspawn` | `op` |
| `nexuswarp.sethub` | Allows setting the hub location via `/sethub` | `op` |
| `nexuswarp.balance` | Allows checking own balance via `/balance` | `true` (all players) |
| `nexuswarp.balance.others` | Allows checking other players' balances via `/balance <player>` | `op` |
| `nexuswarp.pay` | Allows sending payments via `/pay` | `true` (all players) |
| `nexuswarp.balancetop` | Allows viewing top balances via `/balancetop` | `true` (all players) |
| `nexuswarp.eco` | Allows administrative economy manipulation via `/eco` | `op` |
| `nexuswarp.bypass.world` | Bypasses world and dimension restrictions | `false` |
| `nexuswarp.bypass.delay` | Bypasses the teleportation warmup delay | `op` |
| `nexuswarp.admin` | Grants access to administrative commands (`/nexuswarp reload`) | `op` |

---

## Configuration

The default configuration file (`config.yml`):

```yaml
# =======================================================
#               NexusWarp Plugin Configuration
# =======================================================

# Language / Active language
# Supported: en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko
language: "ru"

# Updates configuration
updates:
  # Check for updates on startup via GitHub API
  check-for-updates: true

# Economy configuration
economy:
  # Initial balance for newly registered players
  starting-balance: 0.00

  # Minimum allowable balance
  min-balance: 0.00

  # Maximum allowable balance
  max-balance: 10000000000000.00

  # Currency symbol displayed in messages
  currency-symbol: "$"

  # Place currency symbol behind amount (e.g. 100$ vs $100)
  currency-symbol-placed-behind: false

  # Minimum amount allowed for /pay command
  minimum-pay-amount: 0.01

# Spawn command configuration
# Disabled by default
spawn:
  enabled: false
  world: "world"
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

# Hub command configuration
# Disabled by default
hub:
  enabled: false
  world: "world"
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

# Disabled worlds configuration
disabled-worlds:
  # Enable world restriction
  enabled: false

  # Ignore bypass permission for OPs (true = OPs cannot bypass either)
  ignore-bypass: true

  # List of world names or environment types:
  #   - "world"        (matches world name)
  #   - "world_nether" (matches world name)
  #   - "NORMAL"       (matches environment type)
  #   - "NETHER"       (matches environment type)
  #   - "THE_END"      (matches environment type)
  worlds:
    - "world_nether"
    - "world_the_end"
    - "pvp_arena"

# Teleportation request settings
requests:
  # Request expiration time in seconds (0 = disabled / persists until resolved)
  timeout: 0

  # Warmup delay before teleporting in seconds (0 = instant teleport)
  teleport-delay: 0

  # Cancel teleportation if player moves during warmup
  cancel-on-movement: true

  # Cancel teleportation if player takes damage during warmup
  cancel-on-damage: true

# Safety checks before teleporting
safe-teleport:
  # Enable safety checks
  enabled: true

  # Prevent teleporting into lava
  prevent-lava: true

  # Prevent teleporting into the void
  prevent-void: true

  # Prevent suffocating in solid blocks
  prevent-suffocation: true
```

---

## Localization

Pre-configured language files are located in `plugins/NexusWarp/lang/`:

| Code | Language | Native Name | File |
| :---: | :--- | :--- | :--- |
| `en` | English | English | `messages_en.yml` |
| `ru` | Russian | Русский | `messages_ru.yml` |
| `zh` | Chinese (Simplified) | 简体中文 | `messages_zh.yml` |
| `de` | German | Deutsch | `messages_de.yml` |
| `fr` | French | Français | `messages_fr.yml` |
| `es` | Spanish | Español | `messages_es.yml` |
| `pt` | Portuguese | Português | `messages_pt.yml` |
| `ja` | Japanese | 日本語 | `messages_ja.yml` |
| `uk` | Ukrainian | Українська | `messages_uk.yml` |
| `it` | Italian | Italiano | `messages_it.yml` |
| `pl` | Polish | Polski | `messages_pl.yml` |
| `tr` | Turkish | Türkçe | `messages_tr.yml` |
| `ko` | Korean | 한국어 | `messages_ko.yml` |

---

## Installation

1. Download `NexusWarp-1.2.jar` from the [releases page](https://github.com/NothingFlare/NexusWarp/releases).
2. Place the jar file into your server's `plugins/` directory.
3. If using economy features with external plugins (e.g., shops), install [Vault](https://www.spigotmc.org/resources/vault.34315/).
4. Start or restart the server to generate default configurations and language files.
5. Modify `plugins/NexusWarp/config.yml` according to your requirements.
6. Execute `/nexuswarp reload` to apply configuration changes without server restart.

---

## Compiling from Source

### Requirements
- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.8+
- Git

### Build Instructions

```bash
git clone https://github.com/NothingFlare/NexusWarp.git
cd NexusWarp
mvn clean package
```

The resulting binary will be located in:
```text
target/NexusWarp-1.2.jar
```

---

## License

This project is licensed under the [MIT License](LICENSE).
