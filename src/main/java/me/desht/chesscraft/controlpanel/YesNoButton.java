package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.expector.ExpectUndoResponse;
import me.desht.chesscraft.expector.ExpectYesNoResponse;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.Bukkit;
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
		
		ChessPlayer player = game.getPlayer(colour);
		if (player == null || !player.isHuman())
			return "";
		
		Player p = Bukkit.getPlayer(player.getName());
		
		ResponseHandler rh = ChessCraft.getInstance().responseHandler;
		if (p == null) {
			// gone offline, perhaps?
			return ""; //$NON-NLS-1$
		} else if (rh.isExpecting(p.getName(), ExpectDrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn"); //$NON-NLS-1$
		} else if (rh.isExpecting(p.getName(), ExpectSwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn"); //$NON-NLS-1$
		} else if (rh.isExpecting(p.getName(), ExpectUndoResponse.class)) {
			return Messages.getString("ControlPanel.acceptUndoBtn"); //$NON-NLS-1$
		} else {
			return ""; //$NON-NLS-1$
		}
	}
}
