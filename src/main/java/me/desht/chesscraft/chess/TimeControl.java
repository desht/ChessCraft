package me.desht.chesscraft.chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import com.google.common.base.Joiner;

public class TimeControl implements ConfigurationSerializable {
	public enum ControlType { NONE, ROLLOVER, MOVE_IN, GAME_IN };

	private String spec;
	private ControlType controlType;
	private long totalTime;			// milliseconds
	private long remainingTime;		// milliseconds
	private long elapsed;				// milliseconds
	private int rolloverPhase;
	private int rolloverMovesMade;
	private List<RolloverPhase> rollovers = new ArrayList<TimeControl.RolloverPhase>();
	private long lastChecked;

	public TimeControl(String spec) {
		this.spec = spec;
		if (spec == null || spec.isEmpty()) {
			controlType = ControlType.NONE;
		} else if (spec.startsWith("G/")) {
			// game in - minutes
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 60000;
		} else if (spec.startsWith("M/")) {
			// move in - seconds
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 1000;
		} else if (!spec.isEmpty() && Character.isDigit(spec.charAt(0))) {
			for (String s0 : spec.split(";")) {
				rollovers.add(new RolloverPhase(s0));
			}
			rolloverPhase = rolloverMovesMade = 0;
			remainingTime = rollovers.get(0).getMinutes() * 60000;
		} else {
			throw new IllegalArgumentException("Invalid time control specification: " + spec);
		}
		lastChecked = System.currentTimeMillis();
	}

	public TimeControl(long elapsed) {
		controlType = ControlType.NONE;
		this.elapsed = elapsed;
		this.spec = "";
		this.remainingTime = this.totalTime = 0L;
		this.rolloverMovesMade = this.rolloverPhase = 0;
		this.lastChecked = System.currentTimeMillis();
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("spec", spec);
		res.put("elapsed", elapsed);
		res.put("remainingTime", remainingTime);
		res.put("rolloverPhase", rolloverPhase);
		res.put("rolloverMovesMade", rolloverMovesMade);
		return res;
	}

	public static TimeControl deserialize(Map<String, Object> map) {
		TimeControl tc = new TimeControl((String) map.get("spec"));
		tc.elapsed = Long.parseLong(map.get("elapsed").toString());
		tc.remainingTime = Long.parseLong(map.get("remainingTime").toString());
		tc.rolloverMovesMade = (Integer) map.get("rolloverMovesMade");
		tc.rolloverPhase = (Integer) map.get("rolloverPhase");
		return tc;
	}

	public ControlType getControlType() {
		return controlType;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public long getElapsed() {
		return elapsed;
	}

	public long getRemainingTime() {
		return controlType == ControlType.NONE ? Long.MAX_VALUE : remainingTime;
	}

	public String getClockString() {
		switch (getControlType()) {
		// TODO: fill in other control type cases
		case NONE:
			return ChessGame.milliSecondsToHMS(getElapsed());
		default:
			return "???";
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO: i18n needed here
		switch (controlType) {
		case MOVE_IN:
			return "Move in " + (totalTime / 1000) + " seconds";
		case GAME_IN:
			return "Game in " + (totalTime / 60000) + " minutes";
		case ROLLOVER:
			List<String> l = new ArrayList<String>();
			for (RolloverPhase rp : rollovers) {
				String s = rp.getMoves() + " moves in " + rp.getMinutes() + " minutes";
				if (rp.getIncrement() > 0) {
					s = s + " (" + (rp.getIncrement() / 1000) + " seconds)";
				}
			}
			return Joiner.on(", then ").join(l);
		case NONE:
			return "Elapsed : " + (elapsed / 1000) + " seconds";
		default:
			return "???";	
		}
	}

	/**
	 * Process a clock tick.
	 */
	public void tick() {
		long offset = System.currentTimeMillis() - lastChecked;
		lastChecked = System.currentTimeMillis();
		elapsed += offset;
		if (controlType != ControlType.NONE) {
			remainingTime -= offset;	
		}
	}

	/**
	 * The player has made a move - adjust time control accordingly.
	 */
	public void moveMade() {
		switch (controlType) {
		case MOVE_IN:
			remainingTime = totalTime;
			break;
		case ROLLOVER:
			rolloverMovesMade++;
			long carryOver = 0;
			if (rolloverMovesMade == rollovers.get(rolloverPhase).getMoves()) {
				rolloverMovesMade = 0;
				rolloverPhase = (rolloverPhase + 1) % rollovers.size();
				carryOver = remainingTime;
			}
			remainingTime = rollovers.get(rolloverPhase).getMinutes() * 60000;
			remainingTime += carryOver;
			remainingTime += rollovers.get(rolloverPhase).getIncrement();
		}
	}

	private class RolloverPhase {
		private long increment;
		private int moves;
		private int minutes;

		RolloverPhase(String spec) {
			String[] fields = spec.split("/");
			switch (fields.length) {
			case 3:
				this.increment = Long.parseLong(fields[2]);
				// fall through
			case 2:
				this.moves = Integer.parseInt(fields[0]);
				this.minutes = Integer.parseInt(fields[1]);
				break;
			default:
				throw new IllegalArgumentException("invalid rollover specification: " + spec);
			}
		}

		public long getIncrement() {
			return increment;
		}

		public int getMoves() {
			return moves;
		}

		public int getMinutes() {
			return minutes;
		}
	}
}
