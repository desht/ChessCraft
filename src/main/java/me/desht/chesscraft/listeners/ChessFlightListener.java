package me.desht.chesscraft.listeners;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.event.ChessBoardModifiedEvent;
import me.desht.chesscraft.event.ChessPlayerFlightToggledEvent;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.FlightController;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.cuboid.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class ChessFlightListener extends ChessListenerBase {

	private static final int MESSAGE_COOLDOWN = 5000;
	private static final int BOUNCE_COOLDOWN = 300;

	// notes if the player is currently allowed to fly due to being on/near a board
	// maps the player name to the previous flight speed for the player
	private final Map<String,PreviousSpeed> allowedToFly = new HashMap<String,PreviousSpeed>();

	// notes when a player was last messaged about flight, to reduce spam
	private final Map<String,Long> lastMessagedIn = new HashMap<String,Long>();
	private final Map<String,Long> lastMessagedOut = new HashMap<String,Long>();
	// notes when player was last bounced back while flying
	private final Map<String,Long> lastBounce = new HashMap<String, Long>();

	private boolean enabled;
	private boolean captive;
	private final BoardViewManager bvm;
	private final FlightController controller;

	public ChessFlightListener(ChessCraft plugin) {
		super(plugin);

		bvm = BoardViewManager.getManager();
		controller = new FlightController(plugin);
		controller.setControllerListener(new FlightController.OnControllerChanged() {
			@Override
			public void onChanged(Player player, Plugin controller) {
				System.out.println("ChessCraft: controller changed: now " + (controller == null ? "(none)" : controller.getName()));
			}
		});
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
				if (bvm.getFlightRegion(player.getLocation()) != null) {
					controller.changeFlight(player, true);
				}
//				setFlightAllowed(player, bvm.getFlightRegion(player.getLocation()) != null);
			}
		} else {
			for (String playerName : allowedToFly.keySet()) {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null) {
					controller.yieldControl(player, gameModeAllowsFlight(player));
//					player.setAllowFlight(gameModeAllowsFlight(player));
					// restore previous flight/walk speed
					allowedToFly.get(playerName).restoreSpeeds();
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
	 * @param captive the captive mode
	 */
	public void setCaptive(boolean captive) {
		this.captive = captive;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoined(PlayerJoinEvent event) {
		if (!enabled)
			return;

		Player player = event.getPlayer();
		setFlightAllowed(player, bvm.getFlightRegion(player.getLocation()) != null);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLeft(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName();
		if (allowedToFly.containsKey(playerName)) {
			allowedToFly.get(playerName).restoreSpeeds();
			allowedToFly.remove(playerName);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		final Player player = event.getPlayer();
		final boolean isFlying = player.isFlying();
		if (event.getNewGameMode() != GameMode.CREATIVE && allowedToFly.containsKey(player.getName())) {
			// If switching away from creative mode and on/near a board, allow flight to continue.
			// Seems a delayed task is needed here - calling setAllowFlight() directly from the event handler
			// leaves getAllowFlight() returning true, but the player is still not allowed to fly.  (CraftBukkit bug?)
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (controller.changeFlight(player, true)) {
						player.setFlying(isFlying);
					}
//					player.setAllowFlight(true);
//					player.setFlying(isFlying);
				}
			});
		}
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
		boolean boardFlightAllowed = bvm.getFlightRegion(to) != null;
		boolean otherFlightAllowed = gameModeAllowsFlight(player);

		//		Debugger.getInstance().debug("move: boardflight = " + boardFlightAllowed + " otherflight = " + otherFlightAllowed);
		if (captive) {
			// captive mode - if flying, prevent movement too far from a board by bouncing the
			// player towards the centre of the board they're trying to leave
			if (flyingNow && !boardFlightAllowed && !otherFlightAllowed) {
				Long last = lastBounce.get(player.getName());
				if (last == null) last = 0L;
				if (System.currentTimeMillis() - last > BOUNCE_COOLDOWN) {
					event.setCancelled(true);
					Cuboid c = bvm.getFlightRegion(from);
					Location origin = c == null ? from : c.getCenter().subtract(0, c.getSizeY(), 0);
					Vector vec = origin.toVector().subtract(to.toVector()).normalize();
					player.setVelocity(vec);
					lastBounce.put(player.getName(), System.currentTimeMillis());
				}
			} else {
				setFlightAllowed(player, boardFlightAllowed);
			}
		} else {
			// otherwise, free movement, but flight cancelled if player moves too far
			setFlightAllowed(player, boardFlightAllowed);
		}
		//		System.out.println("move handler: " + (System.nanoTime() - now) + " ns");
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!enabled)
			return;

		final Player player = event.getPlayer();
		final boolean boardFlightAllowed = bvm.getFlightRegion(event.getTo()) != null;
		final boolean crossWorld = event.getTo().getWorld() != event.getFrom().getWorld();

		Debugger.getInstance().debug("teleport: boardflight = " + boardFlightAllowed + ", crossworld = " + crossWorld);

		// Seems a delayed task is needed here - calling setAllowFlight() directly from the event handler
		// leaves getAllowFlight() returning true, but the player is still not allowed to fly.  (CraftBukkit bug?)
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				if (crossWorld) {
					// Player flight seems to be automatically disabled when the world changes, so in that case we
					// force a re-enablement.  Without this, the following call to setFlightAllowed() would be ignored.
					setFlightAllowed(player, false);
				}
				setFlightAllowed(player, boardFlightAllowed);
			}
		});
	}

	/**
	 * Prevent the player from interacting with any block outside a board while enjoying temporary flight.  It's
	 * called early (priority LOWEST) to cancel the event ASAP.
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFlyingInteraction(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (allowedToFly.containsKey(player.getName()) && !gameModeAllowsFlight(player) && player.isFlying()) {
			if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (bvm.partOfChessBoard(event.getClickedBlock().getLocation(), 0) == null
						&& !PermissionUtils.isAllowedTo(player, "chesscraft.flight.interact")) {
					MiscUtil.errorMessage(player, Messages.getString("Flight.interactionStopped"));
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onBoardCreated(ChessBoardCreatedEvent event) {
		bvm.recalculateFlightRegions();
	}

	@EventHandler
	public void onBoardDeleted(ChessBoardDeletedEvent event) {
		bvm.recalculateFlightRegions();
	}

	@EventHandler
	public void onBoardModifed(ChessBoardModifiedEvent event) {
		if (event.getChangedAttributes().contains("enclosure")) {
			bvm.recalculateFlightRegions();
		}
	}

	/**
	 * Mark the player as being allowed to fly or not.  If the player was previously allowed to fly by
	 * virtue of creative mode, he can continue to fly even if chess board flying is being disabled.
	 *
	 * @param player the player
	 * @param allowed true if flight allowed, false otherwise
	 */
	private void setFlightAllowed(final Player player, boolean allowed) {
		String playerName = player.getName();

		boolean currentlyAllowed = allowedToFly.containsKey(playerName);

		if (allowed == currentlyAllowed) {
			return;
		}

		Debugger.getInstance().debug("set chess board flight allowed " + player.getName() + " = " + allowed);

		if (allowed) {
			controller.changeFlight(player, true);
		} else {
			controller.yieldControl(player, gameModeAllowsFlight(player));
		}
//		player.setAllowFlight(flying || gameModeAllowsFlight(player));

		// DEBUG
		System.out.println("--- " + player.getMetadata("flight_controller_change").size() + " metadata entries");
		for (MetadataValue value : player.getMetadata("flight_controller_change")) {
			System.out.println("  metadata: " + value.asString() + " - from " + value.getOwningPlugin().getName());
		}

		long now = System.currentTimeMillis();

		if (allowed) {
			allowedToFly.put(playerName, new PreviousSpeed(player));
			player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
			player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			if (plugin.getConfig().getBoolean("flying.auto")) {
				final Material mat = player.getLocation().subtract(0, 2, 0).getBlock().getType();
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						if (mat.isSolid()) {
							// give player a kick upwards iff they're standing on something solid
							player.setVelocity(new Vector(0, 1.0, 0));
						}
						player.setFlying(true);
					}
				});
			}
			long last = lastMessagedIn.containsKey(playerName) ? lastMessagedIn.get(playerName) : 0;
			if (now - last > MESSAGE_COOLDOWN  && player.getGameMode() != GameMode.CREATIVE) {
				MiscUtil.alertMessage(player, Messages.getString("Flight.flightEnabled"));
				lastMessagedIn.put(playerName, System.currentTimeMillis());
			}
		} else {
			allowedToFly.get(playerName).restoreSpeeds();
			allowedToFly.remove(playerName);
			long last = lastMessagedOut.containsKey(playerName) ? lastMessagedOut.get(playerName) : 0;
			if (now - last > MESSAGE_COOLDOWN && player.getGameMode() != GameMode.CREATIVE) {
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

		Bukkit.getPluginManager().callEvent(new ChessPlayerFlightToggledEvent(player, allowed));
	}

	private boolean gameModeAllowsFlight(Player player) {
		return player.getGameMode() == GameMode.CREATIVE;
	}

	/**
	 * Update current fly/walk speeds for all players currently enjoying board flight mode.  Called when
	 * the fly/walk speeds are changed in config.
	 */
	public void updateSpeeds() {
		for (String playerName : allowedToFly.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.setFlySpeed((float) plugin.getConfig().getDouble("flying.fly_speed"));
				player.setWalkSpeed((float) plugin.getConfig().getDouble("flying.walk_speed"));
			}
		}
	}

	/**
	 * Restore previous fly/walk speeds for all players who have a modified speed.  Called when the
	 * plugin is disabled.
	 */
	public void restoreSpeeds() {
		for (String playerName : allowedToFly.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				allowedToFly.get(playerName).restoreSpeeds();
			}
		}
	}

	private class PreviousSpeed {
		private final WeakReference<Player> player;
		private final float flySpeed;
		private final float walkSpeed;

		public PreviousSpeed(Player p) {
			player = new WeakReference<Player>(p);
			flySpeed = p.getFlySpeed();
			walkSpeed = p.getWalkSpeed();
			Debugger.getInstance().debug("player " + p.getName() + ": store previous speed: walk=" + walkSpeed + " fly=" + flySpeed);
		}

		public void restoreSpeeds() {
			Player p = player.get();
			if (p == null)
				return;
			p.setFlySpeed(flySpeed);
			p.setWalkSpeed(walkSpeed);
			Debugger.getInstance().debug("player " + p.getName() + " restore previous speed: walk=" + walkSpeed + " fly=" + flySpeed);
		}
	}
}
