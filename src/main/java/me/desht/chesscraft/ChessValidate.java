package me.desht.chesscraft;

import me.desht.chesscraft.exceptions.ChessException;

public class ChessValidate {
	public static void isTrue(boolean cond, String err) {
		if (!cond) throw new ChessException(err);
	}

	public static void isFalse(boolean cond, String err) {
		if (cond) throw new ChessException(err);
	}

	public static void notNull(Object o, String err) {
		if (o == null) throw new ChessException(err);
	}
}
