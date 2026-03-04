package com.discraft.config;

/**
 * 单个存档/服务器与 Discord 频道的映射配置。
 * key 格式：
 *   单人存档 -> "world:存档名"
 *   多人服务器 -> "server:ip:port"
 */
public class WorldMapping {

    /** 用户自定义的友好显示名，例如"我的生存服" */
    public String displayName = "";

    /** Discord Webhook URL，用于从游戏发送消息到 Discord */
    public String webhookUrl = "";

    /** Discord 频道 ID，用于接收 Discord 消息（需要 Bot Token） */
    public String channelId = "";

    /** 是否启用此映射 */
    public boolean enabled = true;

    /** 是否将游戏内聊天转发到 Discord */
    public boolean sendToDiscord = true;

    /** 是否将 Discord 消息显示在游戏内 */
    public boolean receiveFromDiscord = true;

    /** 是否在 Discord 显示玩家加入/离开事件 */
    public boolean showJoinLeave = true;

    /** 是否在 Discord 显示玩家死亡事件 */
    public boolean showDeaths = false;
}
