package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import org.bukkit.event.player.PlayerInteractEvent;

public class StartButton extends AbstractSignButton {

	public StartButton(ControlPanel panel) {
		super(panel, "startGameBtn", "start", 4, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();

		if (game != null) {
			game.start(event.getPlayer().getUniqueId().toString());
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null && getGame().getState() == GameState.SETTING_UP;
	}

}
