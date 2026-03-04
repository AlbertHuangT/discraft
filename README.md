# DisCraft

> Minecraft 1.19.2 Fabric 客户端 Mod — 双向桥接游戏内聊天与 Discord

## 功能

- **游戏 → Discord**：玩家聊天消息通过 Webhook 实时转发到 Discord 频道
- **Discord → 游戏**：通过 Bot Gateway WebSocket 接收 Discord 消息，显示在游戏聊天栏
- **多上下文映射**：按存档名 / 服务器 IP 独立配置不同的 Discord 频道
- **事件通知**：可选转发加入、离开、死亡事件到 Discord
- **图形配置界面**：按 `G` 键打开设置，无需手动编辑配置文件

## 安装

1. 前置依赖：[Fabric Loader](https://fabricmc.net/) ≥ 0.14.9 + [Fabric API](https://modrinth.com/mod/fabric-api) 0.76.x（1.19.2）
2. 下载 `discraft-1.0.0.jar`，放入 Minecraft `mods/` 文件夹
3. 启动游戏，按 `G` 键打开 DisCraft 设置

## 配置

### 仅发送消息（游戏 → Discord）

只需 Webhook URL，无需 Bot Token：

1. Discord 频道设置 → 整合 → Webhook → 新 Webhook → 复制 URL
2. 按 `G` → 添加新映射 → 填入 Webhook URL → 启用

### 收发双向（Discord → 游戏）

需要 Discord Bot Token，并在 [Discord Developer Portal](https://discord.com/developers/applications) 开启：

- `MESSAGE CONTENT INTENT`（必须）

操作步骤：

1. 创建 Bot，复制 Token
2. 按 `G` → 填入 Bot Token → 保存 Token
3. 编辑映射 → 填入目标 Discord 频道 ID → 勾选"接收 Discord 消息"

## 构建

需要 JDK 17+：

```bash
./gradlew build
# 产物：build/libs/discraft-1.0.0.jar
```

## 项目结构

```
src/main/java/com/discraft/
├── DisCraft.java              # 入口点，事件注册，快捷键
├── bridge/
│   └── ChatBridgeManager.java # 桥接核心，上下文管理
├── config/
│   ├── DisCraftConfig.java    # 配置 POJO，Gson 序列化
│   └── WorldMapping.java      # 单个频道映射配置
├── discord/
│   ├── GatewayClient.java     # Discord Gateway WebSocket
│   └── WebhookClient.java     # Discord Webhook HTTP 客户端
├── gui/
│   ├── ConfigScreen.java      # 主设置界面
│   ├── MappingEditScreen.java # 编辑映射
│   └── AddMappingScreen.java  # 新增映射
└── mixin/
    └── ChatScreenMixin.java   # 拦截聊天消息
```

## 上下文 Key 格式

| 场景 | Key 格式 | 示例 |
|------|---------|------|
| 单人存档 | `world:<存档名>` | `world:My Survival` |
| 多人服务器 | `server:<ip>` | `server:hypixel.net` |

配置文件存储于：`<minecraft>/config/discraft/config.json`

## License

MIT
