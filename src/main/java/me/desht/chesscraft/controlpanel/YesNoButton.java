package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.chess.player.HumanChessPlayer;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.expector.ExpectUndoResponse;
import me.desht.chesscraft.expector.ExpectYesNoResponse;
import me.desht.dhutils.responsehandler.ResponseHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class YesNoButton extends AbstractSignButton {

	private final int colour;
	private final boolean yesOrNo;

	public YesNoButton(ControlPanel panel, int x, int y, int colour, boolean yesOrNo) {
		super(panel, yesOrNo ? "yesBtn" : "noBtn", null, x, y);
		this.colour = colour;
		this.yesOrNo = yesOrNo;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ExpectYesNoResponse.handleYesNoResponse(event.getPlayer(), yesOrNo);
	}

	@Override
	public boolean isEnabled() {
		return !getOfferText().isEmpty();
	}

	@Override
	public String[] getCustomSignText() {
		String[] text = getSignText();

		text[0] = getOfferText();

		return text;
	}

	private String getOfferText() {
		ChessGame game = getGame();
		if (game == null) return "";

		ChessPlayer cp = game.getPlayer(colour);
		if (cp == null || !cp.isHuman())
			return "";

		ResponseHandler resp = ChessCraft.getInstance().responseHandler;

		Player player = ((HumanChessPlayer) cp).getBukkitPlayer();
		if (player == null) {
			// gone offline, perhaps?
			return "";
		} else if (resp.isExpecting(player, ExpectDrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn");
		} else if (resp.isExpecting(player, ExpectSwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn");
		} else if (resp.isExpecting(player, ExpectUndoResponse.class)) {
			return Messages.getString("ControlPanel.acceptUndoBtn");
		} else {
			return "";
		}
	}
}
