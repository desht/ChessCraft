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
			game.inviteOpen(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();

		if (game == null) return false;

		boolean hasWhite = !game.getWhitePlayerName().isEmpty();
		boolean hasBlack = !game.getBlackPlayerName().isEmpty();

		return game.getState() == GameState.SETTING_UP && (!hasWhite || !hasBlack);
	}

}
