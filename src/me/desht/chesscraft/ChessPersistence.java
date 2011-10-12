package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import me.desht.chesscraft.enums.BoardOrientation;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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
		for (ChessGame game : ChessGame.listGames()) {
			game.deleteTemporary();
		}
		for (BoardView view : BoardView.listBoardViews()) {
			view.deleteTemporary();
		}

		loadPersistedData();
	}

	public static List<Object> freezeLocation(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	public static Location thawLocation(List<Object> list) {
		World w = ChessPersistence.findWorld((String) list.get(0));
		return new Location(w,
		                    (Integer) list.get(1),
		                    (Integer) list.get(2),
		                    (Integer) list.get(3));
	}

	private static World findWorld(String worldName) {
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

		YamlConfiguration conf = new YamlConfiguration();
		for (Entry<String,String> e : ChessGame.getCurrentGames().entrySet()) {
			conf.set("current_games." + e.getKey(), e.getValue());
		}
		try {
			conf.save(ChessConfig.getPersistFile());
		} catch (IOException e1) {
			ChessCraftLogger.severe("Can't save persist.yml", e1);
		}
	}

	private void loadPersistedData() {
		if (!loadOldPersistedData()) {
			// load v0.3 or later saved data - they are in a subdirectory, one
			// file per entry
			int nLoadedBoards = loadBoards();
			int nLoadedGames = loadGames();

			for (BoardView bv : BoardView.listBoardViews()) {
				bv.getControlPanel().repaintSignButtons();
			}

			ChessCraftLogger.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games.");

			try {
				YamlConfiguration conf = YamlConfiguration.loadConfiguration(ChessConfig.getPersistFile());
				ConfigurationSection current = conf.getConfigurationSection("current_games");
				if (current != null) {
					for (String player : current.getKeys(false)) {
						try {
							ChessGame.setCurrentGame(player, current.getString(player));
						} catch (ChessException e) {
							ChessCraftLogger.log(Level.WARNING, "can't set current game for player " + player + ": "
							                     + e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Unexpected Error while loading " + ChessConfig.getPersistFile().getName());
				moveBackup(ChessConfig.getPersistFile());
			}
		} else {
			savePersistedData();
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
				YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

				int nWantedBoards = 0, nLoadedBoards = 0;
				int nWantedGames = 0, nLoadedGames = 0;

				ConfigurationSection boards = conf.getConfigurationSection("boards");
				if (boards != null) {
					Set<String> keys = boards.getKeys(false);
					nWantedBoards = keys.size();

					/**
					 * v0.2 or earlier saved boards - all in the main
					 * persist.yml file
					 * 
					 * @param boardList
					 *            a list of the boards
					 * @return the number of games that were sucessfully loaded
					 */
					for (String boardName : keys) {
						ConfigurationSection boardMap = boards.getConfigurationSection(boardName);
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

				ConfigurationSection games = conf.getConfigurationSection("games");
				if (games != null) {
					Set<String> keys = games.getKeys(false);
					nWantedGames = keys.size();
					/**
					 * v0.2 or earlier saved games - all in the main persist.yml
					 * file
					 * 
					 * @param gameList
					 *            a list of the map objects for each game
					 * @return the number of games that were sucessfully loaded
					 */
					for (String gameName : keys) {
						ConfigurationSection gameMap = boards.getConfigurationSection(gameName);
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

				ConfigurationSection current = conf.getConfigurationSection("current_games");
				if (current != null) {
					for (String player : current.getKeys(false)) {
						try {
							ChessGame.setCurrentGame(player, current.getString(player));
						} catch (ChessException e) {
							ChessCraftLogger.log(Level.WARNING, "can't set current game for player " + player + ": "
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

	private int loadBoards() {
		int nLoaded = 0;
		for (File f : ChessConfig.getBoardPersistDirectory().listFiles(ymlFilter)) {
			try {
				YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

				String bvName = conf.getString("name");
				@SuppressWarnings("unchecked")
				List<Object> origin = (List<Object>) conf.getList("origin");
				World w = findWorld((String) origin.get(0));
				Location originLoc = new Location(w, (Integer) origin.get(1), (Integer) origin.get(2),
				                                  (Integer) origin.get(3));
				BoardView.addBoardView(new BoardView(bvName, plugin, originLoc,
				                                     BoardOrientation.get(conf.getString("direction")),
				                                     conf.getString("boardStyle"), conf.getString("pieceStyle")));
				++nLoaded;
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Error loading " + f.getName() + ": " + e.getMessage());//, e);
				// TODO: restore terrain, if applicable?
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
		YamlConfiguration conf = new YamlConfiguration();
		Map<String, Object> map = board.freeze();
		for (Entry<String, Object> e : map.entrySet()) {
			conf.set(e.getKey(), e.getValue());
		}
		File file = new File(ChessConfig.getBoardPersistDirectory(), safeFileName(board.getName()) + ".yml");
		try {
			conf.save(file);
		} catch (IOException e1) {
			ChessCraftLogger.severe("Can't save board " + board.getName(), e1);
		}
	}

	/**
	 * v0.3 or later saved games - one save file per game in their own
	 * subdirectory
	 * 
	 * @return the number of games that were sucessfully loaded
	 */
	private int loadGames() {
		HashMap<String, ConfigurationSection> toLoad = new HashMap<String, ConfigurationSection>();
		// game validation checks
		for (File f : ChessConfig.getGamesPersistDirectory().listFiles(ymlFilter)) {
			try {
				YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
				conf.set("filename", f.getName());
				String board = conf.getString("boardview");
				if (board == null) {
					ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + f.getName() + ": boardview is null");
				} else if (toLoad.containsKey(board)) {
					// only load the newer game
					long tstart = conf.getInt("started", 0);
					long ostart = Long.parseLong(toLoad.get(board).get("started").toString());
					if (ostart >= tstart) {
						ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + f.getName() + ": another game is using the same board");
						moveBackup(f);
					} else {
						String fn = (String) toLoad.get(board).get("filename");
						ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + fn + ": another game is using the same board");
						(new File(f.getParentFile(), fn)).renameTo(new File(f.getParentFile(), fn + ".bak"));
						toLoad.put(board, conf);
					}
				} else {
					toLoad.put(board, conf);
				}
			} catch (Exception e) {
				ChessCraftLogger.log(Level.SEVERE, "Error loading " + f.getName(), e);
			}
		}
		// now actually load games
		int nLoaded = 0;
		for (ConfigurationSection gameMap : toLoad.values()) {
			if (loadGame(gameMap)) {
				++nLoaded;
			} else {
				moveBackup(new File(ChessConfig.getGamesPersistDirectory(), (String) gameMap.get("filename")));
			}
		}
		return nLoaded;
	}

	private boolean loadGame(ConfigurationSection conf) {
		String gameName = conf.getString("name");
		try {
			ChessGame game = new ChessGame(plugin, conf);
			ChessGame.addGame(gameName, game);
			return true;
		} catch (Exception e) {
			ChessCraftLogger.log(Level.SEVERE, "can't load saved game " + gameName + ": " + e.getMessage(), e);
		}
		return false;
	}

	protected void saveGames() {
		for (ChessGame game : ChessGame.listGames()) {
			saveGame(game);
		}
	}

	public void saveGame(ChessGame game) {
		YamlConfiguration conf = new YamlConfiguration();
		Map<String, Object> map = game.freeze();
		for (Entry<String, Object> e : map.entrySet()) {
			conf.set(e.getKey(), e.getValue());
		}
		File file = new File(ChessConfig.getGamesPersistDirectory(), safeFileName(game.getName()) + ".yml");
		try {
			conf.save(file);
		} catch (IOException e1) {
			ChessCraftLogger.severe("Can't save game " + game.getName(), e1);
		}
	}

	protected static String safeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}

	public void removeGameSavefile(ChessGame game) {
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
