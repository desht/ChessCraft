package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;

import org.bukkit.event.player.PlayerInteractEvent;

public class CreateGameButton extends AbstractSignButton {

	public CreateGameButton(ControlPanel panel) {
		super(panel, "createGameBtn", "create.game", 1, 2);
	}
	
	@Override
	public void execute(PlayerInteractEvent event) {
		// TODO Auto-generated method stub
		ChessGame.createGame(event.getPlayer(), null, getPanel().getView());
	}

	@Override
	public boolean isEnabled() {
		BoardView view = getView();
		return view.getGame() == null && !view.isDesigning(); 
	}

}
