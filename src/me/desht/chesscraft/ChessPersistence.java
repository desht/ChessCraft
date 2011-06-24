package me.desht.chesscraft;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.yaml.snakeyaml.Yaml;

public class ChessPersistence {
	private ChessCraft plugin;
	private static final String gameFile = "games.yml";
	private static final String boardFile = "boards.yml";
	
	ChessPersistence(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	void saveAll() {
		saveBoards();
		saveGames();
	}
	
	void reloadAll() {
		loadBoards();
		loadGames();
	}

	static List<Object> makeBlockList(Location l) {
	    List<Object> list = new ArrayList<Object>();
	    list.add(l.getWorld().getName());
	    list.add(l.getBlockX());
	    list.add(l.getBlockY());
	    list.add(l.getBlockZ());
	
	    return list;
	}

	private void saveGames() {
		Map<String, Map<String,Object>> games = new HashMap<String, Map<String,Object>>();
		
		for (Game game : plugin.listGames()) {
			Map<String,Object> gameMap = game.freeze();
			games.put(game.getName(), gameMap);
		}

		writeMap(new File(plugin.getDataFolder(), gameFile), games);
	}

	private void saveBoards() {
		Map<String, Map<String,Object>> boards = new HashMap<String, Map<String,Object>>();
		
		for (BoardView bv : plugin.listBoardViews()) {
			Map<String,Object> gameMap = bv.freeze();
			boards.put(bv.getName(), gameMap);
		}

		writeMap(new File(plugin.getDataFolder(), boardFile), boards);
	}
	
	private void writeMap(File f, Map<String,Map<String,Object>> map) {
		Yaml yaml = new Yaml();
		try {
			yaml.dump(map, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")));
		} catch (IOException e) {
			plugin.log(Level.SEVERE, e.getMessage());
		}
		plugin.log(Level.INFO, "wrote " + map.size() + " objects to " + f);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Map<String,Object>> readMap(File f) {
		if (!f.exists()) { // create empty file if doesn't already exist
            try {
                f.createNewFile();
            } catch (IOException e) {
                plugin.log(Level.SEVERE, e.getMessage());
                return null;
            }
        }

        try {
            Yaml yaml = new Yaml();
        	return (Map<String,Map<String,Object>>) yaml.load(new FileInputStream(f));
        } catch (Exception e) {
        	plugin.log(Level.SEVERE, "caught exception loading " + f + ": " + e.getMessage());
        	makeBackup(f);
        	return null;
        }
  	}
	
	private void makeBackup(File f) {
		
	}

	private World findWorld(String worldName) {
	    World w = plugin.getServer().getWorld(worldName);
	
	    if (w != null) {
	    	return w;
	    } else {
	    	throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
	    }
	}

	@SuppressWarnings("unchecked")
	private	void loadBoards() {
		File f = new File(plugin.getDataFolder(), boardFile);
		Map<String,Map<String,Object>> map = readMap(f);
		
		for (String bvName : map.keySet()) {
			Map<String,Object> boardMap = map.get(bvName);
			List<Object>l0 = (List<Object>) boardMap.get("location");
			World w = findWorld((String) l0.get(0));
			Location loc = new Location(w, (Integer)l0.get(1), (Integer) l0.get(2), (Integer) l0.get(3));
			try {
				BoardView bv = new BoardView(bvName, plugin, loc, (String) boardMap.get("pieceStyle"));
				plugin.addBoardView(bvName, bv);
			} catch (Exception e) {
				plugin.log(Level.SEVERE, "can't load board " + bvName + ": " + e.getMessage());
				makeBackup(f);
			}
		}
	}
	private void loadGames() {
		File f = new File(plugin.getDataFolder(), gameFile);
		Map<String,Map<String,Object>> map = readMap(f);
		
		for (String gameName : map.keySet()) {
			Map<String,Object> gameMap = map.get(gameName);
			try {
				BoardView bv = plugin.getBoardView((String) gameMap.get("boardview"));
				Game game = new Game(plugin, gameName, bv, null);
				// TODO: set up all the fields
				plugin.addGame(gameName, game);
			} catch (Exception e) {
				plugin.log(Level.SEVERE, "can't load saved game " + gameName + ": " + e.getMessage());
				makeBackup(f);
			}
		}
	}
}