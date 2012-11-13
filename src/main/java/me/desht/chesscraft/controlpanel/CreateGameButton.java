package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import chesspresso.Chess;

public class CreateGameButton extends AbstractSignButton {

	private int colour = Chess.WHITE;
	
	public CreateGameButton(ControlPanel panel) {
		super(panel, "createGameBtn", "create.game", 1, 2);
	}
	
	@Override
	public void execute(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			ChessGame.createGame(event.getPlayer(), null, getView(), colour);
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			// cycle between "White" and "Black"
			colour = Chess.otherPlayer(colour);
			repaint();
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() == null && !getView().isDesigning(); 
	}
	
	@Override
	protected String[] getCustomSignText() {
		String[] res = getSignText();
		
		res[3] = getIndicatorColour() + ChessUtils.getColour(colour);

		return res;
	}
}
