package me.desht.chesscraft.results;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.Game;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.log.ChessCraftLogger;

public class Results {
	private static Results results = null;	// singleton class
	private Connection connection;
	private final List<ResultEntry> entries = new ArrayList<ResultEntry>();
	private Ladder ladder;

	private Results() {
		
	}

	public static Results getResultsHandler() {
		if (results == null) {
			results = new Results();
			results.initDB();
			results.loadEntries();
			results.ladder = new Ladder();
		}
		return results;
	}

	public Ladder getLadder() {
		return ladder;
	}

	public List<ResultEntry> getEntries() {
		return entries;
	}

	public Connection getConnection() {
		return connection;
	}

	public void logResult(Game game, GameResult rt) {
		if (game.getState() != GameState.FINISHED) {
			return;
		}
	
		ResultEntry re = new ResultEntry(game, rt);
		entries.add(re);
		getLadder().addResult(re);
		re.save(connection);
	}
	
	/**
	 * Get the number of wins for a player
	 * 
	 * @param playerName	The player to check
	 * @return	The number of games this player has won
	 */
	public int getWins(String playerName) {
		int nWins = 0;
		
		try {
			PreparedStatement stmtW = connection.prepareStatement(
					"SELECT COUNT(playerWhite) FROM results WHERE " +
					"pgnResult = '1-0' AND playerWhite = ?");
			PreparedStatement stmtB = connection.prepareStatement(
					"SELECT COUNT(playerBlack) FROM results WHERE " +
					"pgnResult = '0-1' AND playerBlack = ?");
			return doSearch(playerName, stmtW, stmtB);
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQL query failed: " + e.getMessage());
		}
		
		return nWins;
	}

	/**
	 * Get the number of draws for a player
	 * 
	 * @param playerName	The player to check
	 * @return	The number of games this player has drawn
	 */
	public int getDraws(String playerName) {
		int nDraws = 0;
		
		try {
			PreparedStatement stmtW = connection.prepareStatement(
					"SELECT COUNT(playerWhite) FROM results WHERE " +
					"pgnResult = '1/2-1/2' AND playerWhite = ?");
			PreparedStatement stmtB = connection.prepareStatement(
					"SELECT COUNT(playerBlack) FROM results WHERE " +
					"pgnResult = '1/2-1/2' AND playerBlack = ?");
			return doSearch(playerName, stmtW, stmtB);
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQL query failed: " + e.getMessage());
		}
		
		return nDraws;
	}
	
	/**
	 * Get the number of losses for a player
	 * 
	 * @param playerName	The player to check
	 * @return	The number of games this player has lost
	 */
	public int getLosses(String playerName) {
		try {
			PreparedStatement stmtW = connection.prepareStatement(
					"SELECT COUNT(playerWhite) FROM results WHERE " +
					"pgnResult = '0-1' AND playerWhite = ?");
			PreparedStatement stmtB = connection.prepareStatement(
					"SELECT COUNT(playerBlack) FROM results WHERE " +
					"pgnResult = '1-0' AND playerBlack = ?");
			return doSearch(playerName, stmtW, stmtB);
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQL query failed: " + e.getMessage());
			return 0;
		}
	}
	
	/**
	 * Return the league table score for a player.  A win count as 3 points,
	 * a draw as 1 point, a loss as 0 points.
	 * 
	 * @param playerName	The player to check
	 * @return	The player's total score
	 */
	public int getScore(String playerName) {
		return getWins(playerName) * 3 + getDraws(playerName);
	}

	private int doSearch(String playerName, PreparedStatement stmtW, PreparedStatement stmtB) throws SQLException {
		int count = 0;
		ResultSet rs = stmtW.executeQuery(playerName);
		count += rs.getInt(1);
		rs = stmtB.executeQuery(playerName);
		count += rs.getInt(1);
		return count;
	}
	
	private void initDB() {
		if (connection == null) {
			try {
				Class.forName("org.sqlite.JDBC");
				File dbFile = new File(ChessConfig.getResultsDir(), "results.db");
				System.out.println(dbFile);
				connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
				setupTables();
			} catch (ClassNotFoundException e) {
				ChessCraftLogger.warning("SQLite not available, result logging disabled: " + e.getMessage());
			} catch (SQLException e) {
				ChessCraftLogger.warning("SQLite connection failed, result logging disabled: " + e.getMessage());
			}
		}
	}

	private void setupTables() throws SQLException {
		try {
			if (!tableExists("results")) {
				String ddl = "CREATE TABLE results (" +
						"playerWhite TEXT NOT NULL," +
						"playerBlack TEXT NOT NULL," +
						"gameName TEXT NOT NULL," +
						"startTime INTEGER NOT NULL," +
						"endTime INTEGER NOT NULL," +
						"result TEXT, pgnResult TEXT NOT NULL," +
						"PRIMARY KEY (gameName, startTime))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("ladder")) {
				System.out.println("create table ladder");
				String ddl = "CREATE TABLE ladder (" +
						"player TEXT NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("league")) {
				String ddl = "CREATE TABLE league (" +
						"player TEXT NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQLite table creation failed: " + e.getMessage());
			throw e;
		}
	}
	
	private boolean tableExists(String table) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null , null, table, null);
		return tables.next();
	}

	private void loadEntries() {
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM results");
			while (rs.next()) {
				ResultEntry e = new ResultEntry(rs);
				entries.add(e);
			}
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQL query failed: " + e.getMessage());
		}
	}
}
