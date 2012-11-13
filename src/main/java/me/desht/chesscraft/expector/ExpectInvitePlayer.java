package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;

import org.bukkit.Bukkit;

public class ExpectInvitePlayer extends ExpectChessBase {
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
	public void doResponse(final String playerName) {
		// Run this as a sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		// So ugly :(
		deferTask(Bukkit.getPlayerExact(playerName), new Runnable() {
			@Override
			public void run() {
				ChessGame game = ChessGameManager.getManager().getCurrentGame(playerName, true);
				game.invitePlayer(playerName, inviteeName);
			}
		});
	}
}
