package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class BlackYesButton extends YesNoButton {

	public BlackYesButton(ControlPanel panel) {
		super(panel, 6, 0, Chess.BLACK, true);
	}

}
