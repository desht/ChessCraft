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
		
		if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty())) {
			ChessCraft.getResponseHandler().expect(event.getPlayer().getName(), new ExpectInvitePlayer());
			MiscUtil.statusMessage(event.getPlayer(), Messages.getString("ControlPanel.chessInvitePrompt")); //$NON-NLS-1$
		}
	}

	@Override
	public boolean isEnabled() {
		ChessGame game = getGame();
		
		if (game == null) return false;
		
		boolean hasWhite = !game.getPlayerWhite().isEmpty();
		boolean hasBlack = !game.getPlayerBlack().isEmpty();
		
		return game.getState() == GameState.SETTING_UP && (!hasWhite || !hasBlack);
	}

}
