package me.desht.chesscraft.chess.player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.chesscraft.chess.ai.ChessAI;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

public class AIChessPlayer extends ChessPlayer {

	private final ChessAI ai;

	public AIChessPlayer(String id, ChessGame game, int colour) {
		super(id, id, game, colour);

		ai = AIFactory.getInstance().getNewAI(game, id, colour == Chess.WHITE);
	}

	@Override
	public void promptForFirstMove() {
		ai.setActive(true);
	}

	@Override
	public void promptForNextMove() {
		Move m = getGame().getPosition().getLastMove();
		ai.userHasMoved(m.getFromSqi(), m.getToSqi());
	}

	@Override
	public void alert(String message) {
		// do nothing here
	}

	@Override
	public void statusMessage(String message) {
		// do nothing here
	}

	@Override
	public void replayMoves() {
		ai.replayMoves(getGame().getHistory());
	}

	@Override
	public String getDisplayName() {
		return ai.getDisplayName();
	}

	@Override
	public void cleanup() {
		ai.delete();
	}

	@Override
	public void validateAffordability(String error) {
		// nothing to do here - AI's have infinite resources, for now
		// (limited AI resources a possible future addition)
	}

	@Override
	public void validateInvited(String error) {
		// nothing to do here - AI's do not need invites
	}

	@Override
	public boolean isHuman() {
		return false;
	}

	@Override
	public void withdrawFunds(double amount) {
		// nothing to do here - AI's have infinite resources, for now
	}

	@Override
	public void depositFunds(double amount) {
		// nothing to do here - AI's have infinite resources, for now
	}

	@Override
	public void summonToGame() {
		// nothing to do here
	}

	@Override
	public void cancelOffers() {
		// AI doesn't respond to offers right now - possible future addition
	}

	@Override
	public double getPayoutMultiplier() {
		AIDefinition aiDef = AIFactory.getInstance().getAIDefinition(getId());
		if (aiDef == null) {
			LogUtils.warning("can't find AI definition for " + getDisplayName());
			return 2.0;
		} else {
			return 1.0 + aiDef.getPayoutMultiplier();
		}
	}

	@Override
	public void drawOffered() {
		ai.offerDraw();
	}

	@Override
	public void swapOffered() {
		// do nothing here
	}

	@Override
	public void undoOffered() {
		// do nothing here
	}

	@Override
	public void undoLastMove() {
		ai.setActive(false);
		ai.undoLastMove();
	}

	@Override
	public void checkPendingAction() {
		ChessGame game = getGame();
		ChessPlayer otherPlayer = game.getPlayer(Chess.otherPlayer(getColour()));

		if (ai.hasFailed()) {
			// this will happen if the AI caught an exception and its state can't be guaranteed anymore
			try {
				if (ChessCraft.getInstance().getConfig().getBoolean("ai.lose_on_fail", false)) {
					game.winByDefault(otherPlayer.getId());
				} else {
					game.drawn(GameResult.Abandoned);
				}
			} catch (ChessException e) {
				// should never get here
				LogUtils.severe("Unexpected exception caught while trying to draw game - deleted", e);
				ChessGameManager.getManager().deleteGame(game.getName(), true);
			}
		} else {
			// see if the AI has any pending actions from the other thread that we need to pick up
			switch (ai.getPendingAction()) {
			case MOVED:
				int from = ai.getPendingFrom();
				int to = ai.getPendingTo();
				try {
					getGame().doMove(getId(), from, to);
				} catch (IllegalMoveException e) {
					getGame().alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
					ai.setFailed(true);
				} catch (ChessException e) {
					getGame().alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
					ai.setFailed(true);
				}
				break;
			case DRAW_OFFERED:
				game.offerDraw(getId());
				break;
			case DRAW_ACCEPTED:
				if (otherPlayer != null) {
					otherPlayer.alert(Messages.getString("ExpectYesNoOffer.drawOfferAccepted", getDisplayName()));
				}
				game.drawn(GameResult.DrawAgreed);
				break;
			case DRAW_DECLINED:
				if (otherPlayer != null) {
					otherPlayer.alert(Messages.getString("ExpectYesNoOffer.drawOfferDeclined", getDisplayName()));
				}
				break;
			default:
				break;
			}
			ai.clearPendingAction();
		}
	}

	@Override
	public void playEffect(String effect) {
		// do nothing
	}

	@Override
	public void notifyTimeControl(TimeControl timeControl) {
		ai.notifyTimeControl(timeControl);
	}
}
