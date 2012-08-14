package me.desht.chesscraft.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class ChessEvent extends Event {

	@Override
	public abstract HandlerList getHandlers();

}
