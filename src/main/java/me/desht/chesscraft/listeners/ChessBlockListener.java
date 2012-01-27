package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.chess.BoardView;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ChessBlockListener implements Listener {

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_burning", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled())
			return;
		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}
	
	/**
	 * Cancelling liquid flow events makes it possible to use water & lava for walls & chess pieces.
	 * 
	 * @param event
	 */
	@EventHandler
	public void onBlockFromTo(BlockFromToEvent event) {		
		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		} else if (BoardView.partOfChessBoard(event.getToBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

}