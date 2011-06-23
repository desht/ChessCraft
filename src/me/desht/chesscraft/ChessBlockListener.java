package me.desht.chesscraft;

import org.bukkit.Location;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

public class ChessBlockListener extends BlockListener {
	ChessCraft plugin;
	
	public ChessBlockListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.isCancelled()) return;
		
		Location loc = event.getBlock().getLocation();
		
		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;
		Location loc = event.getBlock().getLocation();

		System.out.println("block place " + loc);
		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				System.out.println("cancelled");
				event.setCancelled(true);
				return;
			}
		}
	}
}
