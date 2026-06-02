package boti.doc.playertimer;

import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;
import boti.doc.timer.TimerOperationResult;

import org.bukkit.entity.Player;

public interface PlayerTimerAPI {
    boolean startCountup(Player player);
    boolean startCountdown(Player player, int seconds);

    boolean pause(Player player);
    boolean resume(Player player);
    boolean stop(Player player);
    boolean reset(Player player);

    boolean show(Player player);
    boolean hide(Player player);

    boolean hasTimer(Player player);
    int getTime(Player player);
    TimerState getState(Player player);
    TimerMode getMode(Player player);
}