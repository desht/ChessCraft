package me.desht.chesscraft.expector;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.enums.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public abstract class ExpectData {

	ChessCraft plugin;
	ExpectAction action;

	public ExpectData(ChessCraft plugin) {
		this.plugin = plugin;
	}

	public void doResponse(Player p) throws ChessException {
	}

	public void setAction(ExpectAction action) {
		this.action = action;
	}

	public ExpectAction getAction() {
		return action;
	}
}
