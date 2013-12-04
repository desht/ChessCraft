package me.desht.chesscraft.results;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultsDB {
	private enum SupportedDrivers {
		MYSQL,
		SQLITE
	}

	private final Connection connection;

	ResultsDB() throws ClassNotFoundException, SQLException {
		String dbType = ChessCraft.getInstance().getConfig().getString("database.driver", "sqlite");
		SupportedDrivers driver = SupportedDrivers.valueOf(dbType.toUpperCase());
		switch (driver) {
		case MYSQL:
			connection = connectMySQL();
			setupTablesMySQL();
			break;
		case SQLITE:
			connection = connectSQLite();
			setupTablesSQLite();
			break;
		default:
			throw new ChessException("unsupported database type: " + dbType);
		}
		setupTablesCommon();
		checkForOldFormatData();
		LogUtils.fine("Connected to DB: " + connection.getMetaData().getDatabaseProductName());
	}

	void shutdown() {
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
			ResultSet rs = st.executeQuery("select * from " + Results.getResultsHandler().getTableName("results"));
			List<ResultEntry> entries = new ArrayList<ResultEntry>();
			while (rs.next()) {
				ResultEntry e = new ResultEntry(rs);
				entries.add(e);
			}
			oldConn.close();
			connection.setAutoCommit(false);
			for (ResultEntry re : entries) {
				re.saveToDatabase(connection);
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

	private void setupTablesSQLite() throws SQLException {
		createTableIfNotExists("results",
		                       "gameID INTEGER PRIMARY KEY," +
		                    		   "playerWhite VARCHAR(32) NOT NULL," +
		                    		   "playerBlack VARCHAR(32) NOT NULL," +
		                    		   "gameName VARCHAR(64) NOT NULL," +
		                    		   "startTime DATETIME NOT NULL," +
		                    		   "endTime DATETIME NOT NULL," +
		                    		   "result TEXT NOT NULL," +
				"pgnResult TEXT NOT NULL");
	}

	private void setupTablesMySQL() throws SQLException {
		createTableIfNotExists("results",
		                       "gameID INTEGER NOT NULL AUTO_INCREMENT," +
		                    		   "playerWhite VARCHAR(32) NOT NULL," +
		                    		   "playerBlack VARCHAR(32) NOT NULL," +
		                    		   "gameName VARCHAR(64) NOT NULL," +
		                    		   "startTime DATETIME NOT NULL," +
		                    		   "endTime DATETIME NOT NULL," +
		                    		   "result TEXT NOT NULL," +
		                    		   "pgnResult TEXT NOT NULL," +
				"PRIMARY KEY (gameID)");
	}

	private void setupTablesCommon() throws SQLException {
		createTableIfNotExists("ladder",
		                       "player VARCHAR(32) NOT NULL," +
		                    		   "score INTEGER NOT NULL," +
				"PRIMARY KEY (player)");
		createTableIfNotExists("league",
		                       "player VARCHAR(32) NOT NULL," +
		                    		   "score INTEGER NOT NULL," +
				"PRIMARY KEY (player)");
		createTableIfNotExists("pgn",
		                       "gameID INTEGER NOT NULL," +
		                    		   "pgnData TEXT NOT NULL," +
				"FOREIGN KEY (gameID) REFERENCES results(gameID) ON DELETE CASCADE");
	}

	private void createTableIfNotExists(String tableName, String ddl) throws SQLException {
		String fullName = ChessCraft.getInstance().getConfig().getString("database.table_prefix", "chesscraft_") + tableName;
		Statement stmt = connection.createStatement();
		try {
			if (tableExists(tableName)) {
				stmt.executeUpdate("ALTER TABLE " + tableName + " RENAME TO " + fullName);
				LogUtils.info("renamed DB table " + tableName + " to " + fullName);
			} else if (!tableExists(fullName)) {
				stmt.executeUpdate("CREATE TABLE " + fullName + "(" + ddl + ")");
			}
		} catch (SQLException e) {
			LogUtils.warning("can't execute " + stmt + ": " + e.getMessage());
			throw e;
		}
	}

	private boolean tableExists(String table) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null , null, table, null);
		return tables.next();
	}
}
