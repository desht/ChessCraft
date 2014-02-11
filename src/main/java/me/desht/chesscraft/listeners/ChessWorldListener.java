package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.dhutils.Debugger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkUnloadEvent;
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

	@EventHandler
	public void chunkUnload(ChunkUnloadEvent event) {
		BoardViewManager mgr = BoardViewManager.getManager();
		BoardView bv;
		if ((bv = mgr.getBoardViewForChunk(event.getChunk())) != null) {
			if (bv.getGame() != null && bv.getChessBoard().getChessSet().hasMovablePieces()) {
				Debugger.getInstance().debug("chunk unload cancelled: " + event.getWorld().getName() + " " + event.getChunk() + " - board " + bv.getName());
				event.setCancelled(true);
			}
		}
	}

}
