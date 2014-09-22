package me.desht.chesscraft.chess;

import chesspresso.Chess;
import me.desht.chesscraft.util.ChessUtils;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class TwoPlayerClock implements ConfigurationSerializable {
    private TimeControl timeControl;
    private final long[] elapsed = new long[2];
    private final long[] remaining = new long[2];
    private int activePlayer;
    private long lastTick;

    public TwoPlayerClock(String tcSpec) {
        this.activePlayer = Chess.NOBODY;
        elapsed[0] = elapsed[1] = 0L;
        setTimeControl(tcSpec);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tc", timeControl.getSpec());
        map.put("elapsed0", elapsed[0]);
        map.put("elapsed1", elapsed[1]);
        map.put("remaining0", remaining[0]);
        map.put("remaining1", remaining[1]);
        return map;
    }

    public static TwoPlayerClock deserialize(Map<String,Object> map) {
        TwoPlayerClock clock = new TwoPlayerClock((String) map.get("tc"));
        clock.elapsed[0] = getLong(map.get("elapsed0"));
        clock.elapsed[1] = getLong(map.get("elapsed1"));
        clock.remaining[0] = getLong(map.get("remaining0"));
        clock.remaining[1] = getLong(map.get("remaining1"));

        return clock;
    }

    private static long getLong(Object o) {
        if (o instanceof Long) {
            return (Long) o;
        } else if (o instanceof Integer) {
            return Long.valueOf((Integer) o);
        } else {
            throw new IllegalArgumentException("invalid quantity: " + o);
        }
    }

    public TimeControl getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(String tcSpec) {
        this.timeControl = new TimeControl(tcSpec);
        remaining[0] = remaining[1] = timeControl.getRemainingTime();
    }

    public void setActivePlayer(int activePlayer) {
        if (isRunning() && timeControl.getControlType() == TimeControl.ControlType.MOVE_IN) {
            remaining[activePlayer] = timeControl.getRemainingTime();
        }
        this.activePlayer = activePlayer;
        lastTick = System.currentTimeMillis();
    }

    public void stop() {
        activePlayer = Chess.NOBODY;
    }

    public boolean isRunning() {
        return activePlayer != Chess.NOBODY;
    }

    public void tick() {
        if (isRunning()) {
            long now = System.currentTimeMillis();
            long delta = now - lastTick;
            lastTick = now;

            elapsed[activePlayer] += delta;

            if (timeControl.getControlType() != TimeControl.ControlType.NONE) {
                remaining[activePlayer] = Math.max(0L, remaining[activePlayer] - delta);
            }
        }
    }

    public int getActivePlayer() {
        return activePlayer;
    }

    public long getElapsedTime(int colour) {
        return elapsed[colour];
    }

    public long getRemainingTime(int colour) {
        return remaining[colour];
    }

    public String getClockString(int colour) {
        switch (timeControl.getControlType()) {
            case NONE:
                return ChessUtils.milliSecondsToHMS(elapsed[colour]);
            default:
                return ChessUtils.milliSecondsToHMS(remaining[colour]);
        }
    }
}
