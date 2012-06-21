package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;

import org.bukkit.event.player.PlayerInteractEvent;

public class GameInfoButton extends AbstractSignButton {

	public GameInfoButton(ControlPanel panel) {
		super(panel, "gameInfoBtn", "list.game", 7, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();
		if (game != null) {
			game.showGameDetail(event.getPlayer());
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

}
