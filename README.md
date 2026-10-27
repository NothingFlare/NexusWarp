<div align="center">

# NexusWarp

**A lightweight, high-performance teleportation (TPA) & private messaging plugin for Paper, Purpur, Spigot, and Folia.**

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20+-2b2b2b.svg?style=flat-square)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-17+-2b2b2b.svg?style=flat-square)](https://www.oracle.com/java/)
[![Folia](https://img.shields.io/badge/Folia-Supported-2b2b2b.svg?style=flat-square)](https://papermc.io/software/folia)
[![Release](https://img.shields.io/badge/Release-1.1-2b2b2b.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-2b2b2b.svg?style=flat-square)](LICENSE)

[Overview](#overview) • [Key Features](#key-features) • [Compatibility](#compatibility) • [Commands and Permissions](#commands-and-permissions) • [Configuration](#configuration) • [Localization](#localization) • [Installation](#installation) • [Compiling from Source](#compiling-from-source)

</div>

---

## Overview

NexusWarp is an optimized teleportation and communication solution designed specifically for modern Minecraft server software. Standard Bukkit/Spigot teleportation implementations often suffer from synchronous chunk loading spikes and thread safety violations on multi-threaded servers. NexusWarp addresses these architectural limitations by fully supporting Folia's regional schedulers alongside Paper's asynchronous chunk loading pipelines.

The plugin provides interactive clickable chat components, direct administrative teleportation (`/tp`), private messaging (`/msg` and `/reply`), destination safety validation, granular world and dimension restrictions, and native support for 13 languages without external dependencies.

---

## Key Features

- **Native Folia and Paper Concurrency**
  - Fully compatible with Folia's multi-threaded regionized server model using `EntityScheduler`, `GlobalRegionScheduler`, and `AsyncScheduler`.
  - Employs non-blocking asynchronous chunk loading and teleportation via `teleportAsync`.
  - Flags `folia-supported: true` in the plugin manifest.

- **Interactive Text Components**
  - Incoming requests render interactive `[Accept]` and `[Cancel]` buttons directly in the chat.
  - Hover tooltips and instant command execution prevent command injection risks and eliminate the need for console commands.

- **Direct Teleportation (`/tp`)**
  - Instant teleportation to players or coordinates (`/tp <player>`, `/tp <player> <target>`, `/tp <x> <y> <z>`).
  - Permission is disabled by default (`default: false`), designed to be granted via LuckPerms.

- **Private Messaging System (`/msg` & `/reply`)**
  - Fast, thread-safe direct messaging between players and console.
  - Quick reply functionality (`/r <message>`) tracking conversation partners.
  - Available to all players by default (`default: true`).

- **Safe Landing Validation**
  - Pre-teleport verification scans the destination environment before moving the player.
  - Prevents teleportation into lava, the void, or inside solid suffocating blocks.
  - Automatically checks neighboring blocks to locate a safe alternate ground position.

- **World and Dimension Restrictions**
  - Restrict teleportation by specific world names (e.g., `world_nether`, `pvp_arena`).
  - Restrict entire dimensions by environment type (`NORMAL`, `NETHER`, `THE_END`).
  - Includes an `ignore-bypass` directive to ensure restrictions apply to server operators during testing and production.

- **Warmup and Interruption Handling**
  - Configurable teleportation warmup timer.
  - Detects player movement and cancels teleportation while ignoring head rotation (yaw and pitch).
  - Automatically aborts teleportation upon receiving damage during warmup.

- **Comprehensive Localization**
  - Bundled with 13 built-in language configurations. All messages and hover elements are fully customizable.

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

### Commands

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/tpa <player>` | None | Sends a teleportation request to the specified player | `nexuswarp.use` |
| `/tpaccept [player]` | `/tpyes` | Accepts an active incoming teleportation request | `nexuswarp.use` |
| `/tpacancel [player]` | `/tpdeny`, `/tpcancel`, `/tpno` | Cancels an active request (usable by sender or recipient) | `nexuswarp.use` |
| `/tp <player> [target]` | None | Teleports to a player or coordinates | `nexuswarp.tp` |
| `/msg <player> <message>` | `/w`, `/tell`, `/m`, `/whisper` | Sends a private message to a player | `nexuswarp.msg` |
| `/reply <message>` | `/r` | Replies to the last player who messaged you | `nexuswarp.msg` |
| `/nexuswarp reload` | `/nw`, `/tpreload` | Asynchronously reloads configuration and language files | `nexuswarp.admin` |

### Permissions

| Node | Description | Default Assignment |
| :--- | :--- | :--- |
| `nexuswarp.use` | Grants access to standard player TPA commands (`/tpa`, `/tpaccept`, `/tpacancel`) | `true` (all players) |
| `nexuswarp.msg` | Grants access to private messaging (`/msg`, `/reply`) | `true` (all players) |
| `nexuswarp.tp` | Grants access to direct teleportation (`/tp`) — configure via LuckPerms | `false` (no one by default) |
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

# Active language
# Supported: en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko
language: "en"

# World restriction settings
# Disabled by default
disabled-worlds:
  # Enable world restriction
  enabled: false

  # Ignore bypass permissions (true = operators are subject to restrictions)
  ignore-bypass: true

  # World names or dimension environment types:
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
  # Request expiration time in seconds (0 = disabled / request persists until resolved)
  timeout: 0

  # Warmup delay before execution in seconds (0 = instantaneous)
  teleport-delay: 0

  # Abort teleportation if the player moves during warmup
  cancel-on-movement: true

  # Abort teleportation if the player takes damage during warmup
  cancel-on-damage: true

# Safety validation prior to teleportation
safe-teleport:
  # Enable destination safety scanning
  enabled: true

  # Block teleportation into lava
  prevent-lava: true

  # Block teleportation into the void
  prevent-void: true

  # Block teleportation into solid suffocating blocks
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

1. Download `NexusWarp-1.1.jar` from the release section.
2. Place the file into your server's `plugins/` directory.
3. Start or reload the server to generate default files.
4. Adjust `plugins/NexusWarp/config.yml` according to your server requirements.
5. Execute `/nexuswarp reload` to apply configuration changes without downtime.

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
target/NexusWarp-1.1.jar
```

---

## License

This project is licensed under the [MIT License](LICENSE).
