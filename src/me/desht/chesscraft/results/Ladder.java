package me.desht.chesscraft.results;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.results.ResultEntry;

public class Ladder extends ResultViewBase {
	
	private final int INITIAL_POS = 1000;
	private final int MIN_CHANGE = 25;
	private final int DIFF_DIVISOR = 4;
	
	/**
	 * Create a new Ladder object.
	 */
	public Ladder() {
		super("ladder");
	}
	
	/**
	 * Add one result to the ladder.
	 * 
	 * @param re	The ResultEntry to add
	 */
	@Override
	public void addResult(ResultEntry re) {
		String winner = re.getWinner();
		if (winner != null) {
			int diffDivisor = ChessConfig.getConfiguration().getInt("ladder.diff_divisor", DIFF_DIVISOR);
			int minChange = ChessConfig.getConfiguration().getInt("ladder.min_change", MIN_CHANGE);
			String loser = re.getLoser();
			int lWinner = getScore(winner);
			int lLoser = getScore(loser);
			int diff = Math.abs(lWinner - lLoser);
			System.out.println(String.format("ladder: winner = %s, loser = %s, diff = %d", winner, loser, diff));
			awardPoints(winner, Math.max(diff / diffDivisor, minChange));
			awardPoints(loser,  -(Math.max(diff / diffDivisor, minChange)));
		}
	}

	@Override
	int getInitialScore() {
		return ChessConfig.getConfiguration().getInt("ladder.initial_position", INITIAL_POS);
	} 
}