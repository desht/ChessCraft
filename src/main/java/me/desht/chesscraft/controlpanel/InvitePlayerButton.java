package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.expector.ExpectInvitePlayer;
import me.desht.dhutils.MiscUtil;
import org.bukkit.event.player.PlayerInteractEvent;

public class InvitePlayerButton extends AbstractSignButton {

	public InvitePlayerButton(ControlPanel panel) {
		super(panel, "invitePlayerBtn", "invite", 2, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();

		if (game != null && (game.getWhitePlayerName().isEmpty() || game.getBlackPlayerName().isEmpty())) {
			ChessCraft.getInstance().responseHandler.expect(event.getPlayer().getName(), new ExpectInvitePlayer());
			MiscUtil.statusMessage(event.getPlayer(), Messages.getString("ControlPanel.chessInvitePrompt")); //$NON-NLS-1$
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
