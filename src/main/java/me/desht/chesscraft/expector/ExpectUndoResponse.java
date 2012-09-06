package me.desht.chesscraft.expector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;

public class ExpectUndoResponse extends ExpectYesNoResponse {

	public ExpectUndoResponse(ChessGame game, String offerer, String offeree) {
		super(game, offerer, offeree);
	}
	
	@Override
	public void doResponse(final String playerName) {
		deferTask(Bukkit.getPlayer(offerer), new Runnable() {

			@Override
			public void run() {
				if (accepted) {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.undoOfferAccepted", offeree)); //$NON-NLS-1$
					game.undoMove(offerer);
				} else {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.undoOfferDeclined", offeree)); //$NON-NLS-1$
					Player player = Bukkit.getPlayer(playerName);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedUndoOffer")); //$NON-NLS-1$
					}
				}
			}
		});
	}
}
