package me.desht.chesscraft;

import org.bukkit.event.block.BlockListener;

public class ChessBlockListener extends BlockListener {
	ChessCraft plugin;
	
	public ChessBlockListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
}
