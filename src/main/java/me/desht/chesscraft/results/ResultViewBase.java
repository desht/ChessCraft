package me.desht.chesscraft.results;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.dhutils.LogUtils;

/**
 * @author des
 * 
 * Abstract base class to represent a view on the raw results data.  Subclass
 * this and implement the addResult() and getInitialScore() methods.
 */
public abstract class ResultViewBase {
	private final Results handler;
	private final String viewType;
	private Map<String, Integer> scoreMap = null;
	
	ResultViewBase(Results handler, String viewType) {
		this.viewType = viewType;
		this.handler = handler;
	}
	
	/**
	 * Rebuild the view from the result records - this would normally be done if any of the
	 * view parameters have been changed.
	 */
	public void rebuild() {
		if (!Results.resultsHandlerOK()) {
			return;
		}
		
		try {
			// we build the new results in an internal map, and then write them to the DB
			// in a single transaction - much better performance this way
			Connection conn = handler.getConnection();
			conn.setAutoCommit(false);
			Statement del = conn.createStatement();	
			del.executeUpdate("DELETE FROM " + viewType);
			scoreMap = new HashMap<String, Integer>();
			for (ResultEntry re : Results.getResultsHandler().getEntries()) {
				addResult(re);
			}
			PreparedStatement insert = handler.getResultsDB().getCachedStatement("INSERT INTO " + viewType + " VALUES (?,?)");
			for (Entry<String, Integer> e : scoreMap.entrySet()) {
				insert.setString(1, e.getKey());
				insert.setInt(2, e.getValue());
				insert.executeUpdate();
			}
			scoreMap = null;
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			LogUtils.warning("Can't rebuild results view " + viewType + ": " + e.getMessage());
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
			Statement stmt = handler.getConnection().createStatement();
			StringBuilder query = new StringBuilder("SELECT player, score FROM " + viewType + " ORDER BY score DESC");
			if (n > 0) {
				query.append(" LIMIT ").append(n);
			}
			ResultSet rs = stmt.executeQuery(query.toString());
			while (rs.next()) {
				res.add(new ScoreRecord(rs.getString(1), rs.getInt(2)));
			}
		} catch (SQLException e) {
			LogUtils.warning("can't retrieve scores: " + e.getMessage());
		}
		
		return res;
	}
	
	protected void awardPoints(String player, int score) {
//		System.out.println("awardscore " + player + " = " + score);
		
		int current = getScore(player);
		if (score == 0) {
			return;
		}
		setScore(player, current + score, true);
	}

	public void setScore(String player, int score, boolean updateOnly) {
		if (score < 0) {
			return;
		}
		
		if (scoreMap != null) {
			// work on the internal score map - we would do this when doing batch calculations
			// (e.g. a full rebuild of the view) for performance reasons
			scoreMap.put(player, score);
			return;
		}
		
		try {
			ResultsDB rdb = handler.getResultsDB();
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
//					System.out.println("insert score " + player + " = " + score);
					inserted = true;
				}
			}
			if (!inserted) {
				// update existing record
				PreparedStatement update = rdb.getCachedStatement("UPDATE " + viewType + " SET score = ? WHERE player = ?");
				update.setString(2, player);
				update.setInt(1, score);
				update.executeUpdate();
//				System.out.println("update score " + player + " = " + score);
			}
		} catch (SQLException e) {
			LogUtils.warning("Can't set " + viewType + " score for " + player + ": " + e.getMessage());
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
		if (scoreMap != null) {
			// work on the internal score map - we would do this when doing batch calculations
			// (e.g. a full rebuild of the view) for performance reasons
			if (!scoreMap.containsKey(player)) {
				scoreMap.put(player, getInitialScore());
			}
			return scoreMap.get(player);
		}
		
		try {
			ResultsDB rdb = handler.getResultsDB();
			PreparedStatement getPlayer = rdb.getCachedStatement("SELECT score FROM " + viewType + " WHERE player = ?");
			getPlayer.setString(1, player);
			ResultSet rs = getPlayer.executeQuery();
			if (rs.next()) {
//				System.out.println("getscore " + player + " = " + rs.getInt(1));
				return rs.getInt(1);
			} else {
				PreparedStatement insert = rdb.getCachedStatement("INSERT INTO " + viewType + " VALUES (?,?)");
				insert.setString(1, player);
				insert.setInt(2, getInitialScore());
				insert.executeUpdate();
//				System.out.println("getscore " + player + " init= " + getInitialScore());
				return getInitialScore();
			}
		} catch (SQLException e) {
			LogUtils.warning("Can't get " + viewType + " score for " + player + ": " + e.getMessage());
			return 0;
		}
	}
}
