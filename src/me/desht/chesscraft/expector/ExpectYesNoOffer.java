package me.desht.chesscraft.expector;

import me.desht.chesscraft.ChessCraft;
import me.desht.util.ChessUtils;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.Messages;

import org.bukkit.entity.Player;

import me.desht.chesscraft.exceptions.ChessException;

public class ExpectYesNoOffer extends ExpectData {

	private ChessGame game;
	private String offerer;
	private String offeree;
	private boolean accepted;

	public ExpectYesNoOffer(ChessCraft plugin, ChessGame game, String offerer, String offeree) {
		super(plugin);
		this.game = game;
		this.offerer = offerer;
		this.offeree = offeree;
	}

	public void setReponse(boolean accepted) {
		this.accepted = accepted;
	}

	public ChessGame getGame() {
		return game;
	}

	@Override
	public void doResponse(Player player) throws ChessException {
		switch (getAction()) {
			case DrawResponse:
				if (accepted) {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.drawOfferAccepted", offeree)); //$NON-NLS-1$
					game.drawn();
				} else {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.drawOfferDeclined", offeree)); //$NON-NLS-1$
					ChessUtils.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedDrawOffer")); //$NON-NLS-1$
				}
				break;
			case SwapResponse:
				if (accepted) {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferAccepted", offeree)); //$NON-NLS-1$
					game.swapColours();
				} else {
					game.alert(offerer, Messages.getString("ExpectYesNoOffer.swapOfferDeclined", offeree)); //$NON-NLS-1$
					ChessUtils.statusMessage(player, Messages.getString("ExpectYesNoOffer.youDeclinedSwapOffer")); //$NON-NLS-1$
				}
				break;
		}
	}
}
