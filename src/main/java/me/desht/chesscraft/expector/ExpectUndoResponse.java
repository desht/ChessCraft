package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MiscUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpectUndoResponse extends ExpectYesNoResponse {

	public ExpectUndoResponse(ChessGame game, int offererColour) {
		super(game, offererColour);
	}

	@Override
	public void doResponse(final UUID playerId) {
		final UUID offererId = UUID.fromString(game.getPlayer(offererColour).getId());
		deferTask(offererId, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(playerId);
				if (player != null) {
					if (accepted) {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.undoOfferAccepted", player.getDisplayName()));
						game.undoMove(offererId.toString());
					} else {
						game.alert(offererId, Messages.getString("ExpectYesNoOffer.undoOfferDeclined", player.getDisplayName()));
						MiscUtil.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedUndoOffer"));
					}
				}
			}
		});
	}
}
