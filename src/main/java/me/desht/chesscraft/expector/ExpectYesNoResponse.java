package me.desht.chesscraft.expector;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.responsehandler.ResponseHandler;

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
	
	public static void handleYesNoResponse(Player player, boolean isAccepted) throws ChessException {
		ResponseHandler respHandler = ChessCraft.getInstance().responseHandler;

		Class<? extends ExpectYesNoResponse> c = null;
		if (respHandler.isExpecting(player.getName(), ExpectDrawResponse.class)) {
			c = ExpectDrawResponse.class;
		} else if (respHandler.isExpecting(player.getName(), ExpectSwapResponse.class)) {
			c = ExpectSwapResponse.class;
		} else if (respHandler.isExpecting(player.getName(), ExpectUndoResponse.class)) {
			c = ExpectUndoResponse.class;
		} else {
			return;
		}

		ExpectYesNoResponse response = (ExpectYesNoResponse) respHandler.getAction(player.getName(), c);
		response.setResponse(isAccepted);
		response.handleAction();
		response.getGame().getView().getControlPanel().repaintSignButtons();
	}
}
