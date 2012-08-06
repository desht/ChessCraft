package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;

import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;

public class ChessWorldListener extends ChessListenerBase {
	
	public ChessWorldListener(ChessCraft plugin) {
		super(plugin);
	}
	
	@EventHandler
	public void onWorldLoaded(WorldLoadEvent event) {
		BoardView.loadDeferred(event.getWorld().getName());
	}
}
