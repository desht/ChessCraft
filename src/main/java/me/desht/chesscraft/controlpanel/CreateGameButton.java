package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;

import org.bukkit.event.player.PlayerInteractEvent;

import chesspresso.Chess;

public class CreateGameButton extends AbstractSignButton {

	public CreateGameButton(ControlPanel panel) {
		super(panel, "createGameBtn", "create.game", 1, 2);
	}
	
	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame.createGame(event.getPlayer(), null, getView(), Chess.WHITE);
	}

	@Override
	public boolean isEnabled() {
		return getGame() == null && !getView().isDesigning(); 
	}
}
