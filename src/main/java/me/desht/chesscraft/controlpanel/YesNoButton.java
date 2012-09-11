package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.expector.ExpectUndoResponse;
import me.desht.chesscraft.expector.ExpectYesNoResponse;

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
		
		String playerName = game.getPlayer(colour);
		Player p = Bukkit.getPlayer(playerName);
		
		if (p == null) {
			// could be an AI player
//			LogUtils.warning("unknown player:" + playerName + " (offline?) in game " + game.getName());
			return ""; //$NON-NLS-1$
		} else if (ChessCraft.getInstance().responseHandler.isExpecting(playerName, ExpectDrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn"); //$NON-NLS-1$
		} else if (ChessCraft.getInstance().responseHandler.isExpecting(playerName, ExpectSwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn"); //$NON-NLS-1$
		} else if (ChessCraft.getInstance().responseHandler.isExpecting(playerName, ExpectUndoResponse.class)) {
			return Messages.getString("ControlPanel.acceptUndoBtn"); //$NON-NLS-1$
		} else {
			return ""; //$NON-NLS-1$
		}
	}
}
