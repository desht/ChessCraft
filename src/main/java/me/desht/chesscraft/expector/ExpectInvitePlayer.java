package me.desht.chesscraft.expector;

import org.bukkit.Bukkit;

import me.desht.chesscraft.ChessCraft;
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
	public void doResponse(final String playerName) {
		// run this as sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		Bukkit.getScheduler().scheduleSyncDelayedTask(ChessCraft.getInstance(), new Runnable() {
			@Override
			public void run() {
				ChessGame game = ChessGame.getCurrentGame(playerName, true);
				game.invitePlayer(playerName, inviteeName);	
			}
		});
	}
}
