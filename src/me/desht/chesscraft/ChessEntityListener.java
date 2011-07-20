package me.desht.chesscraft;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
		if (!plugin.getConfiguration().getBoolean("no_pvp", false))
			return;
		if (!(event.getEntity() instanceof Player))
			return;
		
		System.out.println("entity damage");
		
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent dbeEvent = (EntityDamageByEntityEvent) event;
			if (dbeEvent.getDamager() instanceof Player) {
				Location attackerLoc = dbeEvent.getDamager().getLocation();
				Location defenderLoc = event.getEntity().getLocation();
				for (BoardView bv : BoardView.listBoardViews()) {
					if (bv.isPartOfBoard(defenderLoc) || bv.isPartOfBoard(attackerLoc)) {
						System.out.println("PVP action prevented!");
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		

	}

}
