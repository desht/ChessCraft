package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameResult;
import me.desht.dhutils.MiscUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpectDrawResponse extends ExpectYesNoResponse {

	public ExpectDrawResponse(ChessGame game, int offererColour) {
		super(game, offererColour);
	}

	@Override
	public void doResponse(final UUID offeree) {
		final UUID offererId = UUID.fromString(game.getPlayer(offererColour).getId());
		deferTask(offererId, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(offeree);
				if (player != null) {
					if (accepted) {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.drawOfferAccepted", player.getDisplayName()));
						game.drawn(GameResult.DrawAgreed);
					} else {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.drawOfferDeclined", player.getDisplayName()));
						MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedDrawOffer"));
					}
				}
			}

		});
	}
}
