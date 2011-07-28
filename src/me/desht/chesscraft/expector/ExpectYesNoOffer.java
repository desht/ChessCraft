package me.desht.chesscraft.expector;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessUtils;
import me.desht.chesscraft.Game;
import org.bukkit.entity.Player;

import me.desht.chesscraft.exceptions.ChessException;

public class ExpectYesNoOffer extends ExpectData {

	private Game game;
	private String offerer;
	private String offeree;
	private boolean accepted;

	public ExpectYesNoOffer(ChessCraft plugin, Game game, String offerer, String offeree) {
		super(plugin);
		this.game = game;
		this.offerer = offerer;
		this.offeree = offeree;
	}

	public void setReponse(boolean accepted) {
		this.accepted = accepted;
	}

	public Game getGame() {
		return game;
	}

	@Override
	public void doResponse(Player player) throws ChessException {
		switch (getAction()) {
			case DrawResponse:
				if (accepted) {
					game.alert(offerer, "Your draw offer has been accepted by &6" + offeree);
					game.drawn();
				} else {
					game.alert(offerer, "Your draw offer has been declined by &6" + offeree);
					ChessUtils.statusMessage(player, "You have declined the draw offer.");
				}
				break;
			case SwapResponse:
				if (accepted) {
					game.alert(offerer, "Your swap offer has been accepted by &6" + offeree);
					game.swapColours();
				} else {
					game.alert(offerer, "Your swap offer has been declined by &6" + offeree);
					ChessUtils.statusMessage(player, "You have declined the swap offer.");
				}
				break;
		}
	}
}
