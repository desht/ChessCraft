package me.desht.chesscraft.results;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ResultEntry {

	private String playerWhite, playerBlack;
	private String gameName;
	private long startTime, endTime;
	private GameResult result;
	private String pgnResult;

	ResultEntry(ChessGame game, GameResult rt) {
		playerWhite = game.getPlayerWhite();
		playerBlack = game.getPlayerBlack();
		gameName = game.getName();
		startTime = game.getStarted();
		endTime = game.getFinished();
		result = rt;
		pgnResult = game.getPGNResult();
	}

	ResultEntry(String plw, String plb, String gn, long start, long end, String pgnRes, GameResult rt) {
		playerWhite = plw;
		playerBlack = plb;
		gameName = gn;
		startTime = start;
		endTime = end;
		result = rt;
		pgnResult = pgnRes;
	}

	public String getPlayerWhite() {
		return playerWhite;
	}

	public String getPlayerBlack() {
		return playerBlack;
	}

	public String getGameName() {
		return gameName;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public GameResult getResult() {
		return result;
	}

	public String getPgnResult() {
		return pgnResult;
	}

	public String getWinner() {
		if (pgnResult.equals("1-0")) {
			return playerWhite;
		} else if (pgnResult.equals("0-1")) {
			return playerBlack;
		} else {
			return null;
		}
	}

	public String getLoser() {
		if (pgnResult.equals("1-0")) {
			return playerBlack;
		} else if (pgnResult.equals("0-1")) {
			return playerWhite;
		} else {
			return null;
		}
	}

	public ResultEntry(ResultSet rs) throws SQLException {
		try {
			playerWhite = rs.getString("playerwhite");
			playerBlack = rs.getString("playerBlack");
			gameName = rs.getString("gameName");
			startTime = rs.getDate("startTime").getTime();
			endTime = rs.getDate("endTime").getTime();
			result = GameResult.valueOf(rs.getString("result"));
			pgnResult = rs.getString("pgnResult");
		} catch (SQLException e) {
			throw e;
		}
	}

	void save(Connection connection) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
					"INSERT INTO results VALUES (?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, playerWhite);
			stmt.setString(2, playerBlack);
			stmt.setString(3, gameName);
			stmt.setDate(4, new Date(startTime));
			stmt.setDate(5, new Date(endTime));
			stmt.setString(6, result.toString());
			stmt.setString(7, pgnResult);
			stmt.executeUpdate();
		} catch (SQLException e) {
			ChessCraftLogger.warning("SQL insert failed: " + e.getMessage());
		}	
	}
}

