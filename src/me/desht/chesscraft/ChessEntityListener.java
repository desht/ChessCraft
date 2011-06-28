package me.desht.chesscraft;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;

public class ChessEntityListener extends EntityListener {
	ChessCraft plugin;
	
	public ChessEntityListener(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.isCancelled())
			return;
		if (!plugin.getConfiguration().getBoolean("no_creatures", false))
			return;
		
		Location loc = event.getLocation();
		for (BoardView bv : plugin.listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled())
			return;
		if (!plugin.getConfiguration().getBoolean("no_explosions", false))
			return;
		
		for (Block b : event.blockList()) {
			for (BoardView bv : plugin.listBoardViews()) {
				if (bv.isPartOfBoard(b.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
}
