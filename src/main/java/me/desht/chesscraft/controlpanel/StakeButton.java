package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.util.ChessUtils;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class StakeButton extends AbstractSignButton {

	public StakeButton(ControlPanel panel) {
		super(panel, "stakeBtn", "stake", 7, 1);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		double stakeIncr;
		if (event.getPlayer().isSneaking()) {
			stakeIncr = ChessCraft.getInstance().getConfig().getDouble("stake.smallIncrement"); //$NON-NLS-1$
		} else {
			stakeIncr = ChessCraft.getInstance().getConfig().getDouble("stake.largeIncrement"); //$NON-NLS-1$
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			stakeIncr = -stakeIncr;
		}

		getGame().adjustStake(event.getPlayer().getName(), stakeIncr);

		repaint();
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public boolean isReactive() {
		ChessGame game = getGame();
		if (game == null) return false;
		if (getView().getLockStake()) return false;

		return game.getState() == GameState.SETTING_UP &&
				(game.getBlackPlayerName().isEmpty() || game.getWhitePlayerName().isEmpty());
	}

	@Override
	protected String[] getCustomSignText() {
		String[] res = getSignText();

		ChessGame game = getGame();
		double stake = game == null ? getView().getDefaultStake() : game.getStake();
		String[] s =  ChessUtils.formatStakeStr(stake).split(" ", 2);
		res[2] = getIndicatorColour() + s[0];
		res[3] = s.length > 1 ? getIndicatorColour() + s[1] : "";

		return res;
	}
}
