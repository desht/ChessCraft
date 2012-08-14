package me.desht.chesscraft.event;

import me.desht.chesscraft.chess.BoardView;

public abstract class ChessBoardEvent extends ChessEvent {

	protected final BoardView boardView;
	
	public ChessBoardEvent(BoardView boardView) {
		this.boardView = boardView;
	}
	
	public BoardView getBoardView() {
		return boardView;
	}
}
