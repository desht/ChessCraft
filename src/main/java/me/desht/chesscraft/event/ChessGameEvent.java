package me.desht.chesscraft.event;

import me.desht.chesscraft.chess.ChessGame;

public abstract class ChessGameEvent extends ChessEvent {
	private final ChessGame game;
	
	public ChessGameEvent(ChessGame game) {
		this.game = game;
	}
	
	public ChessGame getGame() {
		return game;
	}
}
