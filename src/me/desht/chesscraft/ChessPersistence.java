package me.desht.chesscraft;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.config.Configuration;

public class ChessPersistence {

	private ChessCraft plugin;
	private FilenameFilter ymlFilter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".yml");
		}
	};

	public ChessPersistence(ChessCraft plugin) {
		this.plugin = plugin;
	}

	public void save() {
		savePersistedData();
	}

	public void reload() {
		for (Game game : Game.listGames()) {
			game.deleteTransitory();
		}
		for (BoardView view : BoardView.listBoardViews()) {
			view.delete();
		}

		loadPersistedData();
	}

	public static List<Object> makeBlockList(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	public static World findWorld(String worldName) {
		World w = Bukkit.getServer().getWorld(worldName);

		if (w != null) {
			return w;
		} else {
			throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
		}
	}

	private void savePersistedData() {
		saveGames();
		saveBoards();

		Configuration conf = new Configuration(ChessConfig.getPersistFile());

		conf.setProperty("current_games", Game.getCurrentGames());

		conf.save();
	}

	@SuppressWarnings("unchecked")
	private void loadPersistedData() {
		if (!loadOldPersistedData()) {
			// load v0.3 or later saved data - they are in a subdirectory, one
			// file per entry
			int nLoadedBoards = loadBoards();
			int nLoadedGames = loadGames();

			ChessCraftLogger.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games.");

			try {
				Configuration conf = new Configuration(ChessConfig.getPersistFile());
				conf.load();

				Map<String, String> cgMap = (Map<String, String>) conf.getProperty("current_games");
				if (cgMap != null) {
					for (Entry<String, String> entry : cgMap.entrySet()) {
						try {
							Game.setCurrentGame(entry.getKey(), entry.getValue());
						} catch (ChessException e) {
							ChessCraftLogger.log(Level.WARNING, "can't set current game for player " + entry.getKey() + ": "
									+ e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Unexpected Error while loading " + ChessConfig.getPersistFile().getName());
				moveBackup(ChessConfig.getPersistFile());
			}
		} else {
			save();
		}
	}

	/**
	 * old loading routine if the old file is found, will load it, make a
	 * backup, then delete
	 * 
	 * @return true if the old file was found
	 */
	@SuppressWarnings("unchecked")
	private boolean loadOldPersistedData() {
		final String oldPersistFilename = "persist.yml";
		File f = new File(plugin.getDataFolder(), oldPersistFilename);
		if (f.exists()) {
			try {
				Configuration conf = new Configuration(f);
				conf.load();

				int nWantedBoards = 0, nLoadedBoards = 0;
				int nWantedGames = 0, nLoadedGames = 0;

				List<Map<String, Object>> boards = (List<Map<String, Object>>) conf.getProperty("boards");
				if (boards != null) {
					nWantedBoards = boards.size();

					/**
					 * v0.2 or earlier saved boards - all in the main
					 * persist.yml file
					 * 
					 * @param boardList
					 *            a list of the boards
					 * @return the number of games that were sucessfully loaded
					 */
					for (Map<String, Object> boardMap : boards) {
						String bvName = (String) boardMap.get("name");
						List<Object> origin = (List<Object>) boardMap.get("origin");
						World w = findWorld((String) origin.get(0));
						Location originLoc = new Location(w, (Integer) origin.get(1), (Integer) origin.get(2),
								(Integer) origin.get(3));
						try {
							BoardView.addBoardView(new BoardView(bvName, plugin, originLoc, (String) boardMap.get("boardStyle"), (String) boardMap.get("pieceStyle")));
							++nLoadedBoards;
						} catch (Exception e) {
							ChessCraftLogger.log(Level.SEVERE, "can't load board " + bvName + ": " + e.getMessage());
						}
					}
				}
				List<Map<String, Object>> games = (List<Map<String, Object>>) conf.getProperty("games");
				if (games != null) {
					nWantedGames = games.size();
					/**
					 * v0.2 or earlier saved games - all in the main persist.yml
					 * file
					 * 
					 * @param gameList
					 *            a list of the map objects for each game
					 * @return the number of games that were sucessfully loaded
					 */
					for (Map<String, Object> gameMap : games) {
						if (loadGame(gameMap)) {
							++nLoadedGames;
						}
					}
				}

				if (nWantedBoards != nLoadedBoards || nWantedGames != nLoadedGames) {
					ChessCraftLogger.log(Level.INFO, "An error occurred while loading the saved data");
				}

				ChessCraftLogger.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games from old file.");

				for (BoardView bv : BoardView.listBoardViews()) {
					bv.paintAll();
				}

				Map<String, String> cgMap = (Map<String, String>) conf.getProperty("current_games");
				if (cgMap != null) {
					for (Entry<String, String> entry : cgMap.entrySet()) {
						try {
							Game.setCurrentGame(entry.getKey(), entry.getValue());
						} catch (ChessException e) {
							ChessCraftLogger.log(Level.WARNING, "can't set current game for player " + entry.getKey() + ": "
									+ e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Unexpected Error while loading the saved data: " + e.getMessage());
			}
			ChessCraftLogger.log(Level.INFO, "old file will be backed up, just in case");
			File backup = getBackupFileName(f.getParentFile(), oldPersistFilename);
			// rename much easier than copy & delete :)
			f.renameTo(backup);

			return true;
		}
		return false;
	}

	/**
	 * move to a backup & delete original <br>
	 * (for if the file is considered corrupt)
	 * 
	 * @param original
	 *            file to backup
	 */
	private void moveBackup(File original) {
		File backup = getBackupFileName(original.getParentFile(), original.getName());

		ChessCraftLogger.log(Level.INFO, "An error occurred while loading " + original.getName() + ":\n"
				+ "a backup copy has been saved to " + backup.getPath());
		original.renameTo(backup);
	}

	public static File getBackupFileName(File parentFile, String template) {
		String ext = ".BACKUP.";
		File backup;
		int idx = 0;

		do {
			backup = new File(parentFile, template + ext + idx);
			++idx;
		} while (backup.exists());
		return backup;
	}

	@SuppressWarnings("unchecked")
	private int loadBoards() {
		int nLoaded = 0;
		for (File f : ChessConfig.getBoardPersistDirectory().listFiles(ymlFilter)) {
			try {
				Configuration conf = new Configuration(f);
				conf.load();

				String bvName = conf.getString("name");
				List<Object> origin = (List<Object>) conf.getProperty("origin");
				World w = findWorld((String) origin.get(0));
				Location originLoc = new Location(w, (Integer) origin.get(1), (Integer) origin.get(2),
						(Integer) origin.get(3));
				BoardView.addBoardView(new BoardView(bvName, plugin, originLoc, conf.getString("boardStyle"),
						conf.getString("pieceStyle")));
				++nLoaded;
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Error loading " + f.getName() + ": " + e.getMessage());
				moveBackup(f);
			}
		}
		return nLoaded;
	}

	protected void saveBoards() {
		for (BoardView b : BoardView.listBoardViews()) {
			saveBoard(b);
		}
	}

	public void saveBoard(BoardView board) {
		Configuration conf = new Configuration(new File(ChessConfig.getBoardPersistDirectory(),
				safeFileName(board.getName()) + ".yml"));
		Map<String, Object> st = board.freeze();
		for (String key : st.keySet()) {
			conf.setProperty(key, st.get(key));
		}
		conf.save();
	}

	/**
	 * v0.3 or later saved games - one save file per game in their own
	 * subdirectory
	 * 
	 * @return the number of games that were sucessfully loaded
	 */
	private int loadGames() {
		HashMap<String, Map<String, Object>> toLoad = new HashMap<String, Map<String, Object>>();
		// game validation checks
		for (File f : ChessConfig.getGamesPersistDirectory().listFiles(ymlFilter)) {
			try {
				Configuration conf = new Configuration(f);
				conf.load();
				conf.setProperty("filename", f.getName());
				String board = conf.getString("boardview");
				if (board == null) {
					ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + f.getName() + ": boardview is null");
					moveBackup(f);
				} else if (toLoad.containsKey(board)) {
					// only load the newer game
					int tstart = conf.getInt("started", 0);
					int ostart = (Integer) toLoad.get(board).get("started");
					if (ostart >= tstart) {
						ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + f.getName() + ": another game is using the same board");
						moveBackup(f);
					} else {
						String fn = (String) toLoad.get(board).get("filename");
						ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + fn + ": another game is using the same board");
						(new File(f.getParentFile(), fn)).renameTo(new File(f.getParentFile(), fn + ".bak"));
						toLoad.put(board, conf.getAll());
					}
				} else {
					toLoad.put(board, conf.getAll());
				}
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Error loading " + f.getName(), e);
				moveBackup(f);
			}
		}
		// now actually load games
		int nLoaded = 0;
		for (Map<String, Object> gameMap : toLoad.values()) {
			if (loadGame(gameMap)) {
				++nLoaded;
			} else {
				moveBackup(new File(ChessConfig.getGamesPersistDirectory(), (String) gameMap.get("filename")));
			}
		}
		return nLoaded;
	}

	private boolean loadGame(Map<String, Object> gameMap) {
		String gameName = (String) gameMap.get("name");
		try {
			BoardView bv = BoardView.getBoardView((String) gameMap.get("boardview"));
			Game game = new Game(plugin, gameName, bv, null);
			if (game.thaw(gameMap)) {
				Game.addGame(gameName, game);
				return true;
			}
		} catch (Exception e) {
			ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + gameName + ": " + e.getMessage(), e);
		}
		return false;
	}

	protected void saveGames() {
		for (Game game : Game.listGames()) {
			saveGame(game);
		}
	}

	public void saveGame(Game game) {
		Configuration gConf = new Configuration(new File(ChessConfig.getGamesPersistDirectory(),
				safeFileName(game.getName()) + ".yml"));
		Map<String, Object> map = game.freeze();
		for (Entry<String, Object> e : map.entrySet()) {
			gConf.setProperty(e.getKey(), e.getValue());
		}
		gConf.save();
	}

	protected static String safeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}

	public void removeGameSavefile(Game game) {
		File f = new File(ChessConfig.getGamesPersistDirectory(), game.getName() + ".yml");
		if (!f.delete()) {
			ChessCraftLogger.log(Level.WARNING, "Can't delete game save file " + f);
		}
	}

	public void removeBoardSavefile(BoardView board) {
		File f = new File(ChessConfig.getBoardPersistDirectory(), board.getName() + ".yml");
		if (!f.delete()) {
			ChessCraftLogger.log(Level.WARNING, "Can't delete board save file " + f);
		}
	}
}
