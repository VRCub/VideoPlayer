package com.github.squi2rel.vp;

import com.github.squi2rel.vp.network.VideoPayload;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.provider.VideoProviders;
import com.github.squi2rel.vp.video.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"resource", "DataFlowIssue"})
public class VideoPlayerClient implements ClientModInitializer {
    public static final HashMap<String, ClientVideoArea> areas = new HashMap<>();
    public static final ArrayList<IVideoPlayer> players = new ArrayList<>();
    public static boolean updated = false;
    private static final TouchHandler touchHandler = new TouchHandler();
    private static ClientVideoScreen currentLooking, currentScreen;
    private static BossBar bossBar = null;
    private static boolean bossBarAdded = false;

    public static boolean connected = false;
    public static String remoteControlName = "minecraft:iron_ingot";
    public static float remoteControlId = -1;
    public static float remoteControlRange = 64;
    public static float noControlRange = 16;

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_AREAS = (context, builder) -> {
        for (ClientVideoArea a : areas.values()) {
            if (a.name.startsWith(builder.getRemaining())) {
                builder.suggest(a.name);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_SCREENS = (context, builder) -> {
        ClientVideoArea area = areas.get(context.getArgument("area", String.class));
        if (area == null) return Suggestions.empty();
        for (VideoScreen screen : area.screens) {
            if (screen.name.startsWith(builder.getRemaining())) {
                builder.suggest(screen.name);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_REAL_SCREENS = (context, builder) -> {
        ClientVideoArea area = areas.get(context.getArgument("area", String.class));
        if (area == null) return Suggestions.empty();
        for (VideoScreen screen : area.screens) {
            if (!screen.source.isEmpty()) continue;
            if (screen.name.startsWith(builder.getRemaining())) {
                builder.suggest(screen.name);
            }
        }
        return builder.buildFuture();
    };

    @Override
    public void onInitializeClient() {
        VlcDecoder.load();
        VideoProviders.register();
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> c.execute(() -> { // TODO why don't works
            connected = false;
            for (ClientVideoArea area : areas.values()) {
                area.remove();
            }
            areas.clear();
            for (IVideoPlayer player : players) {
                player.cleanup();
            }
            players.clear();
            currentLooking = null;
        }));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::render);
        ClientTickEvents.START_CLIENT_TICK.register(c -> updated = false);
        ClientPlayNetworking.registerGlobalReceiver(VideoPayload.ID, (p, c) -> MinecraftClient.getInstance().execute(() -> ClientPacketHandler.handle(Unpooled.wrappedBuffer(p.data()))));
        ClientCommandRegistrationCallback.EVENT.register((d, c) -> d.register(ClientCommandManager.literal("vlc")
                .then(ClientCommandManager.literal("play")
                        .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(s -> {
                                    if (checkInvalid(s, true)) return 0;
                                    ClientPacketHandler.request(currentScreen, s.getArgument("url", String.class));
                                    return 1;
                                })
                        ))
                .then(ClientCommandManager.literal("skip")
                        .then(ClientCommandManager.argument("force", BoolArgumentType.bool())
                                .executes(s -> {
                                    if (checkInvalid(s, true)) return 0;
                                    ClientPacketHandler.skip(currentScreen, s.getArgument("force", Boolean.class));
                                    return 1;
                                })
                        )
                        .executes(s -> {
                            if (checkInvalid(s, true)) return 0;
                            ClientPacketHandler.skip(currentScreen, false);
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("volume")
                        .then(ClientCommandManager.argument("volume", IntegerArgumentType.integer(0, 100))
                                .executes(s -> {
                                    int v = s.getArgument("volume", Integer.class);
                                    players.getFirst().setVolume(v);
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("createArea")
                        .then(ClientCommandManager.argument("x1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("x2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                .executes(s -> {
                                    if (checkInvalid(s, false)) return 0;
                                    ClientPacketHandler.createArea(
                                            new Vector3f(
                                                s.getArgument("x1", Float.class),
                                                s.getArgument("y1", Float.class),
                                                s.getArgument("z1", Float.class)
                                            ),
                                            new Vector3f(
                                                s.getArgument("x2", Float.class),
                                                s.getArgument("y2", Float.class),
                                                s.getArgument("z2", Float.class)
                                            ),
                                            s.getArgument("name", String.class)
                                    );
                                    return 1;
                                })))
                        )))))
                )
                .then(ClientCommandManager.literal("removeArea")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string()).suggests(SUGGEST_AREAS)
                                .executes(s -> {
                                    if (checkInvalid(s, false)) return 0;
                                    String name = s.getArgument("name", String.class);
                                    ClientPacketHandler.removeArea(name);
                                    return 1;
                                }))
                )
                .then(ClientCommandManager.literal("createScreen")
                        .then(ClientCommandManager.argument("area", StringArgumentType.string()).suggests(SUGGEST_AREAS)
                        .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .then(ClientCommandManager.argument("x1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z1", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("x2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z2", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("x3", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y3", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z3", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("x4", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("y4", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("z4", FloatArgumentType.floatArg())
                        .then(ClientCommandManager.argument("source", StringArgumentType.string()).suggests(SUGGEST_REAL_SCREENS)
                                .executes(s -> {
                                    VideoArea area = getArea(s);
                                    if (area == null) return 0;
                                    ClientPacketHandler.createScreen(new VideoScreen(
                                            area,
                                            s.getArgument("name", String.class),
                                            new Vector3f(
                                                    s.getArgument("x1", Float.class),
                                                    s.getArgument("y1", Float.class),
                                                    s.getArgument("z1", Float.class)
                                            ),
                                            new Vector3f(
                                                    s.getArgument("x2", Float.class),
                                                    s.getArgument("y2", Float.class),
                                                    s.getArgument("z2", Float.class)
                                            ),
                                            new Vector3f(
                                                    s.getArgument("x3", Float.class),
                                                    s.getArgument("y3", Float.class),
                                                    s.getArgument("z3", Float.class)
                                            ),
                                            new Vector3f(
                                                    s.getArgument("x4", Float.class),
                                                    s.getArgument("y4", Float.class),
                                                    s.getArgument("z4", Float.class)
                                            ),
                                            s.getArgument("source", String.class)
                                            ));
                                    return 1;
                                })
                        )))))))))))))))
                )
                .then(ClientCommandManager.literal("removeScreen")
                        .then(ClientCommandManager.argument("area", StringArgumentType.string()).suggests(SUGGEST_AREAS)
                                .then(ClientCommandManager.argument("name", StringArgumentType.string()).suggests(SUGGEST_SCREENS)
                                        .executes(s -> {
                                            VideoArea area = getArea(s);
                                            if (area == null) return 0;
                                            String screenName = s.getArgument("name", String.class);
                                            VideoScreen screen = area.getScreen(screenName);
                                            if (screen == null) {
                                                s.getSource().sendFeedback(Text.of("没有名为 " + screenName + " 的屏幕"));
                                                return 0;
                                            }
                                            ClientPacketHandler.removeScreen(screen);
                                            return 1;
                                        })))
                )
                .then(ClientCommandManager.literal("skipPercent")
                        .then(ClientCommandManager.argument("percent", FloatArgumentType.floatArg(0, 1.01f))
                                .executes(s -> {
                                    if (checkInvalid(s, true)) return 0;
                                    ClientPacketHandler.skipPercent(currentScreen, s.getArgument("percent", Float.class));
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("list")
                        .executes(s -> {
                            if (currentScreen == null) {
                                s.getSource().sendFeedback(Text.of("当前没有在观影区内"));
                                return 0;
                            }
                            s.getSource().sendFeedback(Text.literal(currentScreen.infos.stream()
                                    .map(i -> String.format("%s 请求玩家: %s", i.name(), i.playerName()))
                                    .collect(Collectors.joining("\n"))
                            ).formatted(Formatting.GOLD));
                            return 1;
                        })
                )
                .then(ClientCommandManager.literal("sync")
                        .executes(s -> {
                            if (currentScreen == null) {
                                s.getSource().sendFeedback(Text.literal("当前没有在观影区内").formatted(Formatting.RED));
                                return 0;
                            }
                            ClientPacketHandler.sync(currentScreen);
                            return 1;
                        })
                )
        ));
        bossBar = new ClientBossBar(UUID.randomUUID(), Text.of(""), 0, BossBar.Color.WHITE, BossBar.Style.PROGRESS, false, false, false);
    }

    private VideoArea getArea(CommandContext<FabricClientCommandSource> s) {
        if (checkInvalid(s, false)) return null;
        String name = s.getArgument("area", String.class);
        VideoArea area = areas.get(name);
        if (area == null) {
            s.getSource().sendFeedback(Text.literal("没有名为 " + name + " 的区域").formatted(Formatting.RED));
            return null;
        }
        return area;
    }

    private boolean checkInvalid(CommandContext<FabricClientCommandSource> s, boolean checkScreen) {
        if (!connected) {
            s.getSource().sendFeedback(Text.literal("未连接到服务器").formatted(Formatting.RED));
            return true;
        }
        if (checkScreen && currentScreen == null) {
            s.getSource().sendFeedback(Text.literal("当前没有在观影区内").formatted(Formatting.RED));
            return true;
        }
        return false;
    }

    private void render(WorldRenderContext ctx) {
        Profilers.get().push("video");
        Profilers.get().push("checkInteract");
        checkInteract(ctx);
        Profilers.get().swap("updateBossBar");
        if (currentLooking != null) {
            ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            if (!bossBarAdded) {
                handler.onBossBar(BossBarS2CPacket.add(bossBar));
                bossBarAdded = true;
            }
            VideoInfo info = currentLooking.infos.peek();
            if (info != null) {
                String name = info.name();
                long progress = System.currentTimeMillis() - currentLooking.getStartTime();
                long totalProgress = currentLooking.player.getTotalProgress();
                String time;
                if (totalProgress > 0) {
                    boolean showHour = progress >= 3600000 || totalProgress >= 3600000;
                    time = formatDuration(progress, showHour) + "/" + formatDuration(totalProgress, showHour);
                } else {
                    time = formatDuration(progress, progress >= 3600000) + "/LIVE";
                }
                bossBar.setName(Text.of(name + " " + time));
                bossBar.setPercent((float) progress / totalProgress);
            } else {
                bossBar.setName(Text.of("无"));
                bossBar.setPercent(1);
            }
            handler.onBossBar(BossBarS2CPacket.updateName(bossBar));
            handler.onBossBar(BossBarS2CPacket.updateProgress(bossBar));
        } else if (bossBarAdded) {
            ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            handler.onBossBar(BossBarS2CPacket.remove(bossBar.getUuid()));
            bossBarAdded = false;
        }
        Profilers.get().swap("updateFrame");
        if (!updated) {
            for (IVideoPlayer player : players) {
                player.updateTexture();
            }
            updated = true;
        }
        Profilers.get().swap("render");
        MatrixStack matrices = ctx.matrixStack();
        Vec3d camera = ctx.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.disableCull();
        for (IVideoPlayer player : players) {
            player.draw(mat);
        }
        matrices.pop();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        Profilers.get().pop();
        Profilers.get().pop();
    }

    private void checkInteract(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (players.isEmpty()) {
            currentLooking = null;
            currentScreen = null;
            touchHandler.handle(null);
            return;
        }

        currentScreen = null;
        for (ClientVideoArea area : areas.values()) {
            if (!area.loaded) continue;
            for (VideoScreen screen : area.screens) {
                if (screen instanceof ClientVideoScreen s) {
                    currentScreen = s;
                    break;
                }
            }
            break;
        }

        float delta = ctx.tickCounter().getTickDelta(true);
        Vec3d eyePos = client.player.getCameraPosVec(delta);
        Vec3d lookVec = client.player.getRotationVec(delta);

        Vector3f lineStart = new Vector3f(eyePos.toVector3f());

        boolean remoteControl = false;
        for (ItemStack item : client.player.getHandItems()) {
            if (!Registries.ITEM.getId(item.getItem()).toString().equals(remoteControlName)) continue;
            CustomModelDataComponent data = item.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
            if (data == null) continue;
            List<Float> id = data.floats();
            if (id.isEmpty() || !id.contains(remoteControlId)) continue;
            remoteControl = true;
        }
        Vector3f lineEnd = eyePos.add(lookVec.multiply(remoteControl ? remoteControlRange : noControlRange)).toVector3f();

        ArrayList<Intersection.Result> list = new ArrayList<>();
        for (IVideoPlayer player : players) {
            Intersection.Result result = Intersection.intersect(lineStart, lineEnd, player.getScreen());
            if (result.intersects) list.add(result);
        }
        Intersection.Result target = list.isEmpty() ? null : Collections.min(list, Comparator.comparing(s -> s.distance));
        currentLooking = target == null || target.player == null ? null : (ClientVideoScreen) target.player;
        touchHandler.handle(target);
    }

    public static boolean checkVersion(String v) {
        String[] p1 = StringUtils.split(v, '.');
        String[] p2 = StringUtils.split(VideoPlayerMain.version, '.');
        if (p1.length < 2 || p2.length < 2) return false;
        return p1[0].equals(p2[0]) && p1[1].equals(p2[1]);
    }

    private static String formatDuration(long millis, boolean showHour) {
        long all = millis / 1000;
        long hours = all / 3600;
        long minutes = (all % 3600) / 60;
        long seconds = all % 60;

        if (showHour) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}