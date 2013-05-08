package me.desht.chesscraft.results;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseSavable {
	public void saveToDatabase(Connection conn) throws SQLException;
}
