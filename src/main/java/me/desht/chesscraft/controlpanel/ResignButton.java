package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.enums.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class ResignButton extends AbstractSignButton {

	public ResignButton(ControlPanel panel) {
		super(panel, "resignBtn", "resign", 6, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();

		if (game != null) {
            String playerId = event.getPlayer().getUniqueId().toString();
            game.ensurePlayerInGame(playerId);
            ChessPlayer cp = game.getPlayer(playerId);
			game.resign(cp.getColour());
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null && getGame().getState() == GameState.RUNNING;
	}

}
