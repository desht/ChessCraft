package me.desht.chesscraft.controlpanel;

import chesspresso.Chess;

public class PromoteWhiteButton extends PromoteButton {

	public PromoteWhiteButton(ControlPanel panel) {
		super(panel, "whitePawnPromotionBtn", null, 1, 1, Chess.WHITE);
	}

}
