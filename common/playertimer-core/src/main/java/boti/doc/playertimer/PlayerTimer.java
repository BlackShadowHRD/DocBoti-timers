package boti.doc.playertimer;

import boti.doc.timer.TimerMode;
import boti.doc.timer.TimerState;
import boti.doc.timer.TimerOperationResult;

import boti.doc.timer.TimerColor;

public class PlayerTimer {

    private final TimerMode mode;
    private TimerState state;
    private boolean visible;
    private int time;
    private TimerColor color;

    public PlayerTimer(TimerMode mode, int startTime) {
        this.mode = mode;
        this.time = startTime;
        this.state = TimerState.READY;
        this.visible = true;
        this.color = TimerColor.WHITE;
    }

    // --- Domain operations ---

    public void start() {
        if (state == TimerState.RUNNING) {
            throw new IllegalStateException("Timer is already running");
        }
        state = TimerState.RUNNING;
    }

    // timer can only be paused if it is running
    public TimerOperationResult pause() {
        if (state != TimerState.RUNNING) {
            return TimerOperationResult.NOT_RUNNING;
        }
        state = TimerState.PAUSED;
        return TimerOperationResult.SUCCESS;
    }

    // timer can only be resumed if it is paused
    public TimerOperationResult resume() {
        if (state != TimerState.PAUSED) {
            return TimerOperationResult.NOT_PAUSED;
        }
        state = TimerState.RUNNING;
        return TimerOperationResult.SUCCESS;
    }

    // timer can only be stopped if running or paused
    public TimerOperationResult stop() {
        if (state != TimerState.RUNNING && state != TimerState.PAUSED) {
            return TimerOperationResult.NOT_ACTIVE;
        }
        state = TimerState.STOPPED;
        time = 0;
        return TimerOperationResult.SUCCESS;
    }

    // there are no preconditions for resetting the timer so it will always succeed
    public TimerOperationResult reset() {
        state = TimerState.READY;
        time = 0;
        return TimerOperationResult.SUCCESS;
    }

    /**
     * Advances the timer by one second up or down depending on mode.
     * Enforces invariants 1–6. Returns true if the timer just finished (hit zero),
     * so callers can react (play sound, send message) without inspecting state themselves.
     */
    public boolean tick() {
        if (state != TimerState.RUNNING) {       // invariant 1
            return false;
        }
        
        if (mode == TimerMode.COUNTDOWN) {       // invariants 3 & 4
            time = Math.max(0, time - 1);        // invariant 5
            if (time == 0) {                     // invariant 6
                state = TimerState.FINISHED;
                return true;                     // signal: timer just expired
            }
        } else {
            time += 1;
        }

        return false;
    }

    public String toDisplayString() {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int seconds = time % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    // This is to bypass the normal state preconditions during loading
    void restore(TimerState state, boolean visible, TimerColor color) {
        this.state = state;
        this.visible = visible;
        this.color = color;
    }

    // --- Accessors (no more raw setters for time/state/mode) ---

    public TimerMode getMode()        { return mode; }
    public TimerState getState()      { return state; }
    public int getTime()              { return time; }
    public boolean isVisible()        { return visible; }
    public TimerColor getColor()  { return color; }

    public void setVisible(boolean visible) { this.visible = visible; }
    public void setColor(TimerColor color) { this.color = color; }
}