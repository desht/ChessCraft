package me.desht.chesscraft;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

abstract class ExpectData {
	ChessCraft plugin;
	
	ExpectData(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	void doResponse(Player p) throws ChessException {
		
	}
}
