package me.desht.chesscraft.results;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.results.ResultEntry;

public class Ladder {
	private final Map<String, Integer> ladder = new HashMap<String, Integer>();
	
	private final int INITIAL_POS = 1000;
	private final int MIN_CHANGE = 25;
	private final int DIFF_DIVISOR = 4;
	
	private final int initialPos, minChange, diffDivisor;
	
	/**
	 * Create a new Ladder, with data loaded from the database.
	 */
	public Ladder() {
		initialPos = ChessConfig.getConfiguration().getInt("ladder.initial_position", INITIAL_POS);
		minChange = ChessConfig.getConfiguration().getInt("ladder.min_change", MIN_CHANGE);
		diffDivisor = ChessConfig.getConfiguration().getInt("ladder.diff_divisor", DIFF_DIVISOR);
		
		loadAll();
	}
	
	/**
	 * Create a new Ladder, calculating player positions from the result log.
	 * Save the data to database when done.  This would normally be called if the
	 * ladder needs to be recalculated due to parameter change.
	 * 
	 * @param l	List of entries to load into the ladder
	 */
	public Ladder(List<ResultEntry> l) {
		initialPos = ChessConfig.getConfiguration().getInt("ladder.initial_position", INITIAL_POS);
		minChange = ChessConfig.getConfiguration().getInt("ladder.min_change", MIN_CHANGE);
		diffDivisor = ChessConfig.getConfiguration().getInt("ladder.diff_divisor", DIFF_DIVISOR);
		
		for (ResultEntry re : l) {
			addResult(re);
		}
		
		saveAll();
	}
	
	public void loadAll() {
		Connection conn = Results.getResultsHandler().getConnection();
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM ladder");
			while (rs.next()) {
				ladder.put(rs.getString(1), rs.getInt(2));
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't read ladder data: " + e.getMessage());
		}
	}
	
	
	public void saveAll() {
		Connection conn = Results.getResultsHandler().getConnection();
		try {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM ladder");
			PreparedStatement stmtUpdate = conn.prepareStatement("INSERT INTO ladder VALUES (?, ?)"); 
			for (Entry<String, Integer> entry : ladder.entrySet()) {
				stmtUpdate.setString(1, entry.getKey());
				stmtUpdate.setInt(2, entry.getValue());
				stmtUpdate.execute();
			}
			conn.commit();
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't write ladder data: " + e.getMessage());
		}
	}
	
	public void savePlayer(String player) {
		Connection conn = Results.getResultsHandler().getConnection();
		
		try {
			PreparedStatement stmtUpdate = conn.prepareStatement("INSERT INTO ladder VALUES (?, ?)");
			stmtUpdate.setString(1, player);
			stmtUpdate.setInt(2, ladder.get(player));
			stmtUpdate.execute();
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't write ladder data for " + player + ": " + e.getMessage());
		}
	}
	
	public void addResult(ResultEntry e) {
		String winner = e.getWinner();
		if (winner == null) {
			// it was a draw
			awardScore(e.playerWhite, minChange);
			awardScore(e.playerBlack, minChange);
		} else {
			String loser = e.getLoser();
			int lWinner = getScore(winner);
			int lLoser = getScore(loser);
			int diff = Math.abs(lWinner - lLoser);
			awardScore(winner, Math.min(diff / diffDivisor, minChange));
			awardScore(loser,  -(Math.min(diff / diffDivisor, minChange)));
		}
	}

	private void awardScore(String player, int score) {
		int current = getScore(player);
		setScore(player, current + score);
	}

	private void setScore(String player, int score) {
		ladder.put(player, score);
		savePlayer(player);
	}

	private int getScore(String player) {
		if (!ladder.containsKey(player)) {
			ladder.put(player, initialPos);
			savePlayer(player);
		}
		return ladder.get(player);
	}
}