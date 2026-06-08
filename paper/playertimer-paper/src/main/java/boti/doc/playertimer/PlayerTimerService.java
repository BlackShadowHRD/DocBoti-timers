package boti.doc.playertimer;

import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;
import boti.doc.timer.TimerColor;
import boti.doc.timer.TimeParser;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerTimerService implements Listener {

    private final Map<UUID, PlayerTimer> timers;
    private final TimerStore store;
    private final PlayerTimerConfig config;

    public PlayerTimerService(
            TimerStore store,
            PlayerTimerConfig config) {

        this.store = store;
        this.config = config;

        this.timers = store.load();
    }

    // --- Player commands ---

    public int startCountup(CommandSourceStack source, String colorName) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        UUID id = ctx.player().getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            ctx.reply("Timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTUP, 0);
        newTimer.setColor(chooseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);
        saveAll();
        ctx.reply("Timer started.");

        return Command.SINGLE_SUCCESS;
    }

    public int startCountdown(CommandSourceStack source, int seconds, String colorName) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        UUID id = ctx.player().getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            ctx.reply("Timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTDOWN, seconds);
        newTimer.setColor(chooseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);
        saveAll();
        ctx.reply("Timer started.");

        return Command.SINGLE_SUCCESS;
    }

    public int pauseTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        switch (timer.pause()) {
            case SUCCESS -> {
                saveAll();
                ctx.reply("Timer paused.");
            }
            case NOT_RUNNING -> ctx.reply("That timer is not running so cannot be paused.");
            case NOT_PAUSED, NOT_ACTIVE -> ctx.reply("Unable to pause timer.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int resumeTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        switch (timer.resume()) {
            case SUCCESS -> {
                saveAll();
                ctx.reply("Timer resumed.");
            }
            case NOT_PAUSED -> ctx.reply("That timer is not paused so cannot be resumed.");
            case NOT_RUNNING, NOT_ACTIVE -> ctx.reply("Unable to resume timer.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int stopTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        switch (timer.stop()) {
            case SUCCESS -> {
                saveAll();
                ctx.reply("Timer stopped.");
            }
            case NOT_ACTIVE -> ctx.reply("That timer is neither running nor paused so cannot be stopped.");
            case NOT_RUNNING, NOT_PAUSED -> ctx.reply("Unable to stop timer.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int resetTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        switch (timer.reset()) {
            case SUCCESS -> {
                saveAll();
                ctx.reply("Timer reset.");
            }
            case NOT_RUNNING, NOT_PAUSED, NOT_ACTIVE -> ctx.reply("Unable to reset timer.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int hideTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        timer.setVisible(false);
        saveAll();
        ctx.reply("Timer hidden.");

        return Command.SINGLE_SUCCESS;
    }

    public int showTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUniqueId());
        if (timer == null) {
            ctx.reply("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        timer.setVisible(true);
        saveAll();
        ctx.reply("Timer visible.");

        return Command.SINGLE_SUCCESS;
    }

    public int executeStartCountdown(
            CommandContext<CommandSourceStack> ctx,
            String duration, String colorName
    ) {
        TimerCommandContext timerCtx = requirePlayer(ctx.getSource());
        if (timerCtx == null) return Command.SINGLE_SUCCESS;

        try {
            int seconds = TimeParser.parseToSeconds(duration);
            return startCountdown(ctx.getSource(), seconds, colorName);
        } catch (IllegalArgumentException ignored) {
                timerCtx.reply("Invalid duration. Use seconds, mm:ss, hh:mm:ss, or formats like 1h0m10s.");
            return Command.SINGLE_SUCCESS;
        }
    }

    // --- Admin commands ---

    public int clearAllTimers(CommandSourceStack source) {
        timers.clear();
        saveAll();
        source.getSender().sendMessage("All player timers have been cleared.");
        return Command.SINGLE_SUCCESS;
    }

    public int clearPlayerTimer(CommandSourceStack source, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            source.getSender().sendMessage("Player not found or not online.");
            return Command.SINGLE_SUCCESS;
        }
        timers.remove(target.getUniqueId());
        saveAll();
        source.getSender().sendMessage("Timer cleared for " + target.getName() + ".");
        return Command.SINGLE_SUCCESS;
    }

    // --- Tick loop ---

    public void tickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerTimer timer = timers.get(player.getUniqueId());
            if (timer == null) continue;

            boolean justFinished = timer.tick();
            if (justFinished) {
                saveAll();
                notifyFinished(player);
            }
            if (timer.isVisible()) renderTimer(player, timer);
        }
    }

    private void notifyFinished(Player player) {
        // Use the player parameter directly
        player.sendMessage(net.kyori.adventure.text.Component.text("Your time is up."));

        player.playSound(
                player.getLocation(),
                "item.goat_horn.sound.0",
                org.bukkit.SoundCategory.MASTER,
                1.0f,
                1.0f
        );
    }

    private void renderTimer(Player player, PlayerTimer timer) {
        player.sendActionBar(
                Component.text("Time: " + timer.toDisplayString(), toPaperColor(timer.getColor()))
        );
    }

    // --- Persistence ---

    public void saveAll() {
        store.save(timers);
    }

    // --- Events ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveAll();
    }

    // --- Helpers ---

    private record TimerCommandContext(Player player, CommandSourceStack source) {
        void reply(String message) {
            if (source.getExecutor() instanceof Player) {
                // Player ran it themselves — speak directly to them
                source.getSender().sendMessage(message);
            } else {
                // External trigger — include player name for context
                source.getSender().sendMessage("Player " + player.getName() + ": " + message);
            }
        }
    }

    private TimerCommandContext requirePlayer(CommandSourceStack source) {
        if (source.getExecutor() instanceof Player player) {
            return new TimerCommandContext(player, source);
        }

        source.getSender().sendMessage("Only players can use this command.");
        return null;
    }

    private TimerColor chooseColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return config.getDefaultColor();
        }

        return parseColor(colorName);
    }

    private TimerColor parseColor(String colorName) {
        try {
            return TimerColor.valueOf(colorName.toUpperCase());
        } catch (Exception e) {
            return TimerColor.WHITE;
        }
    }

    private NamedTextColor toPaperColor(TimerColor color) {
        return switch (color) {
            case WHITE -> NamedTextColor.WHITE;
            case RED -> NamedTextColor.RED;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case YELLOW -> NamedTextColor.YELLOW;
            case GOLD -> NamedTextColor.GOLD;
            case AQUA -> NamedTextColor.AQUA;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLACK -> NamedTextColor.BLACK;
        };
    }
}