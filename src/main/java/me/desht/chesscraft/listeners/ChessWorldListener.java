package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardViewManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ChessWorldListener extends ChessListenerBase {

	public ChessWorldListener(ChessCraft plugin) {
		super(plugin);
	}

	@EventHandler
	public void onWorldLoaded(WorldLoadEvent event) {
		BoardViewManager.getManager().loadDeferred(event.getWorld().getName());
	}

	@EventHandler
	public void onWorldUnloaded(WorldUnloadEvent event) {
		BoardViewManager.getManager().unloadBoardsForWorld(event.getWorld().getName());
	}
}
