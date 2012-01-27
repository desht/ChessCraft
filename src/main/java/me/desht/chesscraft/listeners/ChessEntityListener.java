package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;

public class ChessEntityListener implements Listener {

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_creatures")) { //$NON-NLS-1$
			return;
		}

		Location loc = event.getLocation();
		for (BoardView bv : BoardView.listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (event.isCancelled() || !(event.getTarget() instanceof Player)
				|| !ChessConfig.getConfig().getBoolean("no_creatures")) { //$NON-NLS-1$
			return;
		}

		if (BoardView.partOfChessBoard(event.getEntity().getLocation()) != null
				|| BoardView.partOfChessBoard(event.getTarget().getLocation()) != null) {
			event.setCancelled(true);
			// don't remove tame (pet) wolves
			if (!(event.getEntity() instanceof Wolf && ((Wolf) event.getEntity()).isTamed())) {
				event.getEntity().remove();
			}
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_explosions")) { //$NON-NLS-1$
			return;
		}

		for (Block b : event.blockList()) {
			for (BoardView bv : BoardView.listBoardViews()) {
				if (bv.isPartOfBoard(b.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent dbeEvent = (EntityDamageByEntityEvent) event;
			if (dbeEvent.getDamager() == null) {
				return;
			}
			if (isAllowedPlayerAttack(dbeEvent.getDamager()) || isAllowedMonsterAttack(dbeEvent.getDamager())) {
				return;
			}

			Location attackerLoc = dbeEvent.getDamager().getLocation();
			Location defenderLoc = event.getEntity().getLocation();
			for (BoardView bv : BoardView.listBoardViews()) {
				if (bv.isPartOfBoard(defenderLoc) || bv.isPartOfBoard(attackerLoc)) {
					event.setCancelled(true);
					if ((event.getEntity() instanceof Player) && // victim is a player
							!(dbeEvent.getDamager() instanceof Player) // and attacker is a monster
							&& dbeEvent.getDamager() instanceof LivingEntity) {
						dbeEvent.getDamager().remove();
					}
					return;
				}
			}

		}
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		if (!ChessConfig.getConfig().getBoolean("no_misc_damage")) { //$NON-NLS-1$
			return;
		}

		if (event.getCause() == DamageCause.SUFFOCATION) {
			BoardView bv = BoardView.partOfChessBoard(event.getEntity().getLocation());
			if (bv != null) {
				// player must have had a chess piece placed on them
				displacePlayerSafely(event);
				event.setCancelled(true);
			}
		} else {
			// any other damage to a player while on a board, e.g. falling off of a piece or viewing platform,
			// cactus/lava/fire on pieces, etc..
			BoardView bv = BoardView.partOfChessBoard(event.getEntity().getLocation(), 1);
			if (bv != null) {
				event.setCancelled(true);
				event.getEntity().setFireTicks(0);
			}
		}
	}

	/**
	 * Safely displace a player out of the way so they are not entombed by a chess piece
	 * 
	 * @param event	The suffocation event that triggered this
	 */
	private void displacePlayerSafely(EntityDamageEvent event) {
		final int MAX_DIST = 100;
		
		Player p = (Player) event.getEntity();
		Location loc = p.getLocation().clone();
		int n = 0;
		do {
			loc.add(0, 0, -1); // east
		} while (loc.getBlock().getTypeId() != 0 && loc.getBlock().getRelative(BlockFace.UP).getTypeId() != 0
				&& n < MAX_DIST);
		if (n >= MAX_DIST) {
			ChessUtils.errorMessage(p, Messages.getString("ChessEntityListener.goingToSpawn")); //$NON-NLS-1$
			p.teleport(p.getWorld().getSpawnLocation());
		} else {
			p.teleport(loc);
		}
	}

	private boolean isAllowedMonsterAttack(Entity damager) {
		return !(damager instanceof Player) && damager instanceof LivingEntity
				&& !ChessConfig.getConfig().getBoolean("no_monster_attacks"); //$NON-NLS-1$
	}

	private boolean isAllowedPlayerAttack(Entity damager) {
		return damager instanceof Player && !ChessConfig.getConfig().getBoolean("no_pvp"); //$NON-NLS-1$
	}
}
