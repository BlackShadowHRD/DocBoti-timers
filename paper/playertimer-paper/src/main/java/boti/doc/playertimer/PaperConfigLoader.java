package boti.doc.playertimer;

import boti.doc.timer.TimerColor;
import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class PaperConfigLoader {

    private final Path configFile;
    private final Logger logger;

    public PaperConfigLoader(Path dataDir, Logger logger) {
        this.configFile = dataDir.resolve("config.toml");
        this.logger = logger;
    }

    public PlayerTimerConfig load() {
        PlayerTimerConfig config = new PlayerTimerConfig();

        try {
            createDefaultConfigIfMissing();

            Toml toml = new Toml().read(configFile.toFile());
            String color = toml.getString("defaultColor", "WHITE");

            config.setDefaultColor(TimerColor.valueOf(color.toUpperCase()));
        } catch (Exception e) {
            logger.warning("Failed to load config; using defaults: " + e.getMessage());
        }

        return config;
    }

    private void createDefaultConfigIfMissing() throws IOException {
        if (Files.exists(configFile)) return;

        Files.createDirectories(configFile.getParent());

        Files.writeString(
                configFile,
                """
                # PlayerTimer configuration

                # Default colour for new timers
                defaultColor = "WHITE"
                """
        );
    }
}