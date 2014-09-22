package me.desht.chesscraft.expector;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.dhutils.responsehandler.ResponseHandler;
import org.bukkit.entity.Player;

public abstract class ExpectYesNoResponse extends ExpectBase {
	protected final ChessGame game;
	protected final int offererColour;
	protected boolean accepted;

	public ExpectYesNoResponse(ChessGame game, int offererColour) {
		this.game = game;
		this.offererColour = offererColour;
	}

	public void setResponse(boolean accepted) {
		this.accepted = accepted;
	}

	public ChessGame getGame() {
		return game;
	}

	/**
	 * The given player has just typed "yes" or "no" (or used a Yes/No button).  Work out to what offer they're
	 * responding, and carry out the associated action.
	 *
	 * @param player the player
	 * @param isAccepted true if accepted, false if declined
	 * @throws ChessException
	 */
	public static void handleYesNoResponse(Player player, boolean isAccepted) throws ChessException {
		ResponseHandler respHandler = ChessCraft.getInstance().responseHandler;

		// TODO: code smell!
		Class<? extends ExpectYesNoResponse> c;
		if (respHandler.isExpecting(player, ExpectDrawResponse.class)) {
			c = ExpectDrawResponse.class;
		} else if (respHandler.isExpecting(player, ExpectSwapResponse.class)) {
			c = ExpectSwapResponse.class;
		} else if (respHandler.isExpecting(player, ExpectUndoResponse.class)) {
			c = ExpectUndoResponse.class;
		} else {
			return;
		}

		ExpectYesNoResponse response = respHandler.getAction(player, c);
		response.setResponse(isAccepted);
		response.handleAction(player);
//		response.getGame().getView().getControlPanel().repaintControls();
	}
}
