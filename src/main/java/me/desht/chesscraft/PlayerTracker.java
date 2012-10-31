package me.desht.chesscraft;

import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 * Keeps track of players' last teleport position, and when they last logged out.
 * 
 */
public class PlayerTracker {
	private final Map<String, Location> lastPos = new HashMap<String, Location>();
	private final Map<String, Long> loggedOutAt = new HashMap<String, Long>();
	
	public void teleportPlayer(Player player, Location loc) {
		setLastPos(player, player.getLocation());
		ChessUtils.playEffect(player.getLocation(), "teleport_out");
		player.teleport(loc);
		ChessUtils.playEffect(player.getLocation(), "teleport_in");
	}

	public Location getLastPos(Player player) {
		return lastPos.get(player.getName());
	}

	private void setLastPos(Player player, Location loc) {
		lastPos.put(player.getName(), loc);
	}

	public void playerLeft(String who) {
		loggedOutAt.put(who, System.currentTimeMillis());
	}

	public void playerRejoined(String who) {
		loggedOutAt.remove(who);
	}

	public long getPlayerLeftAt(String who) {
		return loggedOutAt.containsKey(who) ? loggedOutAt.get(who) : 0;
	}
}
