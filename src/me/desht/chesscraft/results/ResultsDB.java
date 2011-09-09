package me.desht.chesscraft.results;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ResultsDB {

	private Connection connection;
	
	private final Map<String, PreparedStatement> statementCache = new HashMap<String, PreparedStatement>();

	ResultsDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			File dbFile = new File(ChessConfig.getResultsDir(), "results.db");
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
			setupTables();
		} catch (ClassNotFoundException e) {
			ChessCraftLogger.warning("SQLite not available, result logging disabled: " + e.getMessage());
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQLite connection failed, result logging disabled: " + e.getMessage());
		}
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public void shutdown() {
		try {
			if (!connection.getAutoCommit()) {
				connection.rollback();
			}
			connection.close();
		} catch (SQLException e) {
			ChessCraftLogger.warning("can't cleanly shut down DB connection: " + e.getMessage());
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
			//TODO?
			// init chess AIs?
			// (ai 17 is closer to 0 than 1000)
			// (will need to input ai names as something like __ai__## to avoid renaming problems)
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

	PreparedStatement getCachedStatement(String query) throws SQLException {
		if (!statementCache.containsKey(query)) {
			statementCache.put(query, connection.prepareStatement(query));
		}
		return statementCache.get(query);
	}
}
