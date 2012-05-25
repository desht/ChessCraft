package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class ExpectDrawResponse extends ExpectYesNoResponse {

	public ExpectDrawResponse(ChessGame game, String offerer, String offeree) {
		super(game, offerer, offeree);
	}

	@Override
	public void doResponse(Player player) {
		if (accepted) {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.drawOfferAccepted", offeree)); //$NON-NLS-1$
			game.drawn();
		} else {
			game.alert(offerer, Messages.getString("ExpectYesNoOffer.drawOfferDeclined", offeree)); //$NON-NLS-1$
			MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedDrawOffer")); //$NON-NLS-1$
		}
	}
}
