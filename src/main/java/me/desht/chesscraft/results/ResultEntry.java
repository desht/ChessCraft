package me.desht.chesscraft.results;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import chesspresso.Chess;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.GameResult;
import me.desht.dhutils.LogUtils;

public class ResultEntry {

	private final String playerWhite, playerBlack;
	private final String gameName;
	private final long startTime, endTime;
	private final GameResult result;
	private final String pgnResult;

	ResultEntry(ChessGame game, GameResult rt) {
		playerWhite = game.getPlayer(Chess.WHITE).getName();
		playerBlack = game.getPlayer(Chess.BLACK).getName();
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

	ResultEntry(ResultSet rs) throws SQLException {
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

	void save(Connection connection) {
		try {
			PreparedStatement stmt = connection.prepareStatement(
					"INSERT INTO results (playerWhite, playerBlack, gameName, startTime, endTime, result, pgnResult)" +
					" VALUES (?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, playerWhite);
			stmt.setString(2, playerBlack);
			stmt.setString(3, gameName);
			stmt.setTimestamp(4, new Timestamp(startTime));
			stmt.setTimestamp(5, new Timestamp(endTime));
			stmt.setString(6, result.toString());
			stmt.setString(7, pgnResult);
			stmt.executeUpdate();
		} catch (SQLException e) {
			LogUtils.warning("SQL insert failed: " + e.getMessage());
		}	
	}
}

