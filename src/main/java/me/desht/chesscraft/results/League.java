package me.desht.chesscraft.results;

import me.desht.chesscraft.ChessCraft;
import org.bukkit.configuration.Configuration;

public class League extends ResultViewBase {

	private static final int WIN_POINTS = 2;
	private static final int LOSS_POINTS = 0;
	private static final int DRAW_POINTS = 1;

	public League(Results handler) {
		super(handler, "league");
	}

	@Override
	public void addResult(ResultEntry re) {
		String winner = re.getWinner();
		Configuration cfg = ChessCraft.getInstance().getConfig();
		if (winner != null) {
			String loser = re.getLoser();
			awardPoints(winner, cfg.getInt("league.win_points", WIN_POINTS));
			awardPoints(loser , cfg.getInt("league.loss_points", LOSS_POINTS));
		} else {
			awardPoints(re.getPlayerWhite(), cfg.getInt("league.draw_points", DRAW_POINTS));
			awardPoints(re.getPlayerBlack(), cfg.getInt("league.draw_points", DRAW_POINTS));
		}
	}

	@Override
	protected int getInitialScore() {
		return 0;
	}
}
