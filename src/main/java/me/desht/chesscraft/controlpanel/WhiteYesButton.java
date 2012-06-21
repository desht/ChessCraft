package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class WhiteYesButton extends YesNoButton {

	public WhiteYesButton(ControlPanel panel) {
		super(panel, 0, 0, Chess.WHITE, true);
	}

}
