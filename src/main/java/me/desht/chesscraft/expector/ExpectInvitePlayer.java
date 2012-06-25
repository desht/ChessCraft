package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.responsehandler.ExpectBase;

public class ExpectInvitePlayer extends ExpectBase {
	private String inviteeName;
	
	public ExpectInvitePlayer() {
	}
	
	public String getInviteeName() {
		return inviteeName;
	}

	public void setInviteeName(String playerName) {
		this.inviteeName = playerName;
	}

	@Override
	public void doResponse(String playerName) {
		ChessGame game = ChessGame.getCurrentGame(playerName, true);
		game.invitePlayer(playerName, inviteeName);
	}
}
