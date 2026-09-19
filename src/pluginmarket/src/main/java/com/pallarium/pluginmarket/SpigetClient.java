package com.pallarium.pluginmarket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pallarium.pluginmarket.model.Category;
import com.pallarium.pluginmarket.model.PluginInfo;
import com.pallarium.pluginmarket.model.SortMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Thin client for the public Spiget REST API (spiget.org) - resource search, listing and download. */
public class SpigetClient {
    private static final String BASE = "https://api.spiget.org/v2";
    private final String userAgent;

    public SpigetClient(String userAgent) {
        this.userAgent = userAgent;
    }

    public List<PluginInfo> browse(Category category, SortMode sort, String query, int page, int pageSize) throws IOException {
        String field = "id,name,tag,author,downloads,rating,updateDate,releaseDate,version,premium,external,file,icon,testedVersions";
        StringBuilder url = new StringBuilder(BASE);
        boolean searching = query != null && !query.trim().isEmpty();
        if (searching) {
            url.append("/search/resources/").append(URLEncoder.encode(query.trim(), StandardCharsets.UTF_8))
               .append("?field=").append(field);
        } else {
            url.append("/resources?field=").append(field);
        }
        url.append("&size=").append(pageSize)
           .append("&page=").append(page)
           .append("&sort=").append(sort.spigetSort);
        if (category == Category.SKRIPT) {
            url.append("&category=25");
        } else if (category == Category.SPIGOT) {
            url.append("&category=4");
        }
        JsonElement root = get(url.toString());
        List<PluginInfo> out = new ArrayList<>();
        if (root == null || !root.isJsonArray()) return out;
        for (JsonElement el : root.getAsJsonArray()) {
            PluginInfo info = parse(el.getAsJsonObject(), category);
            if (info != null) {
                if (category == Category.BUKKIT && info.premium) continue;
                out.add(info);
            }
        }
        return out;
    }

    private PluginInfo parse(JsonObject o, Category category) {
        PluginInfo p = new PluginInfo();
        p.id = o.get("id").getAsInt();
        p.name = str(o, "name", "Unknown");
        p.tag = str(o, "tag", "");
        JsonObject author = o.getAsJsonObject("author");
        p.author = author != null && author.has("name") ? author.get("name").getAsString() : "unknown";
        p.downloads = o.has("downloads") ? o.get("downloads").getAsInt() : 0;
        JsonObject rating = o.getAsJsonObject("rating");
        if (rating != null) {
            p.rating = rating.has("average") ? rating.get("average").getAsDouble() : 0;
            p.ratingCount = rating.has("count") ? rating.get("count").getAsInt() : 0;
        }
        p.updateEpochMs = o.has("updateDate") ? o.get("updateDate").getAsLong() * 1000L : 0;
        p.releaseEpochMs = o.has("releaseDate") ? o.get("releaseDate").getAsLong() * 1000L : 0;
        p.version = o.has("version") && o.get("version").isJsonObject()
                ? str(o.getAsJsonObject("version"), "name", "?") : "?";
        p.premium = o.has("premium") && o.get("premium").getAsBoolean();
        JsonObject file = o.getAsJsonObject("file");
        p.externalDownload = file != null && file.has("externalUrl") && !file.get("externalUrl").isJsonNull();
        JsonObject icon = o.getAsJsonObject("icon");
        p.iconUrl = icon != null && icon.has("url") ? icon.get("url").getAsString() : "";
        p.category = category;
        p.nativeMinecraft = true;
        return p;
    }

    private String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    /** Returns the resolved direct-download URL for a resource id (following Spiget's redirect/CDN indirection). */
    public String resolveDownloadUrl(int resourceId) {
        return BASE + "/resources/" + resourceId + "/download";
    }

    public byte[] download(String urlStr) throws IOException {
        HttpURLConnection c = open(urlStr);
        c.setInstanceFollowRedirects(true);
        int redirects = 0;
        while (true) {
            int code = c.getResponseCode();
            if ((code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && redirects < 5) {
                String loc = c.getHeaderField("Location");
                if (loc == null) break;
                c = open(loc);
                redirects++;
                continue;
            }
            break;
        }
        try (InputStream in = wrap(c)) {
            return in.readAllBytes();
        }
    }

    private HttpURLConnection open(String urlStr) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", userAgent);
        c.setRequestProperty("Accept-Encoding", "gzip");
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(false);
        return c;
    }

    private InputStream wrap(HttpURLConnection c) throws IOException {
        InputStream raw = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        if ("gzip".equalsIgnoreCase(c.getContentEncoding())) return new GZIPInputStream(raw);
        return raw;
    }

    private JsonElement get(String urlStr) throws IOException {
        HttpURLConnection c = open(urlStr);
        try (Reader r = new InputStreamReader(wrap(c), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r);
        }
    }
}
