package boti.doc.playertimer;

import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;
import boti.doc.timer.TimerColor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class TimerStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "timers.json";

    private final Path dataFolder;
    private final Logger logger;

    public TimerStore(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void save(Map<UUID, PlayerTimer> timers) {
        Map<String, TimerData> serializable = new HashMap<>();

        for (Map.Entry<UUID, PlayerTimer> entry : timers.entrySet()) {
            serializable.put(entry.getKey().toString(), TimerData.from(entry.getValue()));
        }

        try {
            Files.createDirectories(dataFolder);
            try (Writer writer = Files.newBufferedWriter(dataFolder.resolve(FILE_NAME))) {
                GSON.toJson(serializable, writer);
            }
        } catch (IOException e) { // we regard the timer as critical so failure needs to result in a server shutdown
            logger.severe("Failed to write timer data to " + dataFolder.resolve(FILE_NAME) +
                    ". Check file permissions and disk health. " + e.getMessage());
            Bukkit.shutdown();
        }
    }

    public Map<UUID, PlayerTimer> load() {
        Path file = dataFolder.resolve(FILE_NAME);

        if (!Files.exists(file)) return new HashMap<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<Map<String, TimerData>>() {}.getType();
            Map<String, TimerData> raw = GSON.fromJson(reader, type);

            if (raw == null) return new HashMap<>();

            Map<UUID, PlayerTimer> timers = new HashMap<>();
            for (Map.Entry<String, TimerData> entry : raw.entrySet()) {
                try {
                    timers.put(UUID.fromString(entry.getKey()), entry.getValue().toTimer());
                } catch (IllegalArgumentException e) {
                    // Corrupt entry — skip it and keep loading the rest
                    logger.warning("Skipping timer with invalid UUID: " + entry.getKey());
                }
            }
            return timers;

        } catch (IOException e) {
            throw new RuntimeException( // we are just disabling the plugin here
                    "Failed to read timer data from " + file + ". " +
                            "Check file permissions and disk health. Server cannot continue safely.", e
            );
        }
    }

    // Flat DTO — Gson serializes this directly
    private static class TimerData {
        String mode;
        String state;
        boolean visible;
        int time;
        String color;

        static TimerData from(PlayerTimer timer) {
            TimerData d = new TimerData();
            d.mode = timer.getMode().name();
            d.state = timer.getState().name();
            d.visible = timer.isVisible();
            d.time = timer.getTime();
            d.color = timer.getColor().name();
            return d;
        }

        PlayerTimer toTimer() {
            PlayerTimer timer = new PlayerTimer(
                    TimerMode.valueOf(mode),
                    time
            );
            // Pause running timers on restore — the server stopped, so they weren't ticking
            TimerState restored = TimerState.valueOf(state);
            if (restored == TimerState.RUNNING) restored = TimerState.PAUSED;

            // Use package-private or a restore method to set state without
            // triggering normal precondition checks
            TimerColor restoredColor;

            try {
                restoredColor = TimerColor.valueOf(color);
            } catch (Exception e) {
                restoredColor = TimerColor.WHITE;
            }

            timer.restore(
                    restored,
                    visible,
                    restoredColor
            );

            return timer;
        }
    }

}