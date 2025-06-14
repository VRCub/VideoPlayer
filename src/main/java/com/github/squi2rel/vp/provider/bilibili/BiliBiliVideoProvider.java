package com.github.squi2rel.vp.provider.bilibili;

import com.github.squi2rel.vp.provider.IProviderSource;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliBiliVideoProvider extends BiliBiliProvider {
    public static final String FETCH_URL = "https://api.bilibili.com/x/web-interface/view?bvid=%s";
    public static final String PLAY_URL = "https://api.bilibili.com/x/player/playurl?bvid=%s&cid=%s&qn=80&platform=html5";
    public static final Pattern REGEX = Pattern.compile("(?<=^|/)BV[0-9A-Za-z]{10}");

    @Override
    public @Nullable CompletableFuture<VideoInfo> from(String str, IProviderSource source) {
        Matcher matcher = REGEX.matcher(str);
        if (!matcher.find()) return null;
        String bvid = matcher.group();
        return CompletableFuture.supplyAsync(() -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.send(makeRequest(String.format(FETCH_URL, bvid)), HttpResponse.BodyHandlers.ofString());
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("data");
                return new VideoMeta(root.get("title").getAsString(), root.get("cid").getAsString());
            } catch (Exception e) {
                source.reply(e.toString());
                return null;
            }
        }).thenApply(meta -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.send(makeRequest(String.format(PLAY_URL, bvid, meta.cid())), HttpResponse.BodyHandlers.ofString());
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                String url = root.getAsJsonObject("data").getAsJsonArray("durl").get(0).getAsJsonObject().get("url").getAsString();
                return new VideoInfo(source.name(), meta.title(), url, bvid, System.currentTimeMillis() + 1000 * 60 * 60 * 2, true, VLC_PARAMS);
            } catch (Exception e) {
                source.reply(e.toString());
                return null;
            }
        });
    }
}
