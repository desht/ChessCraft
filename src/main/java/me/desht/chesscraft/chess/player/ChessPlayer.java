package me.desht.chesscraft.chess.player;

import me.desht.chesscraft.chess.ChessGame;
import chesspresso.Chess;

public abstract class ChessPlayer {
	private final String name;
	private final ChessGame game;
	
	private int colour;
	private int promotionPiece;
	
	public ChessPlayer(String name, ChessGame game, int colour) {
		if (name == null) throw new NullPointerException("ChessPlayer: name must not be null");
		if (game == null) throw new NullPointerException("ChessPlayer: game must not be null");
		this.name = name;
		this.game = game;
		this.colour = colour;
		this.promotionPiece = Chess.QUEEN;
	}
	
	public String getName() {
		return name;
	}
	
	public int getColour() {
		return colour;
	}
	
	public int getOtherColour() {
		return Chess.otherPlayer(colour);
	}
	
	public void setColour(int colour) {
		this.colour = colour;
	}

	public ChessGame getGame() {
		return game;
	}
	
	public void setPromotionPiece(int promotionPiece) {
		this.promotionPiece = promotionPiece;
	}

	public int getPromotionPiece() {
		return promotionPiece;
	}

	public int cyclePromotionPiece() {
		switch (promotionPiece) {
		case Chess.QUEEN:
			promotionPiece = Chess.KNIGHT; break;
		case Chess.KNIGHT:
			promotionPiece = Chess.BISHOP; break;
		case Chess.BISHOP:
			promotionPiece = Chess.ROOK; break;
		case Chess.ROOK:
			promotionPiece = Chess.QUEEN; break;
		default:
			promotionPiece = Chess.QUEEN; break;
		}
		return promotionPiece;
	}

	public abstract void validateAffordability(String error);
	
	public abstract void validateInvited(String error);
	
	public abstract void promptForFirstMove();
	public abstract void promptForNextMove();

	public abstract void alert(String message);

	public abstract void statusMessage(String message);

	public abstract void replayMoves();

	public abstract void cleanup();
	
	public abstract boolean isHuman();
	
	public abstract void withdrawFunds(double amount);
	public abstract void depositFunds(double amount);
	
	public String getDisplayName() {
		return name;
	}

	public abstract void summonToGame();

	public abstract void cancelOffers();

	public abstract double getPayoutMultiplier();
	
	public abstract void drawOffered();

	public abstract void swapOffered();
	
	public abstract void undoLastMove();
	
	public abstract void checkPendingMove();

	public abstract void playEffect(String effect);
}
