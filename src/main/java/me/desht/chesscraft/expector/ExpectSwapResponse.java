package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpectSwapResponse extends ExpectYesNoResponse {

	public ExpectSwapResponse(ChessGame game, int offererColour) {
		super(game, offererColour);
	}

	@Override
	public void doResponse(final UUID offereeId) {
		final UUID offererId = UUID.fromString(game.getPlayer(offererColour).getId());
		deferTask(offererId, new Runnable() {

			@Override
			public void run() {
				Player player = Bukkit.getPlayer(offereeId);
				if (player != null) {
					if (accepted) {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.swapOfferAccepted", player.getDisplayName()));
						game.swapColours();
					} else {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.swapOfferDeclined", player.getDisplayName()));
						MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer"));
					}
				}
			}
		});
	}

}
