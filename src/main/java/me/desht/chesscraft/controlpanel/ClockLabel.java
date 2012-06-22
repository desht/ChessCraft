package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.util.ChessUtils;
import chesspresso.Chess;

public class ClockLabel extends AbstractSignLabel {

	private static int[] xPos = new int[2];
	static {
		xPos[Chess.WHITE] = 2;
		xPos[Chess.BLACK] = 5;
	};
	
	private TimeControl timeControl;
	
	public ClockLabel(ControlPanel panel, int colour) {
		super(panel, ChessUtils.getColour(colour), xPos[colour], 1);
		
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
		String[] res = getSignText();
		
		if (timeControl == null) {
			res[2] = AbstractSignLabel.INDICATOR_COLOUR + ChessUtils.milliSecondsToHMS(0);
		} else {
			res[2] = AbstractSignLabel.INDICATOR_COLOUR + timeControl.getClockString();
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
