package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class UndoButton extends AbstractSignButton {

	public UndoButton(ControlPanel panel) {
		super(panel, "undoBtn", "undo", 7, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();
		if (game != null) {
			game.offerUndoMove(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		return gameInState(GameState.RUNNING);
	}

}
