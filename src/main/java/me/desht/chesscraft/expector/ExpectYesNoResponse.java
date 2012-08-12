package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.ChessGame;

public abstract class ExpectYesNoResponse extends ExpectChessBase {

	protected final ChessGame game;
	protected final String offerer;
	protected final String offeree;
	protected boolean accepted;

	public ExpectYesNoResponse(ChessGame game, String offerer, String offeree) {
		this.game = game;
		this.offerer = offerer;
		this.offeree = offeree;
	}

	public void setResponse(boolean accepted) {
		this.accepted = accepted;
	}

	public ChessGame getGame() {
		return game;
	}
}
