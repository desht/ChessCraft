package me.desht.chesscraft.chess.player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.chess.ai.AbstractAI;
import me.desht.chesscraft.chess.ai.AIFactory.AIDefinition;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

public class AIChessPlayer extends ChessPlayer {

	private final AbstractAI ai;
	
	public AIChessPlayer(String name, ChessGame game, int colour) {
		super(name, game, colour);
		
		ai = AIFactory.instance.getNewAI(game, name, colour == Chess.WHITE);
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
		AIDefinition aiDef = AIFactory.instance.getAIDefinition(getName());
		if (aiDef == null) {
			LogUtils.warning("can't find AI definition for " + getName());
			return 2.0;
		} else {
			return 1.0 + aiDef.getPayoutMultiplier();
		}
	}

	@Override
	public void drawOffered() {
		// do nothing here
	}

	@Override
	public void swapOffered() {
		// do nothing here
	}

	@Override
	public void undoLastMove() {
		ai.setActive(false);
		ai.undoLastMove();
	}

	@Override
	public void checkPendingMove() {
		if (ai.hasFailed()) {
			// this will happen if the AI caught an exception and its state can't be guaranteed anymore
			try {
				getGame().drawn(GameResult.Abandoned);
			} catch (ChessException e) {
				// should never get here
				LogUtils.severe("Unexpected exception caught while trying to draw game - deleted", e);
				getGame().deletePermanently();
			}
		} else if (ai.getPendingMove()) {
			int from = ai.getPendingFrom();
			int to = ai.getPendingTo();
			ai.clearPendingMove();
			try {
				getGame().doMove(getName(), to, from);
			} catch (IllegalMoveException e) {
				getGame().alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
				ai.setFailed(true);
			} catch (ChessException e) {
				getGame().alert(Messages.getString("ChessAI.AIunexpectedException", e.getMessage())); //$NON-NLS-1$
				ai.setFailed(true);
			}
		}
	}
}
