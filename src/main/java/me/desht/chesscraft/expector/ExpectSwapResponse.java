package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpectSwapResponse extends ExpectYesNoResponse {

	public ExpectSwapResponse(ChessGame game, String offerer) {
		super(game, offerer);
	}

	@Override
	public void doResponse(final String offeree) {
		deferTask(Bukkit.getPlayer(offerer), new Runnable() {

			@Override
			public void run() {
				if (accepted) {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferAccepted", getPlayerName())); //$NON-NLS-1$
					game.swapColours();
				} else {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferDeclined", getPlayerName())); //$NON-NLS-1$
					Player player = Bukkit.getPlayer(offeree);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer")); //$NON-NLS-1$
					}
				}
			}
		});
	}

}
