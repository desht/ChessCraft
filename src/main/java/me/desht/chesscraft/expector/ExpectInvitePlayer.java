package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.dhutils.responsehandler.ExpectBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

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
	public void doResponse(final UUID playerId) {
		// Run this as a sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		// So ugly :(
		deferTask(playerId, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(playerId);
				ChessGame game = ChessGameManager.getManager().getCurrentGame(player, true);
				game.invitePlayer(player, inviteeName);
			}
		});
	}
}
