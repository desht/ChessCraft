package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class PromoteBlackButton extends PromoteButton {

	public PromoteBlackButton(ControlPanel panel) {
		super(panel, null, null, 6, 1, Chess.BLACK);
	}

}
