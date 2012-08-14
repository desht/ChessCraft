package me.desht.chesscraft.events;

import me.desht.chesscraft.chess.BoardView;

import org.bukkit.event.HandlerList;

public class ChessBoardDeletedEvent extends ChessBoardEvent {

	private static final HandlerList handlers = new HandlerList();

	public ChessBoardDeletedEvent(BoardView boardView) {
		super(boardView);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
