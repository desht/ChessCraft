package me.desht.chesscraft.results;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.log.ChessCraftLogger;

public abstract class ResultViewBase {
	private String viewType;
	
	ResultViewBase(String viewType) {
		this.viewType = viewType;
	}
	
	/**
	 * Rebuild the view from the result records - this would normally be done if any of the
	 * view parameters have been changed.
	 */
	public void rebuild() {
		try {
			Statement del = Results.getResultsHandler().getConnection().createStatement();
			del.executeUpdate("DELETE FROM " + viewType);
			for (ResultEntry re : Results.getResultsHandler().getEntries()) {
				addResult(re);
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't rebuild results view " + viewType + ": " + e.getMessage());
		}
	}
	
	public abstract void addResult(ResultEntry re);
	
	abstract int getInitialScore();
	
	/**
	 * Get a list of all player scores, highest first.
	 * 
	 * @return	A list of score records (player, score)
	 */
	public List<ScoreRecord> getScores() {
		return getScores(0);
	}
	
	/**
	 * Get the top N scores on the server, highest first.
	 * 
	 * @param n	The number of players to return
	 * @return	A list of score records (player, score)
	 */
	public List<ScoreRecord> getScores(int n) {
		List<ScoreRecord> res = new ArrayList<ScoreRecord>();
		
		try {
			Statement stmt = Results.getResultsHandler().getConnection().createStatement();
			StringBuilder query = new StringBuilder("SELECT player, score FROM " + viewType + " ORDER BY score DESC");
			if (n > 0) {
				query.append(" LIMIT ").append(n);
			}
			ResultSet rs = stmt.executeQuery(query.toString());
			while (rs.next()) {
				res.add(new ScoreRecord(rs.getString(1), rs.getInt(2)));
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("can't retrieve scores: " + e.getMessage());
		}
		
		return res;
	}
	
	protected void awardPoints(String player, int score) {
		System.out.println("awardscore " + player + " = " + score);
		if (score == 0) {
			return;
		}
		int current = getScore(player);
		setScore(player, current + score, true);
	}

	private void setScore(String player, int score, boolean updateOnly) {
		if (score < 0) {
			return;
		}
		
		try {
			ResultsDB rdb = Results.getResultsHandler().getDB();
			boolean inserted = false;
			if (!updateOnly) {
				PreparedStatement getPlayer = rdb.getCachedStatement("SELECT player FROM " + viewType + " WHERE player = ?");
				getPlayer.setString(1, player);
				ResultSet rs = getPlayer.executeQuery();
				if (!rs.next()) {
					// insert record
					PreparedStatement insert = rdb.getCachedStatement("INSERT INTO " + viewType + " VALUES (?,?)");
					insert.setString(1, player);
					insert.setInt(2, score);
					insert.executeUpdate();
					System.out.println("insert score " + player + " = " + score);
					inserted = true;
				}
			}
			if (!inserted) {
				// update existing record
				PreparedStatement update = rdb.getCachedStatement("UPDATE " + viewType + " SET score = ? WHERE player = ?");
				update.setString(2, player);
				update.setInt(1, score);
				update.executeUpdate();
				System.out.println("update score " + player + " = " + score);
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't set " + viewType + " score for " + player + ": " + e.getMessage());
		}
	}

	/**
	 * Get the score for the given player.  If the player is not yet in the database,
	 * return the initial score (and add the player with that score).
	 * 
	 * @param player	The player to check for
	 * @return			The player's score
	 */
	public int getScore(String player) {
		try {
			ResultsDB rdb = Results.getResultsHandler().getDB();
			PreparedStatement getPlayer = rdb.getCachedStatement("SELECT score FROM " + viewType + " WHERE player = ?");
			getPlayer.setString(1, player);
			ResultSet rs = getPlayer.executeQuery();
			if (rs.next()) {
				System.out.println("getscore " + player + " = " + rs.getInt(1));
				return rs.getInt(1);
			} else {
				PreparedStatement insert = rdb.getCachedStatement("INSERT INTO " + viewType + " VALUES (?,?)");
				insert.setString(1, player);
				insert.setInt(2, getInitialScore());
				insert.executeUpdate();
				System.out.println("getscore " + player + " init= " + getInitialScore());
				return getInitialScore();
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("Can't get " + viewType + " score for " + player + ": " + e.getMessage());
			return 0;
		}
	}
}
