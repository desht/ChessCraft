package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class PromoteWhiteButton extends PromoteButton {

	public PromoteWhiteButton(ControlPanel panel) {
		super(panel, null, null, 1, 1, Chess.WHITE);
	}

}
