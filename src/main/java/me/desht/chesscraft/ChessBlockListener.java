package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ChessBlockListener extends BlockListener {

	ChessCraft plugin;

	public ChessBlockListener(ChessCraft plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!plugin.getConfig().getBoolean("no_building", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!plugin.getConfig().getBoolean("no_burning", true)) {
			return;
		}

		if (BoardView.partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled())
			return;
		
		Block b = event.getBlock();
		BoardView bv = BoardView.partOfChessBoard(b.getLocation());
		if (bv == null) {
			return;
		}
		
		if (b.getState() instanceof Sign) {
			event.setCancelled(true);
		}
	}
}