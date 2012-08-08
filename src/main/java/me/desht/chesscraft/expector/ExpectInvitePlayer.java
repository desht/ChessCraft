package me.desht.chesscraft.expector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
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
		// Run this as a sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		// So ugly :(
		Bukkit.getScheduler().scheduleSyncDelayedTask(ChessCraft.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					ChessGame game = ChessGame.getCurrentGame(playerName, true);
					game.invitePlayer(playerName, inviteeName);
				} catch (ChessException e) {
					Player player = Bukkit.getPlayerExact(playerName);
					if (player != null) {
						MiscUtil.errorMessage(player, e.getMessage());
					}
				}
			}
		});
	}
}
