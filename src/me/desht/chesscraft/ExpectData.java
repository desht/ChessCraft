package me.desht.chesscraft;

import me.desht.chesscraft.ExpectResponse.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

abstract class ExpectData {
	ChessCraft plugin;
	ExpectAction action;

	ExpectData(ChessCraft plugin) {
		this.plugin = plugin;
	}

	void doResponse(Player p) throws ChessException {

	}

	void setAction(ExpectAction action) {
		this.action = action;
	}

	ExpectAction getAction() {
		return action;
	}
}
