package me.desht.chesscraft.results;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseUpdaterTask implements Runnable {
	private final Results handler;

	public DatabaseUpdaterTask(Results handler) {
		this.handler = handler;
	}

	@Override
	public void run() {
		Debugger.getInstance().debug("database writer thread starting");
		while (true) {
			try {
				DatabaseSavable savable = handler.pollDatabaseUpdate();	// block until there's a record available
				if (savable instanceof Results.EndMarker) {
					break;
				}
				Connection conn = handler.getDBConnection();
				if (conn != null) {
					savable.saveToDatabase(conn);
				}
			} catch (InterruptedException e) {
				LogUtils.warning("interrupted while saving database results");
				e.printStackTrace();
			} catch (SQLException e) {
				LogUtils.warning("failed to save results record to database: " + e.getMessage());
			}
		}
		Debugger.getInstance().debug("database writer thread exiting");
	}
}
