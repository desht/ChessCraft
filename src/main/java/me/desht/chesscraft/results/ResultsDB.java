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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.dhutils.LogUtils;

public class ResultsDB {

	private final Connection connection;

	private final Map<String, PreparedStatement> statementCache = new HashMap<String, PreparedStatement>();

	ResultsDB() throws ClassNotFoundException, SQLException {
		String dbType = ChessCraft.getInstance().getConfig().getString("database.driver", "sqlite");

		if (dbType.equals("mysql")) {
			connection = connectMySQL();
			setupTablesMySQL();
		} else {
			// sqlite is the default
			connection = connectSQLite();
			setupTablesSQLite();
			checkForOldFormatData();
		}
		LogUtils.fine("Connected to DB: " + connection.getMetaData().getDatabaseProductName());
	}

	private void checkForOldFormatData() {
		File oldDbFile = new File(DirectoryStructure.getResultsDir(), "results.db");
		if (!oldDbFile.exists()) {
			return;
		}
		try {
			LogUtils.info("Migrating old-format game results into new DB schema...");
			Class.forName("org.sqlite.JDBC");
			Connection oldConn = DriverManager.getConnection("jdbc:sqlite:" + oldDbFile.getAbsolutePath());
			Statement st = oldConn.createStatement();
			ResultSet rs = st.executeQuery("select * from results");
			List<ResultEntry> entries = new ArrayList<ResultEntry>();
			while (rs.next()) {
				ResultEntry e = new ResultEntry(rs);
				entries.add(e);
			}
			oldConn.close();
			connection.setAutoCommit(false);
			for (ResultEntry re : entries) {
				re.save(connection);
			}
			connection.setAutoCommit(true);
			LogUtils.info("Sucessfully migrated " + entries.size() + " old-format game results");
			File oldDbBackup = new File(DirectoryStructure.getResultsDir(), "oldresults.db");
			if (!oldDbFile.renameTo(oldDbBackup)) {
				LogUtils.warning("couldn't rename " + oldDbFile + " to" + oldDbBackup);
			}
			Bukkit.getScheduler().runTask(ChessCraft.getInstance(), new Runnable() {
				@Override
				public void run() {
					Results.getResultsHandler().rebuildViews();		
				}
			});
		} catch (Exception e) {
			LogUtils.warning("Could not migrate old-format game results: " + e.getMessage());
		}
	}

	public Connection getConnection() {
		return connection;
	}

	private Connection connectSQLite() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		File dbFile = new File(DirectoryStructure.getResultsDir(), "gameresults.db");
		return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
	}

	private Connection connectMySQL() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		Configuration config = ChessCraft.getInstance().getConfig();
		String user = config.getString("database.user", "chesscraft");
		String pass = config.getString("database.password", "");
		String host = config.getString("database.host", "localhost");
		String dbName = config.getString("database.name", "chesscraft");
		int port = config.getInt("database.port", 3306);
		String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
		return DriverManager.getConnection(url, user, pass);
	}

	public void shutdown() {
		try {
			if (!connection.getAutoCommit()) {
				connection.rollback();
			}
			LogUtils.fine("Closing DB connection to " + connection.getMetaData().getDatabaseProductName());
			connection.close();
		} catch (SQLException e) {
			LogUtils.warning("can't cleanly shut down DB connection: " + e.getMessage());
		}
	}

	private void setupTablesSQLite() throws SQLException {
		try {
			if (!tableExists("results")) {
				String ddl = "CREATE TABLE results (" +
						"gameID INTEGER PRIMARY KEY," +
						"playerWhite VARCHAR(32) NOT NULL," +
						"playerBlack VARCHAR(32) NOT NULL," +
						"gameName VARCHAR(64) NOT NULL," +
						"startTime DATETIME NOT NULL," +
						"endTime DATETIME NOT NULL," +
						"result TEXT NOT NULL," +
						"pgnResult TEXT NOT NULL)";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("ladder")) {
				String ddl = "CREATE TABLE ladder (" +
						"player VARCHAR(32) NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("league")) {
				String ddl = "CREATE TABLE league (" +
						"player VARCHAR(32) NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
		} catch (SQLException e) {
			LogUtils.warning("Table creation failed: " + e.getMessage());
			throw e;
		}
	}

	private void setupTablesMySQL() throws SQLException {
		try {
			if (!tableExists("results")) {
				String ddl = "CREATE TABLE results (" +
						"gameID INT(11) NOT NULL AUTO_INCREMENT," +
						"playerWhite VARCHAR(32) NOT NULL," +
						"playerBlack VARCHAR(32) NOT NULL," +
						"gameName VARCHAR(64) NOT NULL," +
						"startTime DATETIME NOT NULL," +
						"endTime DATETIME NOT NULL," +
						"result TEXT NOT NULL," +
						"pgnResult TEXT NOT NULL," +
						"PRIMARY KEY (gameID))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("ladder")) {
				String ddl = "CREATE TABLE ladder (" +
						"player VARCHAR(32) NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
			if (!tableExists("league")) {
				String ddl = "CREATE TABLE league (" +
						"player VARCHAR(32) NOT NULL," +
						"score INTEGER NOT NULL," +
						"PRIMARY KEY (player))";
				Statement stmt = connection.createStatement();
				stmt.executeUpdate(ddl);
			}
		} catch (SQLException e) {
			LogUtils.warning("Table creation failed: " + e.getMessage());
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
