package me.desht.chesscraft.event;

import me.desht.chesscraft.chess.BoardView;
import org.bukkit.event.HandlerList;

import java.util.HashSet;
import java.util.Set;

public class ChessBoardModifiedEvent extends ChessBoardEvent {
	private static final HandlerList handlers = new HandlerList();

	private final Set<String> changed;

	public ChessBoardModifiedEvent(BoardView boardView, Set<String> changedAttributes) {
		super(boardView);
		changed = new HashSet<String>(changedAttributes);
	}

	public Set<String> getChangedAttributes() {
		return changed;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
