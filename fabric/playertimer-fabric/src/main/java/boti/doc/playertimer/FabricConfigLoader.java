package boti.doc.playertimer;

import boti.doc.timer.TimerColor;
import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FabricConfigLoader {

    private final Path configFile;

    public FabricConfigLoader(Path configDir) {
        this.configFile = configDir.resolve("playertimer.toml");
    }

    public PlayerTimerConfig load() {
        PlayerTimerConfig config = new PlayerTimerConfig();

        try {
            createDefaultConfigIfMissing();

            Toml toml = new Toml().read(configFile.toFile());

            String color = toml.getString("defaultColor", "WHITE");

            config.setDefaultColor(
                    TimerColor.valueOf(color.toUpperCase())
            );

        } catch (Exception e) {
            System.err.println(
                    "[PlayerTimer] Failed to load config: "
                            + e.getMessage()
            );
        }

        return config;
    }

    private void createDefaultConfigIfMissing()
            throws IOException {

        if (Files.exists(configFile)) {
            return;
        }

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