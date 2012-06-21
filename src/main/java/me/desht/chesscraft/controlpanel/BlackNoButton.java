package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class BlackNoButton extends YesNoButton {

	public BlackNoButton(ControlPanel panel) {
		super(panel, 7, 0, Chess.BLACK, false);
	}

}
