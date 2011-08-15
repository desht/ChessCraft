package me.desht.chesscraft.results;

import me.desht.chesscraft.ChessConfig;

public class League extends ResultViewBase {

	private final int WIN_POINTS = 2;
	private final int LOSS_POINTS = 0;
	private final int DRAW_POINTS = 1;
	
	public League() {
		super("league");
	}

	@Override
	public void addResult(ResultEntry re) {
		String winner = re.getWinner();
		if (winner != null) {
			String loser = re.getLoser();
			awardPoints(winner, ChessConfig.getConfiguration().getInt("league.win_points", WIN_POINTS));	
			awardPoints(loser , ChessConfig.getConfiguration().getInt("league.loss_points", LOSS_POINTS));
		} else {
			awardPoints(re.playerWhite, ChessConfig.getConfiguration().getInt("league.draw_points", DRAW_POINTS));
			awardPoints(re.playerBlack, ChessConfig.getConfiguration().getInt("league.draw_points", DRAW_POINTS));
		}
	}

	@Override
	int getInitialScore() {
		return 0;
	}
}
