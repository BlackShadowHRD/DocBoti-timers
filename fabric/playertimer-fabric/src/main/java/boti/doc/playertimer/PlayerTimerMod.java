package boti.doc.playertimer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerTimerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("playertimer");

    private static final int AUTO_SAVE_INTERVAL_TICKS = 20 * 30; // every 30 seconds
    private int ticksUntilTimerUpdate = 20;
    private int ticksUntilAutoSave = AUTO_SAVE_INTERVAL_TICKS;

    private PlayerTimerService timerService;

    @Override
    public void onInitialize() {
        TimerStore store = new TimerStore(
                FabricLoader.getInstance().getConfigDir().resolve("playertimer"),
                LOGGER
        );

        timerService = new PlayerTimerService(store);

        registerCommands();
        registerTimerTick();
        registerPlayerQuit();
        registerShutdownSave();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                new PlayerTimerCommand(timerService).register(dispatcher, registryAccess)
        );
    }

    private void registerTimerTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            // Timer tick — once per second
            ticksUntilTimerUpdate--;
            if (ticksUntilTimerUpdate <= 0) {
                ticksUntilTimerUpdate = 20;
                timerService.tickAllPlayers(server);
            }

            // Auto-save — every 30 seconds
            ticksUntilAutoSave--;
            if (ticksUntilAutoSave <= 0) {
                ticksUntilAutoSave = AUTO_SAVE_INTERVAL_TICKS;
                timerService.saveAll();
            }
        });
    }

    private void registerPlayerQuit() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                timerService.onPlayerQuit(handler.player)
        );
    }

    private void registerShutdownSave() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                timerService.saveAll()
        );
    }
}
