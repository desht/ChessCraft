package me.desht.chesscraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author des
 *
 * Keeps track of players' last teleport position, and when they last logged out.
 *
 */
public class PlayerTracker {
	private final Map<UUID, Location> lastPos = new HashMap<UUID, Location>();
	private final Map<UUID, Long> loggedOutAt = new HashMap<UUID, Long>();

	public void teleportPlayer(Player player, Location loc) {
		setLastPos(player, player.getLocation());
		ChessCraft.getInstance().getFX().playEffect(player.getLocation(), "teleport_out");
		player.teleport(loc);
		ChessCraft.getInstance().getFX().playEffect(player.getLocation(), "teleport_in");
	}

	public Location getLastPos(Player player) {
		return lastPos.get(player.getUniqueId());
	}

	private void setLastPos(Player player, Location loc) {
		lastPos.put(player.getUniqueId(), loc);
	}

	public void playerLeft(Player player) {
		loggedOutAt.put(player.getUniqueId(), System.currentTimeMillis());
	}

	public void playerRejoined(Player player) {
		loggedOutAt.remove(player.getUniqueId());
	}

	public long getPlayerLeftAt(UUID who) {
		return loggedOutAt.containsKey(who) ? loggedOutAt.get(who) : 0;
	}
}
