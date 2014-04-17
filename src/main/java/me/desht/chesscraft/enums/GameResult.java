package me.desht.chesscraft.enums;

public enum GameResult {
	Checkmate("Game.checkmated"),
	Stalemate("Game.stalemated"),
	DrawAgreed("Game.drawAgreed"),
	Resigned("Game.resigned"),
	Abandoned("Game.abandoned"),
	FiftyMoveRule("Game.fiftyMoveRule"),
	Forfeited("Game.forfeited");

	private final String msgKey;

	GameResult(String msgKey) {
		this.msgKey = msgKey;
	}

	public String getMsgKey() {
		return msgKey;
	}
}
