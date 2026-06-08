package boti.doc.playertimer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import org.bukkit.plugin.Plugin;

// Implement the command using Brigadier
// playertimer
// ├── start
// │   ├── countup
// │       └── color
// │   └── countdown
// │       └── seconds
// │            └── color
// ├── pause
// ├── resume
// ├── stop
// ├── reset
// ├── hide
// └── show

public class PlayerTimerCommand {

    private final PlayerTimerService timerService;

    public PlayerTimerCommand(PlayerTimerService timerService) {
        this.timerService = timerService;
    }

    public void register(LifecycleEventManager<Plugin> lifecycleManager) {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register(
                    Commands.literal("playertimer")
                            .then(buildStartCountup())
                            .then(buildStartCountdown())
                            .then(Commands.literal("pause")
                                    .executes(ctx -> timerService.pauseTimer(ctx.getSource())))
                            .then(Commands.literal("resume")
                                    .executes(ctx -> timerService.resumeTimer(ctx.getSource())))
                            .then(Commands.literal("stop")
                                    .executes(ctx -> timerService.stopTimer(ctx.getSource())))
                            .then(Commands.literal("reset")
                                    .executes(ctx -> timerService.resetTimer(ctx.getSource())))
                            .then(Commands.literal("hide")
                                    .executes(ctx -> timerService.hideTimer(ctx.getSource())))
                            .then(Commands.literal("show")
                                    .executes(ctx -> timerService.showTimer(ctx.getSource())))
                            .then(buildAdmin())
                            .build()
            )
        );
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildStartCountup() {
        return Commands.literal("startcountup")
                .executes(ctx ->
                        timerService.startCountup(ctx.getSource(), null))
                .then(Commands.argument("color", StringArgumentType.word())
                        .executes(ctx -> {
                            String color = StringArgumentType.getString(ctx, "color");
                            return timerService.startCountup(ctx.getSource(), color);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildStartCountdown() {
        return Commands.literal("startcountdown")
                .executes(ctx ->
                        timerService.startCountdown(ctx.getSource(), 300, null))
                .then(Commands.argument("duration", StringArgumentType.word())
                        .executes(ctx -> {
                            String duration = StringArgumentType.getString(ctx, "duration");
                            return timerService.executeStartCountdown(ctx, duration, null);
                        })
                        .then(Commands.argument("color", StringArgumentType.word())
                                .executes(ctx -> {
                                    String duration = StringArgumentType.getString(ctx, "duration");
                                    String color = StringArgumentType.getString(ctx, "color");
                                    return timerService.executeStartCountdown(ctx, duration, color);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildAdmin() {
        return Commands.literal("admin")
                .requires(src -> src.getSender().hasPermission("playertimer.admin"))
                .then(Commands.literal("clearall")
                        .executes(ctx -> timerService.clearAllTimers(ctx.getSource())))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    return timerService.clearPlayerTimer(ctx.getSource(), playerName);
                                })
                        )
                );
    }
}