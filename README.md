[🇨🇳 中文](README.zh.md)

# DisCraft

> Minecraft 1.19.2 Fabric & Forge client mod — bridges in-game chat with Discord, with automatic voice channel joining

## Features

- **Game → Discord**: Player chat forwarded to Discord via Webhook in real time
- **Auto voice channel**: Auto-joins configured Discord voice channel on world/server join; leaves on disconnect (requires Discord running in background)
- **Multi-context mappings**: Per-world name / server IP independent Discord channel config
- **Event notifications**: Optional join, leave, death event forwarding to Discord
- **GUI config**: Press `G` to open settings — no config file editing needed

## Download

Latest build is available in [Releases](https://github.com/AlbertHuangT/discraft/releases/tag/latest):
- `discraft-1.0.0.jar` — Fabric (requires Fabric API)
- `discraft-forge-1.0.0.jar` — Forge

## Installation

1. **Fabric**: Requires [Fabric Loader](https://fabricmc.net/) ≥ 0.14.9 + [Fabric API](https://modrinth.com/mod/fabric-api) 0.76.x (1.19.2)
2. Drop the JAR into your Minecraft `mods/` folder
3. Launch the game, press `G` to open DisCraft settings

## Configuration

### Chat bridge only (Game → Discord)

Only a Webhook URL is needed — no Bot Token required:

1. Discord channel settings → Integrations → Webhooks → New Webhook → Copy URL
2. Press `G` → Add mapping → Enter Webhook URL → Enable

### Voice channel auto-switching

Uses Discord IPC to control the local Discord client. Requires a Discord application and one-time OAuth2 authorization:

1. Go to [Discord Developer Portal](https://discord.com/developers/applications) and create a new application
2. Under **OAuth2**, add `http://localhost` as a redirect URL and enable the **RPC** scope
3. Copy the **Client ID** and **Client Secret**
4. In-game: press `G` → fill in Client ID and Client Secret at the top → click **Save App Settings**
5. Click **Authorize Discord (required for voice)** → approve in the Discord client popup
6. In each mapping, set the target voice channel ID — it will auto-join on context switch

> Credentials are stored in `config/discraft/config.json` and only need to be set once.

## Building

Requires JDK 17+:

```bash
# Fabric
./gradlew build
# Output: build/libs/discraft-1.0.0.jar

# Forge
cd forge && ./gradlew build
# Output: forge/build/libs/discraft-forge-1.0.0.jar
```

## Context Key Format

| Scenario | Key format | Example |
|----------|-----------|---------|
| Singleplayer world | `world:<name>` | `world:My Survival` |
| Multiplayer server | `server:<ip>` | `server:hypixel.net` |

Config stored at: `<minecraft>/config/discraft/config.json`

## License

MIT

## Credits

Color scheme and author format for Embed notifications adapted from [jenchanws/discraft](https://github.com/jenchanws/discraft) (MIT). See [CREDITS.md](CREDITS.md).
