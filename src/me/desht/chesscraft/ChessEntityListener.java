package me.desht.chesscraft;

import org.bukkit.event.entity.EntityListener;

public class ChessEntityListener extends EntityListener {
	ChessCraft plugin;
	
	public ChessEntityListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
}
