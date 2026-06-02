package boti.doc.playertimer;

//import boti.doc.playertimer.PlayerTimer;
import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;
import boti.doc.timer.TimeParser;
import boti.doc.timer.TimerColor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.UUID;

public class PlayerTimerService {

    private final Map<UUID, PlayerTimer> timers;
    private final TimerStore store;

    public PlayerTimerService(TimerStore store) {
        this.store = store;
        this.timers = store.load();
    }

    // --- Player commands ---

    public int startCountup(CommandSourceStack source, String colorName) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        UUID id = ctx.player().getUUID();
        PlayerTimer timer = timers.get(id);

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            ctx.reply("Timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTUP, 0);
        newTimer.setColor(parseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);
        saveAll();
        ctx.reply("Timer started.");

        return Command.SINGLE_SUCCESS;
    }

    public int startCountdown(CommandSourceStack source, int seconds, String colorName) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        UUID id = ctx.player().getUUID();
        PlayerTimer timer = timers.get(id);

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            ctx.reply("Timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTDOWN, seconds);
        newTimer.setColor(parseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);
        saveAll();
        ctx.reply("Timer started.");

        return Command.SINGLE_SUCCESS;
    }

    public int pauseTimer(CommandSourceStack source) {
        TimerCommandContext ctx = requirePlayer(source);
        if (ctx == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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

        PlayerTimer timer = timers.get(ctx.player().getUUID());
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
        source.sendSystemMessage(Component.literal("All player timers have been cleared."));
        return Command.SINGLE_SUCCESS;
    }

    public int clearPlayerTimer(CommandSourceStack source, String playerName) {
        // Search online players by name
        ServerPlayer target = null;
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(playerName)) {
                target = p;
                break;
            }
        }

        if (target == null) {
            source.sendSystemMessage(Component.literal("Player not found or not online."));
            return Command.SINGLE_SUCCESS;
        }

        timers.remove(target.getUUID());
        saveAll();
        source.sendSystemMessage(Component.literal("Timer cleared for " + target.getName().getString() + "."));
        return Command.SINGLE_SUCCESS;
    }

    // --- Tick loop ---

    public void tickAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerTimer timer = timers.get(player.getUUID());
            if (timer == null) continue;

            boolean justFinished = timer.tick();
            if (justFinished) {
                saveAll();
                notifyFinished(player);
            }
            if (timer.isVisible()) renderTimer(player, timer);
        }
    }

    private void notifyFinished(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("Your time is up."));
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BELL_BLOCK,
                SoundSource.BLOCKS,
                1.0f,
                0.7f
        );
    }

    private void renderTimer(ServerPlayer player, PlayerTimer timer) {
        player.sendSystemMessage(
                Component.literal("Time: " + timer.toDisplayString())
                        .withStyle(toFabricColor(timer.getColor())),
                true  // true = action bar
        );
    }

    private ChatFormatting toFabricColor(TimerColor color) {
        return switch (color) {
            case WHITE -> ChatFormatting.WHITE;
            case RED -> ChatFormatting.RED;
            case BLUE -> ChatFormatting.BLUE;
            case GREEN -> ChatFormatting.GREEN;
            case YELLOW -> ChatFormatting.YELLOW;
            case GOLD -> ChatFormatting.GOLD;
            case AQUA -> ChatFormatting.AQUA;
            case DARK_AQUA -> ChatFormatting.DARK_AQUA;
            case DARK_BLUE -> ChatFormatting.DARK_BLUE;
            case DARK_GREEN -> ChatFormatting.DARK_GREEN;
            case DARK_RED -> ChatFormatting.DARK_RED;
            case LIGHT_PURPLE -> ChatFormatting.LIGHT_PURPLE;
            case DARK_PURPLE -> ChatFormatting.DARK_PURPLE;
            case GRAY -> ChatFormatting.GRAY;
            case DARK_GRAY -> ChatFormatting.DARK_GRAY;
            case BLACK -> ChatFormatting.BLACK;
        };
    }

    // --- Persistence ---

    public void saveAll() {
        store.save(timers);
    }

    public void onPlayerQuit(ServerPlayer player) {
        saveAll();
    }

    // --- Helpers ---

    private record TimerCommandContext(ServerPlayer player, CommandSourceStack source) {
        void reply(String message) {
            if (source.getEntity() instanceof ServerPlayer) {
                // Player ran it themselves — speak directly to them
                source.sendSystemMessage(Component.literal(message));
            } else {
                // External trigger — include player name for context
                source.sendSystemMessage(Component.literal("Player " + player.getName().getString() + ": " + message));
            }
        }
    }

    private TimerCommandContext requirePlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return new TimerCommandContext(player, source);
        }
        source.sendFailure(Component.literal("Only players can use this command."));
        return null;
    }

    private TimerColor parseColor(String colorName) {
        try {
            return TimerColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return TimerColor.WHITE;
        }
    }
}
