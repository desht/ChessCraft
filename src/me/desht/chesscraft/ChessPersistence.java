package me.desht.chesscraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

public class ChessPersistence {
	private ChessCraft plugin;
	private static final String persistFile = "persist.yml";

	ChessPersistence(ChessCraft plugin) {
		this.plugin = plugin;
	}

	void save() {
		savePersistedData();
	}

	void reload() {
		for (Game game : Game.listGames())
			game.delete();
		for (BoardView view : BoardView.listBoardViews())
			view.delete();
		
		loadPersistedData();
	}

	static List<Object> makeBlockList(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	static World findWorld(String worldName) {
		World w = Bukkit.getServer().getWorld(worldName);

		if (w != null) {
			return w;
		} else {
			throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
		}
	}

	private void savePersistedData() {
		Configuration conf = new Configuration(new File(plugin.getDataFolder(), persistFile));

		conf.setProperty("current_games", Game.getCurrentGames());

		List<Map<String, Object>> boards = new ArrayList<Map<String, Object>>();
		for (BoardView bv : BoardView.listBoardViews()) {
			boards.add(bv.freeze());
		}
		conf.setProperty("boards", boards);

		List<Map<String, Object>> games = new ArrayList<Map<String, Object>>();
		for (Game game : Game.listGames()) {
			games.add(game.freeze());
		}
		conf.setProperty("games", games);

		conf.save();
	}

	@SuppressWarnings("unchecked")
	private void loadPersistedData() {
		File f = new File(plugin.getDataFolder(), persistFile);
		Configuration conf = new Configuration(f);
		conf.load();

		int nWantedBoards = 0, nLoadedBoards = 0;
		int nWantedGames = 0, nLoadedGames = 0;

		List<Map<String, Object>> boards = (List<Map<String, Object>>) conf.getProperty("boards");
		if (boards != null) {
			nWantedBoards = boards.size();
			nLoadedBoards = loadBoards(boards);
		}
		List<Map<String, Object>> games = (List<Map<String, Object>>) conf.getProperty("games");
		if (games != null) {
			nWantedGames = games.size();
			nLoadedGames = loadGames(games);
		}

		if (nWantedBoards != nLoadedBoards || nWantedGames != nLoadedGames)
			makeBackup(f);

		Map<String, String> cgMap = (Map<String, String>) conf.getProperty("current_games");
		if (cgMap != null) {
			for (Entry<String, String> entry : cgMap.entrySet()) {
				try {
					Game.setCurrentGame(entry.getKey(), entry.getValue());
				} catch (ChessException e) {
					plugin.log(Level.WARNING, "can't set current game for player " + entry.getKey() + ": "
							+ e.getMessage());
				}
			}
		}
	}

	private void makeBackup(File original) {
		try {
			File backup = getBackupFileName(original.getParentFile(), persistFile);

			plugin.log(Level.INFO, "An error occurred while loading the saved data, so a backup copy of " + original
					+ " is being created. The backup can be found at " + backup.getPath());
			copy(original, backup);
		} catch (IOException e) {
			plugin.log(Level.SEVERE, "Error while trying to write backup file: " + e);
		}
	}

	static File getBackupFileName(File parentFile, String template) {
		String ext = ".BACKUP.";
		File backup;
		int idx = 0;

		do {
			backup = new File(parentFile, template + ext + idx);
			idx++;
		} while (backup.exists());
		return backup;
	}

	static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	@SuppressWarnings("unchecked")
	private int loadBoards(List<Map<String, Object>> boardList) {
		int nLoaded = 0;
		for (Map<String, Object> boardMap : boardList) {
			String bvName = (String) boardMap.get("name");
			List<Object> origin = (List<Object>) boardMap.get("origin");
			World w = findWorld((String) origin.get(0));
			Location originLoc = new Location(w, (Integer) origin.get(1), (Integer) origin.get(2), (Integer) origin
					.get(3));
			try {
				BoardView bv = new BoardView(bvName, plugin, originLoc, (String) boardMap.get("boardStyle"),
						(String) boardMap.get("pieceStyle"));
				bv.getControlPanel().repaintSignButtons();
				nLoaded++;
			} catch (Exception e) {
				plugin.log(Level.SEVERE, "can't load board " + bvName + ": " + e.getMessage());
			}
		}
		plugin.log(Level.INFO, "loaded " + nLoaded + " saved boards.");
		return nLoaded;
	}

	private int loadGames(List<Map<String, Object>> gameList) {
		int nLoaded = 0;
		for (Map<String, Object> gameMap : gameList) {
			String gameName = (String) gameMap.get("name");
			try {
				BoardView bv = BoardView.getBoardView((String) gameMap.get("boardview"));
				Game game = new Game(plugin, gameName, bv, null);
				game.thaw(gameMap);
				Game.addGame(gameName, game);
				game.getView().getControlPanel().repaintSignButtons();
				nLoaded++;
			} catch (Exception e) {
				plugin.log(Level.SEVERE, "can't load saved game " + gameName + ": " + e.getMessage());
			}
		}
		plugin.log(Level.INFO, "loaded " + nLoaded + " saved games.");
		return nLoaded;
	}

}