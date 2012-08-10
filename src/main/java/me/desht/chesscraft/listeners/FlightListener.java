package me.desht.chesscraft.listeners;

import java.util.HashSet;
import java.util.Set;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.Direction;
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

public class FlightListener extends ChessListenerBase {

	// notes if the player was already allowed to fly, by some other means
	private final Set<String> alreadyAllowedToFly = new HashSet<String>();
	// notes if the player is currently allowed to fly due to being on/near a board
	private final Set<String> allowedToFly = new HashSet<String>();
	private boolean enabled;

	public FlightListener(ChessCraft plugin) {
		super(plugin);
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
		if (!enabled)
			return;

		Location from = event.getFrom();
		Location to = event.getTo();

		// we only care if the player has actually moved to a different block
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		setFlightAllowed(player, shouldBeAllowedToFly(to));
		//		System.out.println("flight allowed = " + player.getAllowFlight() + " in flyers = " + allowedToFly.contains(player.getName()));
	}

	/**
	 * Prevent the player from interacting with any block outside a board while enjoying temporary flight.  It's
	 * called early (priority LOWEST) to cancel the event ASAP.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void flyingInteraction(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (allowedToFly.contains(player.getName()) && !alreadyAllowedToFly.contains(player.getName())) {
			if (player.isFlying() && BoardView.partOfChessBoard(player.getLocation()) == null) {
				if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					MiscUtil.errorMessage(player, Messages.getString("Flight.interactionStopped"));
					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * Check if the player may fly (in a ChessCraft context) given their current position.
	 * 
	 * @param player
	 * @return
	 */
	public boolean shouldBeAllowedToFly(Location loc) {
		int above = plugin.getConfig().getInt("flying.above_board");
		int outside = plugin.getConfig().getInt("flying.outside_board");

		for (BoardView bv: BoardView.listBoardViews()) {
			Cuboid c = bv.getOuterBounds();
			c = c.expand(Direction.Up, (c.getSizeY() * above) / 100)
					.outset(Direction.Horizontal, (c.getSizeX() * outside) / 100);
			if (c.contains(loc))
				return true;
		}
		return false;
	}

	/**
	 * Mark the player as being allowed to fly or not.  If the player was previously allowed to fly by
	 * some other means, he can continue to fly even if flying is being disabled in a ChessCraft context.
	 * 
	 * @param player
	 * @param flying
	 */
	public void setFlightAllowed(Player player, boolean flying) {
		String playerName = player.getName();

		if (flying && allowedToFly.contains(playerName) || !flying && !allowedToFly.contains(playerName))
			return;

		System.out.println("setflightallowed: " + flying);
		// note if the player is already allowed to fly (by some other means)
		if (player.getAllowFlight() && !allowedToFly.contains(playerName)) {
			alreadyAllowedToFly.add(playerName);
		} else {
			alreadyAllowedToFly.remove(playerName);
		}

		System.out.println("player.setAllowFlight: " + (flying || alreadyAllowedToFly.contains(playerName)));
		player.setAllowFlight(flying || alreadyAllowedToFly.contains(playerName));

		if (flying) {
			allowedToFly.add(playerName);
			MiscUtil.alertMessage(player, Messages.getString("Flight.flightEnabled"));
		} else {
			allowedToFly.remove(playerName);
			MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabled"));
		}

		if (!player.getAllowFlight()) {
			// prevent fall damage so players don't fall to their death by flying too far
			// from a chessboard
			Location loc = player.getLocation();
			player.setFallDistance(player.getWorld().getHighestBlockYAt(loc) - loc.getBlockY());
			System.out.println("player falling: falldist now = " + player.getFallDistance());
		}
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
			for (String playerName : allowedToFly) {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null) {
					player.setAllowFlight(alreadyAllowedToFly.contains(playerName));
					MiscUtil.alertMessage(player, Messages.getString("Flight.flightDisabledByAdmin"));
				}
			}
			allowedToFly.clear();
		}

		this.enabled = enabled;

	}

}
