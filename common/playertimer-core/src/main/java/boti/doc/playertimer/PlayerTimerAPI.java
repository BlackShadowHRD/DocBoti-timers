import java.util.UUID;

import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;

public interface PlayerTimerAPI {
    boolean startCountup(UUID playerId);
    boolean startCountdown(UUID playerId, int seconds);

    boolean pause(UUID playerId);
    boolean resume(UUID playerId);
    boolean stop(UUID playerId);
    boolean reset(UUID playerId);

    boolean show(UUID playerId);
    boolean hide(UUID playerId);

    boolean hasTimer(UUID playerId);
    int getTime(UUID playerId);
    TimerState getState(UUID playerId);
    TimerMode getMode(UUID playerId);
}