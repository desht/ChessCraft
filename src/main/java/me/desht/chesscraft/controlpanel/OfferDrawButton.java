package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import org.bukkit.event.player.PlayerInteractEvent;

public class OfferDrawButton extends AbstractSignButton {

	public OfferDrawButton(ControlPanel panel) {
		super(panel, "offerDrawBtn", "offer.draw", 5, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();
		if (game != null) {
			game.offerDraw(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();
		return game != null && game.getState() == GameState.RUNNING;
	}

}
