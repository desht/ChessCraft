package me.desht.chesscraft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * This event is called when a player enters or leaves a chessboard flight region.
 */
public class ChessPlayerFlightToggledEvent extends ChessEvent {
	private static final HandlerList handlers = new HandlerList();

	private final Player player;
	private final boolean isFlightEnabled;

	public ChessPlayerFlightToggledEvent(Player player, boolean isFlightEnabled) {
		this.player = player;
		this.isFlightEnabled = isFlightEnabled;
	}

	/**
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return the isEntering
	 */
	public boolean isFlightEnabled() {
		return isFlightEnabled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
