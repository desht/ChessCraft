package me.desht.chesscraft.controlpanel;

import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.util.ChessUtils;

public abstract class PromoteButton extends AbstractSignButton {

	private final int colour;
	
	public PromoteButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y, int colour) {
		super(panel, labelKey, permissionNode, x, y);
		this.colour = colour;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		int playerColour = getGame().getPlayerColour(event.getPlayer().getName());
		if (playerColour != colour)
			return;
		
		ChessPlayer p = getGame().getPlayer(playerColour);
		if (p != null) {
			p.cyclePromotionPiece();
		}
		repaint();
	}

	@Override
	public String[] getCustomSignText() {
		String[] label = getSignText();
		label[3] = getIndicatorColour() + getPromoStr();
		return label;
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();
		return game != null && !game.getPlayerName(colour).isEmpty();
	}
	
	private String getPromoStr() {
		if (getGame() == null) {
			return "";
		}
		return ChessUtils.pieceToStr(getGame().getPromotionPiece(colour));
	}
}
