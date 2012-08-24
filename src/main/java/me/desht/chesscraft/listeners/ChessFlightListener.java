package me.desht.chesscraft.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class ChessFlightListener extends ChessListenerBase {

	// notes if the player was already allowed to fly, by some other means
	private final Set<String> alreadyAllowedToFly = new HashSet<String>();
	// notes if the player is currently allowed to fly due to being on/near a board
	// maps the player name to the previous flight speed for the player
	private final Map<String,PreviousSpeed> allowedToFly = new HashMap<String,PreviousSpeed>();
	// cache of the regions in which board flight is allowed
	private final List<Cuboid> flightRegions = new ArrayList<Cuboid>();
	// notes when a player was last messaged about flight, to reduce spam
	private final Map<String,Long> lastMessagedIn = new HashMap<String,Long>();
	private final Map<String,Long> lastMessagedOut = new HashMap<String,Long>();
	
	private boolean enabled;
	private boolean captive;

	public ChessFlightListener(ChessCraft plugin) {
		super(plugin);
		enabled = plugin.getConfig().getBoolean("flying.enabled");
		captive = plugin.getConfig().getBoolean("flying.captive");
	}

	/**
	 * Globally enable or disable chessboard flight for all players.
	 */
	public void setEnabled(boolean enabled) {
		if (enabled == this.enabled)
			return;

		if (enabled) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				setFlightAllowed(player, shouldBeAllowedToFly(player.getLocation()));
			}
		} else {
			for (String playerName : allowedToFly.keySet()) {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null) {
					if (!alreadyAllowedToFly.contains(playerName)) {
						player.setAllowFlight(false);
					} else {
						// restore previous flight/walk speed
						allowedToFly.get(playerName).restoreSpeeds();
					}
					player.setAllowFlight(alreadyAllowedToFly.contains(playerName));
					MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabledByAdmin"));
				}
			}
			allowedToFly.clear();
		}

		this.enabled = enabled;

	}

	/**
	 * Set the "captive" mode.  Captive prevents flying players from flying too far from a
	 * board.  Non-captive just disables flight if players try to fly too far.
	 * 
	 * @param captive
	 */
	public void setCaptive(boolean captive) {
		this.captive = captive;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoined(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.getAllowFlight()) {
			// player is already allowed to fly, somehow - make a note of that
			alreadyAllowedToFly.add(player.getName());
		}
		setFlightAllowed(player, shouldBeAllowedToFly(player.getLocation()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLeft(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName();
		allowedToFly.remove(playerName);
		alreadyAllowedToFly.remove(playerName);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
//		long now = System.nanoTime();
		if (!enabled)
			return;

		Location from = event.getFrom();
		Location to = event.getTo();

		// we only care if the player has actually moved to a different block
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		boolean flyingNow = allowedToFly.containsKey(player.getName()) && player.isFlying();
		boolean shouldBeAllowed = shouldBeAllowedToFly(to); // || alreadyAllowedToFly.contains(player.getName());

		if (captive) {
			// captive mode - if flying, prevent movement too far from a board
			if (flyingNow && !shouldBeAllowed && !alreadyAllowedToFly.contains(player.getName())) {
				event.setCancelled(true);
			} else {
				setFlightAllowed(player, shouldBeAllowed);
			}
		} else {
			// otherwise, free movement, but flight cancelled if player moves too far
			setFlightAllowed(player, shouldBeAllowed);
		}
//		System.out.println("move handler: " + (System.nanoTime() - now) + " ns");
		//		System.out.println("flight allowed = " + player.getAllowFlight() + " in flyers = " + allowedToFly.contains(player.getName()));
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		boolean shouldBeAllowed = shouldBeAllowedToFly(event.getTo());
		setFlightAllowed(event.getPlayer(), shouldBeAllowed);
	}

	/**
	 * Prevent the player from interacting with any block outside a board while enjoying temporary flight.  It's
	 * called early (priority LOWEST) to cancel the event ASAP.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFlyingInteraction(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (allowedToFly.containsKey(player.getName()) && !alreadyAllowedToFly.contains(player.getName()) && player.isFlying()) {
			if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (BoardView.partOfChessBoard(event.getClickedBlock().getLocation()) == null) {
					MiscUtil.errorMessage(player, Messages.getString("Flight.interactionStopped"));
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onBoardCreated(ChessBoardCreatedEvent event) {
		recalculateFlightRegions();
	}
	
	@EventHandler
	public void onBoardDeleted(ChessBoardDeletedEvent event) {
		recalculateFlightRegions();
	}

	/**
	 * Cache the regions in which flight is allowed.  We do this to avoid calculation in the
	 * code which is (frequently) called from the PlayerMoveEvent handler.
	 */
	public void recalculateFlightRegions() {
		
		int above = plugin.getConfig().getInt("flying.upper_limit");
		int outside = plugin.getConfig().getInt("flying.outer_limit");
		
		flightRegions.clear();
		
		for (BoardView bv : BoardView.listBoardViews()) {
			Cuboid c = bv.getOuterBounds();
			c = c.expand(Direction.Up, Math.max(5, (c.getSizeY() * above) / 100));
			c = c.outset(Direction.Horizontal, Math.max(5, (c.getSizeX() * outside) / 100));
			flightRegions.add(c);
		}
	}
	
	/**
	 * Check if the player may fly (in a ChessCraft context) given their current position.
	 * 
	 * @param player
	 * @return
	 */
	public boolean shouldBeAllowedToFly(Location loc) {
		for (Cuboid c : flightRegions) {
			if (c.contains(loc))
				return true;
		}
		return false;
	}
	
	/**
	 * Mark the player as being allowed to fly or not.  If the player was previously allowed to fly by
	 * some other means, he can continue to fly even if chess board flying is being disabled.
	 * 
	 * @param player
	 * @param flying
	 */
	public void setFlightAllowed(Player player, boolean flying) {
		String playerName = player.getName();

		boolean currentlyAllowed = allowedToFly.containsKey(playerName);
		
		if (flying && currentlyAllowed || !flying && !currentlyAllowed)
			return;

		// note if the player is already allowed to fly (by some other means)
		if (player.getAllowFlight() && !currentlyAllowed) {
			alreadyAllowedToFly.add(playerName);
		} else {
			alreadyAllowedToFly.remove(playerName);
		}

		player.setAllowFlight(flying || alreadyAllowedToFly.contains(playerName));

		long now = System.currentTimeMillis();
		
		if (flying) {
			allowedToFly.put(playerName, new PreviousSpeed(player));
			player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
			player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			if (plugin.getConfig().getBoolean("flying.auto")) {
				player.setFlying(true);
			}
			long last = lastMessagedIn.containsKey(playerName) ? lastMessagedIn.get(playerName) : 0;
			if (now - last > 5000) {
				MiscUtil.alertMessage(player, Messages.getString("Flight.flightEnabled"));
				lastMessagedIn.put(playerName, System.currentTimeMillis());
			}
		} else {
			allowedToFly.get(playerName).restoreSpeeds();
			allowedToFly.remove(playerName);
			long last = lastMessagedOut.containsKey(playerName) ? lastMessagedOut.get(playerName) : 0;
			if (now - last > 5000) {
				MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabled"));
				lastMessagedOut.put(playerName, System.currentTimeMillis());
			}
		}

		if (!player.getAllowFlight()) {
			// prevent fall damage so players don't fall to their death by flying too far from a chessboard
			Location loc = player.getLocation();
			int dist = player.getWorld().getHighestBlockYAt(loc) - loc.getBlockY();
			if (dist < -1) {
				player.setFallDistance(dist);
			}
		}
	}

	public void updateSpeeds() {
		for (String playerName : allowedToFly.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
				player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			}
		}
	}

	private class PreviousSpeed {
		private final String playerName;
		private final float flySpeed;
		private final float walkSpeed;
		
		public PreviousSpeed(Player p) {
			playerName = p.getName();
			flySpeed = p.getFlySpeed();
			walkSpeed = p.getWalkSpeed();
//			System.out.println("previous speed: walk=" + walkSpeed + " fly=" + flySpeed);
		}
		
		public void restoreSpeeds() {
			Player p = Bukkit.getPlayerExact(playerName);
			if (p == null)
				return;
			p.setFlySpeed(flySpeed);
			p.setWalkSpeed(walkSpeed);
		}
	}
}
