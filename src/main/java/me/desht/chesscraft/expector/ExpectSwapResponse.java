package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class ExpectSwapResponse extends ExpectYesNoResponse {

	public ExpectSwapResponse(ChessGame game, String offerer, String offeree) {
		super(game, offerer, offeree);
	}

	@Override
	public void doResponse(Player player) {
		if (accepted) {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferAccepted", offeree)); //$NON-NLS-1$
			game.swapColours();
		} else {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferDeclined", offeree)); //$NON-NLS-1$
			MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer")); //$NON-NLS-1$
		}

	}

}
