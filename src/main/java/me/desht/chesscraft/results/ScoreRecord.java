package me.desht.chesscraft.results;

/**
 * @author des
 *
 */
public class ScoreRecord {
	private final String player;
	private final int score;
	
	/**
	 * Create a new score record
	 * 
	 * @param p	The player
	 * @param s	The player's score
	 */
	public ScoreRecord(String p, int s) {
		player = p;
		score = s;
	}
	
	/**
	 * Get the player
	 * 
	 * @return	The player
	 */
	public String getPlayer() {
		return player;
	}
	
	/**
	 * Get the score
	 * 
	 * @return	The score
	 */
	public int getScore() {
		return score;
	}
	
}
