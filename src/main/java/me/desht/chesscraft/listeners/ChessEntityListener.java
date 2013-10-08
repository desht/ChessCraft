package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public class ChessEntityListener extends ChessListenerBase {

	public ChessEntityListener(ChessCraft plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (!plugin.getConfig().getBoolean("no_creatures") || plugin.isChessNPC(event.getEntity())) { //$NON-NLS-1$
			return;
		}

		Location loc = event.getLocation();
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.isPartOfBoard(loc)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!plugin.getConfig().getBoolean("no_creatures")) {
			return;
		}
		if (!(event.getTarget() instanceof Player) && !plugin.isChessNPC(event.getTarget())) {
			return;
		}

		if (BoardViewManager.getManager().partOfChessBoard(event.getEntity().getLocation(), 0) != null
				|| BoardViewManager.getManager().partOfChessBoard(event.getTarget().getLocation(), 0) != null) {
			event.setCancelled(true);
			// don't remove tame creatures
			if (!(event.getEntity() instanceof Tameable && ((Tameable) event.getEntity()).isTamed())) {
				event.getEntity().remove();
			}
		}
	}

	/**
	 * Stop zombie/skeleton pieces from burning in the daytime
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		if (plugin.isChessNPC(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	/**
	 * Stop e.g. sheep eating grass
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Stop e.g. snowmen leaving snow trails
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityFormBlockEvent(EntityBlockFormEvent event) {
		if (BoardViewManager.getManager().partOfChessBoard(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Stop chess piece entities dropping items when they die.
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (plugin.isChessNPC(event.getEntity())) {
			event.getDrops().clear();
		}
	}

	/**
	 * Stop endermen pieces (or any other mob that can telport) from teleporting away
	 *
	 * @param event
	 */
	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent event) {
		if (plugin.isChessNPC(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!plugin.getConfig().getBoolean("no_explosions")) { //$NON-NLS-1$
			return;
		}

		for (Block b : event.blockList()) {
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				if (bv.isPartOfBoard(b.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
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
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
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
		if (!plugin.getConfig().getBoolean("no_misc_damage")) { //$NON-NLS-1$
			return;
		}

		if (event.getCause() == DamageCause.SUFFOCATION) {
			BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getEntity().getLocation(), 0);
			if (bv != null) {
				// player must have had a chess piece placed on them
				displacePlayerSafely(event);
				event.setCancelled(true);
			}
		} else {
			// any other damage to a player while on a board, e.g. falling off of a piece or viewing platform,
			// cactus/lava/fire on pieces, etc..
			BoardView bv = BoardViewManager.getManager().partOfChessBoard(event.getEntity().getLocation(), 1);
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
		} while (loc.getBlock().getType() != Material.AIR && loc.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR && n < MAX_DIST);
		if (n >= MAX_DIST) {
			MiscUtil.errorMessage(p, Messages.getString("ChessEntityListener.goingToSpawn")); //$NON-NLS-1$
			p.teleport(p.getWorld().getSpawnLocation());
		} else {
			p.teleport(loc);
		}
	}

	private boolean isAllowedMonsterAttack(Entity damager) {
		return !(damager instanceof Player) && damager instanceof LivingEntity
				&& !plugin.getConfig().getBoolean("no_monster_attacks"); //$NON-NLS-1$
	}

	private boolean isAllowedPlayerAttack(Entity damager) {
		return damager instanceof Player && !plugin.getConfig().getBoolean("no_pvp"); //$NON-NLS-1$
	}
}
