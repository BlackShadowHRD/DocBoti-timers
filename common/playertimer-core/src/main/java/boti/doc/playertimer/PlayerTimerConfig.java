package boti.doc.playertimer;

import boti.doc.timer.TimerColor;

public class PlayerTimerConfig {

    private TimerColor defaultColor = TimerColor.GREEN;

    public TimerColor getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(TimerColor defaultColor) {
        this.defaultColor = defaultColor;
    }
}