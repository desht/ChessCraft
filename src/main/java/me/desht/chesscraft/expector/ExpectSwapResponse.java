package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class ExpectSwapResponse extends ExpectYesNoResponse {

	public ExpectSwapResponse(ChessGame game, String offerer, String offeree) {
		super(game, offerer, offeree);
	}

	@Override
	public void doResponse(Player player) throws ChessException {
		if (accepted) {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferAccepted", offeree)); //$NON-NLS-1$
			game.swapColours();
		} else {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferDeclined", offeree)); //$NON-NLS-1$
			ChessUtils.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer")); //$NON-NLS-1$
		}

	}

}
