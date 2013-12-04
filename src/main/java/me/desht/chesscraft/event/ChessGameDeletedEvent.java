package me.desht.chesscraft.event;

import me.desht.chesscraft.chess.ChessGame;
import org.bukkit.event.HandlerList;

public class ChessGameDeletedEvent extends ChessGameEvent {

	private static final HandlerList handlers = new HandlerList();

	public ChessGameDeletedEvent(ChessGame game) {
		super(game);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
