package com.pallarium.logviewer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads logs/latest.log fresh each call - cheap enough for a GUI open, always current. */
public class LogReader {
    private final File file;

    public LogReader(File serverRoot) {
        this.file = new File(serverRoot, "logs/latest.log");
    }

    public List<String> lines() {
        try {
            if (!file.exists()) return List.of();
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of("[LogViewer] Could not read latest.log: " + e.getMessage());
        }
    }

    public List<String> filtered(LogFilter filter, String query) {
        List<String> out = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        for (String l : lines()) {
            if (l.isBlank()) continue;
            if (!filter.matches(l)) continue;
            if (!q.isEmpty() && !l.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(l);
        }
        return out;
    }

    /** Strip the "[HH:MM:SS] [Thread/LEVEL]: " prefix into (time, level, message). */
    public static String[] split(String line) {
        String time = "", level = "INFO", msg = line;
        if (line.startsWith("[") && line.length() > 10 && line.charAt(9) == ']') {
            time = line.substring(1, 9);
            int a = line.indexOf('[', 10);
            int b = line.indexOf("]: ", a);
            if (a > 0 && b > a) {
                String thread = line.substring(a + 1, b);
                int slash = thread.lastIndexOf('/');
                if (slash >= 0) level = thread.substring(slash + 1);
                msg = line.substring(b + 3);
            }
        }
        return new String[]{time, level, msg};
    }
}
