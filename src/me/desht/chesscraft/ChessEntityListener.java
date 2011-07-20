package me.desht.chesscraft;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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
		for (BoardView bv : BoardView.listBoardViews()) {
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
			for (BoardView bv : BoardView.listBoardViews()) {
				if (bv.isPartOfBoard(b.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@Override
	public void onEntityDamage(EntityDamageEvent event)	{
		if (event.isCancelled())
			return;
		if (!(event.getEntity() instanceof Player))
			return;
		
		if (event instanceof EntityDamageByEntityEvent) {
			if (!plugin.getConfiguration().getBoolean("no_pvp", false))
				return;
			EntityDamageByEntityEvent dbeEvent = (EntityDamageByEntityEvent) event;
			if (dbeEvent.getDamager() instanceof Player) {
				Location attackerLoc = dbeEvent.getDamager().getLocation();
				Location defenderLoc = event.getEntity().getLocation();
				for (BoardView bv : BoardView.listBoardViews()) {
					if (bv.isPartOfBoard(defenderLoc) || bv.isPartOfBoard(attackerLoc)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		} else if (event.getCause() == DamageCause.SUFFOCATION) {
			BoardView bv = BoardView.partOfChessBoard(event.getEntity().getLocation());
			if (bv != null) {
				final int MAX_DIST = 100;
				// player must have had a chess piece placed on them
				Player p = (Player) event.getEntity();
				Location loc = p.getLocation().clone();
				int n = 0;
				do { 
					loc.add(0, 0, -1); // east
				} while (loc.getBlock().getTypeId() != 0 && loc.getBlock().getRelative(BlockFace.UP).getTypeId() != 0 && n < MAX_DIST);
				if (n >= MAX_DIST) {
					plugin.errorMessage(p, "Can't find a safe place to displace you - going to spawn");
					p.teleport(p.getWorld().getSpawnLocation());
				}
				p.teleport(loc);
				event.setCancelled(true);
				System.out.println("moved to loc " + loc);
			}
		}
		

	}

}
