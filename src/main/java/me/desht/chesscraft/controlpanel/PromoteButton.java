package me.desht.chesscraft.controlpanel;

import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.util.ChessUtils;

public abstract class PromoteButton extends AbstractSignButton {

	private final int colour;
	
	public PromoteButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y, int colour) {
		super(panel, labelKey, permissionNode, x, y);
		this.colour = colour;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		getGame().cyclePromotionPiece(event.getPlayer().getName());
		
		repaint();
	}

	@Override
	public String[] getCustomSignText() {
		String[] label = getSignText();
		label[3] = getIndicatorColour() + getPromoStr(colour);
		return label;
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();
		return game != null && !game.getPlayerName(colour).isEmpty();
	}
	
	private String getPromoStr(int colour) {
		if (getGame() == null) {
			return ""; //$NON-NLS-1$
		}
		return ChessUtils.pieceToStr(getGame().getPromotionPiece(colour));
	}
}
