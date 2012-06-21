package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class WhiteNoButton extends YesNoButton {

	public WhiteNoButton(ControlPanel panel) {
		super(panel, 1, 0, Chess.WHITE, false);
	}

}
