package boti.doc.playertimer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// This plugin provides a timer on a per player basis. The timer can be counting up
// or down and the time the countdown runs for is user adjustable but will default
// to 5 minutes
public final class PlayerTimerPlugin extends JavaPlugin {

    private PlayerTimerService timerService;

    @Override
    public void onEnable() {
        getLogger().info("PlayerTimer enabled");
        TimerStore store = new TimerStore(getDataFolder().toPath(), getLogger());
        timerService = new PlayerTimerService(store);
        new PlayerTimerCommand(timerService).register(getLifecycleManager());
        Bukkit.getScheduler().runTaskTimer(this, timerService::tickAllPlayers, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(timerService, this);
        // timers will get auto-saved every 30 seconds
        Bukkit.getScheduler().runTaskTimer(this, timerService::saveAll, 20L * 30, 20L * 30);
    }

    @Override
    public void onDisable() {
        if (timerService != null) {
            timerService.saveAll();
        }
        getLogger().info("PlayerTimer disabled");
    }

}
