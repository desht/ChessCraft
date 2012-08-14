package me.desht.chesscraft.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class ChessEvent extends Event {

	@Override
	public abstract HandlerList getHandlers();

}
