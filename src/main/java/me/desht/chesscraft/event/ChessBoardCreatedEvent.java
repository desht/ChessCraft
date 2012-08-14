package me.desht.chesscraft.event;

import me.desht.chesscraft.chess.BoardView;

import org.bukkit.event.HandlerList;

public class ChessBoardCreatedEvent extends ChessBoardEvent {
	private static final HandlerList handlers = new HandlerList();

	public ChessBoardCreatedEvent(BoardView boardView) {
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
