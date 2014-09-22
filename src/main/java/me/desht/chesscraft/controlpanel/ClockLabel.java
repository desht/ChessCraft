package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.util.ChessUtils;

public class ClockLabel extends AbstractSignLabel {

	private static final int[] xPos = new int[2];

    private String timeStr = ChessUtils.milliSecondsToHMS(0);

	static {
		xPos[Chess.WHITE] = 2;
		xPos[Chess.BLACK] = 5;
	}

	private final int colour;
//	private TimeControl timeControl;

	public ClockLabel(ControlPanel panel, int colour) {
		super(panel, ChessUtils.getColour(colour), xPos[colour], 1);

		this.colour = colour;
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = new String[] { "", "", "", "" };

		res[0] = colour == Chess.WHITE ? Messages.getString("Game.white") : Messages.getString("Game.black");
        res[2] = getIndicatorColour() + timeStr;

        if (getGame() == null) {
            res[3] = "";
        } else {
            TimeControl timeControl = getGame().getClock().getTimeControl();
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

    public void setLabel(String timeStr) {
        this.timeStr = timeStr == null ? ChessUtils.milliSecondsToHMS(0) : timeStr;
    }
}
