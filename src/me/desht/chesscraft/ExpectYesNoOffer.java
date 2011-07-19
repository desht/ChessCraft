package me.desht.chesscraft;

import org.bukkit.entity.Player;

import me.desht.chesscraft.exceptions.ChessException;

public class ExpectYesNoOffer extends ExpectData {

	private Game game;
	private String offerer;
	private String offeree;
	private boolean accepted;

	ExpectYesNoOffer(ChessCraft plugin, Game game, String offerer, String offeree) {
		super(plugin);
		this.game = game;
		this.offerer = offerer;
		this.offeree = offeree;
	}

	void setReponse(boolean accepted) {
		this.accepted = accepted;
	}

	Game getGame() {
		return game;
	}

	@Override
	void doResponse(Player player) throws ChessException {
		switch (getAction()) {
		case DrawResponse:
			if (accepted) {
				game.alert(offerer, "Your draw offer has been accepted by &6" + offeree);
				game.drawn();
			} else {
				game.alert(offerer, "Your draw offer has been declined by &6" + offeree);
			}
			break;
		case SwapResponse:
			if (accepted) {
				game.alert(offerer, "Your swap offer has been accepted by &6" + offeree);
				game.swapColours();
			} else {
				game.alert(offerer, "Your swap offer has been declined by &6" + offeree);
			}
			break;
		}
	}
}
