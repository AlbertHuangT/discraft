package com.discraft.discord;

import com.discraft.DisCraft;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;

public class DiscordIpc {

    private static final Logger LOGGER = LoggerFactory.getLogger("discraft");

    public static final String CLIENT_ID = "1478939568475738314";
    private static final String CLIENT_SECRET = "UpDyery6xI0uuglPhz5isFABwu27qHeq";

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private InputStream ipcIn;
    private OutputStream ipcOut;
    private Closeable ipcChannel;

    private volatile boolean connected = false;
    private volatile boolean authenticated = false;

    public synchronized boolean ensureConnected() {
        if (authenticated) return true;
        if (!connected) {
            if (!doConnect()) {
                LOGGER.info("[DisCraft] Discord 未运行，IPC 连接失败");
                return false;
            }
            if (!doHandshake()) {
                doDisconnect();
                return false;
            }
        }
        String token = DisCraft.CONFIG.discordAccessToken;
        if (token != null && !token.isBlank()) {
            try {
                if (doAuthenticate(token)) {
                    authenticated = true;
                    LOGGER.info("[DisCraft] Discord IPC 认证成功（使用已保存 Token）");
                    return true;
                } else {
                    DisCraft.CONFIG.discordAccessToken = "";
                    DisCraft.CONFIG.save();
                    LOGGER.info("[DisCraft] 已保存的 Discord Token 已过期，需要重新授权");
                }
            } catch (IOException e) {
                LOGGER.error("[DisCraft] 认证失败", e);
            }
        }
        return false;
    }

    public synchronized void startAuth(Minecraft mc) {
        if (!connected) {
            if (!doConnect() || !doHandshake()) {
                showInfo(mc, "§7[DisCraft] Discord 未运行，无法进行授权");
                return;
            }
        }

        try {
            showInfo(mc, "§e[DisCraft] 正在请求 Discord 授权，请在 Discord 客户端中点击授权...");

            JsonObject args = new JsonObject();
            args.addProperty("client_id", CLIENT_ID);
            JsonArray scopes = new JsonArray();
            scopes.add("rpc");
            scopes.add("rpc.voice.write");
            args.add("scopes", scopes);

            send(OP_FRAME, buildCommand("AUTHORIZE", args).toString());

            JsonObject resp = readFrame();
            LOGGER.info("[DisCraft] AUTHORIZE 响应: {}", resp);
            if (isError(resp)) {
                showInfo(mc, "§c[DisCraft] 授权被拒绝或失败");
                return;
            }

            if (!resp.has("data") || resp.get("data").isJsonNull()
                    || !resp.getAsJsonObject("data").has("code")) {
                LOGGER.error("[DisCraft] AUTHORIZE 响应格式异常: {}", resp);
                showInfo(mc, "§c[DisCraft] Discord 授权响应异常，请检查 Developer Portal 中的 APPLICATION_ID 配置");
                return;
            }

            String code = resp.getAsJsonObject("data").get("code").getAsString();

            showInfo(mc, "§e[DisCraft] 正在获取 Token...");
            String token = exchangeCodeForToken(code);
            if (token == null) {
                showInfo(mc, "§c[DisCraft] Token 获取失败，请检查 Discord 应用配置（CLIENT_ID/CLIENT_SECRET）");
                return;
            }

            if (doAuthenticate(token)) {
                authenticated = true;
                DisCraft.CONFIG.discordAccessToken = token;
                DisCraft.CONFIG.save();
                showInfo(mc, "§a[DisCraft] Discord 授权成功！进入存档时将自动加入语音频道");
            } else {
                showInfo(mc, "§c[DisCraft] Discord 认证失败，请重试");
            }

        } catch (Exception e) {
            LOGGER.error("[DisCraft] 授权流程失败", e);
            showInfo(mc, "§c[DisCraft] 授权失败: " + e.getMessage());
        }
    }

    public synchronized void selectVoiceChannel(String channelId, Minecraft mc) {
        if (!authenticated) return;

        try {
            JsonObject args = new JsonObject();
            if (channelId != null && !channelId.isBlank()) {
                args.addProperty("channel_id", channelId);
                args.addProperty("force", true);
            } else {
                args.add("channel_id", JsonNull.INSTANCE);
            }

            send(OP_FRAME, buildCommand("SELECT_VOICE_CHANNEL", args).toString());
            JsonObject resp = readFrame();

            if (isError(resp)) {
                int code = 0;
                try {
                    code = resp.getAsJsonObject("data").get("code").getAsInt();
                } catch (Exception ignored) {}
                if (code == 4005) {
                    showInfo(mc, "§c[DisCraft] 你不在该 Discord 服务器，请先加入后再使用语音功能");
                } else {
                    LOGGER.warn("[DisCraft] SELECT_VOICE_CHANNEL 错误 (code={}): {}", code, resp);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[DisCraft] 发送语音频道选择命令失败", e);
            connected = false;
            authenticated = false;
        }
    }

    public synchronized void disconnect() {
        doDisconnect();
    }

    public boolean isConnected() {
        return connected && authenticated;
    }

    private boolean doConnect() {
        String os = System.getProperty("os.name", "").toLowerCase();
        for (int i = 0; i <= 9; i++) {
            try {
                if (os.contains("win")) {
                    String pipeName = "\\\\.\\pipe\\discord-ipc-" + i;
                    RandomAccessFile raf = new RandomAccessFile(pipeName, "rw");
                    this.ipcIn = new FileInputStream(raf.getFD());
                    this.ipcOut = new FileOutputStream(raf.getFD());
                    this.ipcChannel = raf;
                } else {
                    String tmpDir = System.getenv("XDG_RUNTIME_DIR");
                    if (tmpDir == null) tmpDir = System.getenv("TMPDIR");
                    if (tmpDir == null) tmpDir = "/tmp";
                    var address = UnixDomainSocketAddress.of(Paths.get(tmpDir, "discord-ipc-" + i));
                    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                    channel.connect(address);
                    this.ipcIn = Channels.newInputStream(channel);
                    this.ipcOut = Channels.newOutputStream(channel);
                    this.ipcChannel = channel;
                }
                this.connected = true;
                LOGGER.info("[DisCraft] 已连接 Discord IPC (discord-ipc-{})", i);
                return true;
            } catch (IOException ignored) {}
        }
        return false;
    }

    private boolean doHandshake() {
        try {
            JsonObject hs = new JsonObject();
            hs.addProperty("v", 1);
            hs.addProperty("client_id", CLIENT_ID);
            send(OP_HANDSHAKE, hs.toString());
            JsonObject resp = readFrame();
            if (isError(resp)) {
                LOGGER.error("[DisCraft] IPC 握手失败: {}", resp);
                return false;
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("[DisCraft] IPC 握手异常", e);
            return false;
        }
    }

    private boolean doAuthenticate(String token) throws IOException {
        JsonObject args = new JsonObject();
        args.addProperty("access_token", token);
        send(OP_FRAME, buildCommand("AUTHENTICATE", args).toString());
        JsonObject resp = readFrame();
        return !isError(resp);
    }

    private void doDisconnect() {
        connected = false;
        authenticated = false;
        if (ipcChannel != null) {
            try { ipcChannel.close(); } catch (IOException ignored) {}
            ipcChannel = null;
        }
        ipcIn = null;
        ipcOut = null;
    }

    private void send(int opcode, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(opcode);
        header.putInt(body.length);
        ipcOut.write(header.array());
        ipcOut.write(body);
        ipcOut.flush();
    }

    private JsonObject readFrame() throws IOException {
        byte[] header = readExact(8);
        int opcode = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int length = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

        if (opcode == OP_CLOSE) {
            connected = false;
            authenticated = false;
            throw new IOException("Discord IPC 连接已关闭");
        }

        byte[] body = readExact(length);
        String json = new String(body, StandardCharsets.UTF_8);
        LOGGER.info("[DisCraft] IPC 收到: {}", json);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private byte[] readExact(int n) throws IOException {
        byte[] buf = new byte[n];
        int read = 0;
        while (read < n) {
            int r = ipcIn.read(buf, read, n - read);
            if (r == -1) throw new EOFException("IPC 流意外关闭");
            read += r;
        }
        return buf;
    }

    private String exchangeCodeForToken(String code) {
        try {
            String body = "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();

            if (json.has("access_token")) {
                return json.get("access_token").getAsString();
            }
            LOGGER.error("[DisCraft] Token 交换失败: {}", resp.body());
        } catch (Exception e) {
            LOGGER.error("[DisCraft] Token 交换请求异常", e);
        }
        return null;
    }

    private JsonObject buildCommand(String cmd, JsonObject args) {
        JsonObject obj = new JsonObject();
        obj.addProperty("cmd", cmd);
        obj.addProperty("nonce", UUID.randomUUID().toString());
        obj.add("args", args);
        return obj;
    }

    private boolean isError(JsonObject resp) {
        if (!resp.has("evt")) return false;
        JsonElement evt = resp.get("evt");
        return !evt.isJsonNull() && "ERROR".equals(evt.getAsString());
    }

    private void showInfo(Minecraft mc, String message) {
        mc.execute(() -> {
            if (mc.gui != null) {
                mc.gui.getChat().addMessage(Component.literal(message));
            }
        });
    }
}
