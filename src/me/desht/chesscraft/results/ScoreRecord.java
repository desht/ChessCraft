package me.desht.chesscraft.results;

public class ScoreRecord {
	private String player;
	private int score;
	
	public ScoreRecord(String p, int s) {
		player = p;
		score = s;
	}
	
	public String getPlayer() {
		return player;
	}
	
	public int getScore() {
		return score;
	}
	
}
