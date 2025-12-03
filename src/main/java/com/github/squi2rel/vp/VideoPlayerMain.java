package com.github.squi2rel.vp;

import com.github.squi2rel.vp.network.ServerPacketHandler;
import com.github.squi2rel.vp.network.VideoPayload;
import com.github.squi2rel.vp.provider.VideoProviders;
import com.github.squi2rel.vp.video.StreamListener;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VideoPlayerMain implements ModInitializer {
    public static final String MOD_ID = "videoplayer";
    public static final String version = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion().toString();
    public static Throwable error = null;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, VideoPlayerMain::newDaemon);

    @SuppressWarnings("resource")
    @Override
    public void onInitialize() {
        try {
            StreamListener.load();
        } catch (Throwable e) {
            error = e;
            VideoPlayerMain.LOGGER.error("Cannot load vlc library", e);
            return;
        }
        VideoProviders.register();
        VideoPayload.register();
        ServerLifecycleEvents.SERVER_STARTED.register(DataHolder::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(DataHolder::stop);
        ServerTickEvents.START_WORLD_TICK.register(s -> DataHolder.update());
        ServerPlayConnectionEvents.JOIN.register((e, p, s) -> DataHolder.playerJoin(e.player));
        ServerPlayConnectionEvents.DISCONNECT.register((e, s) -> DataHolder.playerLeave(e.player.getUuid()));
        ServerPlayNetworking.registerGlobalReceiver(VideoPayload.ID, (p, c) -> c.server().execute(() -> {
            ByteBuf buf = Unpooled.wrappedBuffer(p.data());
            try {
                ServerPacketHandler.handle(c.player(), buf);
            } catch (Exception e) {
                c.player().networkHandler.disconnect(Text.of(e.toString()));
            } finally {
                buf.release();
            }
        }));
        CommandRegistrationCallback.EVENT.register((d, c, e) -> d.register(CommandManager.literal("").then(CommandManager.argument("command", StringArgumentType.greedyString()).executes(s -> {
            if (!s.getSource().isExecutedByPlayer()) return 0;
            ServerPacketHandler.sendTo(s.getSource().getPlayer(), ServerPacketHandler.execute(s.getArgument("command", String.class)));
            return 1;
        }))));
    }

    private static Thread newDaemon(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        return t;
    }
}
