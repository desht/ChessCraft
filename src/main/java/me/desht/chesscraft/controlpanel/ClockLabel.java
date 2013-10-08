package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.util.ChessUtils;
import chesspresso.Chess;

public class ClockLabel extends AbstractSignLabel {

	private static final int[] xPos = new int[2];
	static {
		xPos[Chess.WHITE] = 2;
		xPos[Chess.BLACK] = 5;
	}

	private final int colour;
	private TimeControl timeControl;

	public ClockLabel(ControlPanel panel, int colour) {
		super(panel, ChessUtils.getColour(colour), xPos[colour], 1);

		this.colour = colour;
		timeControl = null;
	}

	public TimeControl getTimeControl() {
		return timeControl;
	}

	public void setTimeControl(TimeControl timeControl) {
		this.timeControl = timeControl;
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = new String[] { "", "", "", "" };

		res[0] = colour == Chess.WHITE ? Messages.getString("Game.white") : Messages.getString("Game.black");

		if (timeControl == null) {
			res[2] = getIndicatorColour() + ChessUtils.milliSecondsToHMS(0);
		} else {
			res[2] = getIndicatorColour() + timeControl.getClockString();
			switch (timeControl.getControlType()) {
			case NONE:
				res[3] = Messages.getString("ControlPanel.timeElapsed");
				break;
			default:
				res[3] = Messages.getString("ControlPanel.timeRemaining");
				break;
			}
		}

		return res;
	}

}
