package me.desht.chesscraft.results;

import java.sql.SQLException;

import me.desht.dhutils.LogUtils;

public class DatabaseUpdaterTask implements Runnable {

	private final Results handler;

	public DatabaseUpdaterTask(Results handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		while (true) {
			try {
				DatabaseSavable savable = handler.pollDatabaseUpdate();	// block until there's a record available
				savable.saveToDatabase(handler.getConnection());
			} catch (InterruptedException e) {
				LogUtils.warning("interrupted while saving database results");
				e.printStackTrace();
			} catch (SQLException e) {
				LogUtils.warning("failed to save results record to database: " + e.getMessage());
			}
		}
	}
}
