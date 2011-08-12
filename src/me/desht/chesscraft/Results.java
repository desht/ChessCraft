package me.desht.chesscraft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import me.desht.chesscraft.enums.GameResult;
import me.desht.chesscraft.enums.GameState;

public class Results {
	private static Results results = null;	// singleton class
	
	private final List<ResultEntry> entries = new ArrayList<ResultEntry>();
	
	private Results() {
		
	}
	
	static Results getResultsHandler() {
		if (results == null) {
			results = new Results();
			results.load();
		}
		return results;
	}
	
	public void logResult(Game game, GameResult rt) {
		if (game.getState() != GameState.FINISHED) {
			return;
		}
		
		entries.add(new ResultEntry(game, rt));
		
		save();
	}
	
	// TODO: load/save of flat files like this is quite inefficient (especially
	// when there are many results)
	//  consider using SQLite?
	
	private void load() {
		Configuration c = new Configuration(new File(ChessConfig.getResultsDir(), "results.yml"));
		c.load();
		
		List<ConfigurationNode> l = c.getNodeList("results", null);
		if (l == null) {
			return;
		}
		for (ConfigurationNode node : l) {
			entries.add(new ResultEntry(node));
		}
	}
	
	private void save() {
		Configuration c = new Configuration(new File(ChessConfig.getResultsDir(), "results.yml"));
		
		List<Map<String, String>> l = new ArrayList<Map<String,String>>();
		for (ResultEntry e : entries) {
			l.add(e.freeze());
		}
		c.setProperty("results", l);
		c.save();
	}
	
	/*----------------------------------------------------------------*/
	
	private class ResultEntry {
		String playerWhite, playerBlack;
		String gameName;
		long startTime, endTime;
		GameResult result;
		String pgnResult;
		
		ResultEntry(Game game, GameResult rt) {
			playerWhite = game.getPlayerWhite();
			playerBlack = game.getPlayerBlack();
			gameName = game.getName();
			startTime = game.getStarted();
			endTime = game.getFinished();
			result = rt;
			pgnResult = game.getPGNResult();
		}
		
		ResultEntry(ConfigurationNode node) {
			playerWhite = node.getString("playerWhite");
			playerBlack = node.getString("playerBlack");
			gameName = node.getString("gameName");
			startTime = Long.parseLong(node.getString("startTime"));
			endTime = Long.parseLong(node.getString("endTime"));
			result = GameResult.valueOf(node.getString("result"));
			pgnResult = node.getString("pgnResult");
		}
		
		Map<String, String> freeze() {
			Map<String, String> res = new HashMap<String, String>();
			res.put("playerWhite", playerWhite);
			res.put("playerBlack", playerBlack);
			res.put("gameName", gameName);
			res.put("startTime", Long.toString(startTime));
			res.put("endTime", Long.toString(endTime));
			res.put("result", result.toString());
			res.put("pgnResult", pgnResult);
			return res;
		}
	}
}
