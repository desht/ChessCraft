package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.entity.Player;

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
	public void doResponse(Player player) {
		ChessGame game = ChessGame.getCurrentGame(player.getName(), true);
		game.invitePlayer(player.getName(), inviteeName);
	}
}
