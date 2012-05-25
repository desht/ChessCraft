package me.desht.chesscraft.exceptions;

@SuppressWarnings("serial")
public class ChessException extends RuntimeException {

	public ChessException() {
	}

	public ChessException(String message) {
		super(message);
	}
}
