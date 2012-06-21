package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.TimeControlDefs;
import me.desht.chesscraft.enums.GameState;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerInteractEvent;

import chesspresso.Chess;

public class TimeControlButton extends AbstractSignButton {

	private final TimeControlDefs tcDefs;
	
	public TimeControlButton(ControlPanel panel) {
		super(panel, "timeControl", "tc", 3, 0);

		tcDefs = new TimeControlDefs();
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();
		if (game == null) return;
		
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			tcDefs.nextDef(); break;
		case RIGHT_CLICK_BLOCK:
			tcDefs.prevDef(); break;
		}
		game.setTimeControl(tcDefs.currentDef().getSpec());
		getPanel().updateClock(Chess.WHITE, game.getTcWhite());
		getPanel().updateClock(Chess.BLACK, game.getTcBlack());
		
		repaint();
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	protected String[] getCustomSignText() {
		String[] text = getSignText();
		
		ChessGame game = getGame();
		String col;
		if (game == null) {
			col = "";
		} else if (game.getState() == GameState.SETTING_UP) {
			col = ChatColor.DARK_RED.toString();
		} else {
			col = ChatColor.BLACK.toString();
		}
		
		String[] tcText = tcDefs.currentDef().getLabel();
		int start = tcText.length < 3 ? 2 : 1;
		
		for (int l = start, i = 0; l < 4; l++, i++) {
			text[l] = col + (i < tcText.length ? tcText[i] : "");
		}
		
		return text;
	}
}
