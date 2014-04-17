package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import org.bukkit.event.player.PlayerInteractEvent;

public class InviteAnyoneButton extends AbstractSignButton {

	public InviteAnyoneButton(ControlPanel panel) {
		super(panel, "inviteAnyoneBtn", "invite.anyone", 3, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();

		if (game != null) {
			game.inviteOpen(event.getPlayer());
		}
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();
		return game != null && game.getState() == GameState.SETTING_UP && !game.isFull();
	}
}
