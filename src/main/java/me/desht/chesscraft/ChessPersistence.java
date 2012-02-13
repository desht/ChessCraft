package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ChessPersistence {

	private FilenameFilter ymlFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".yml");
		}
	};

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
		int nLoadedBoards = loadBoards();
		int nLoadedGames = loadGames();

		for (BoardView bv : BoardView.listBoardViews()) {
			bv.getControlPanel().repaintSignButtons();
		}

		ChessCraftLogger.log(Level.INFO, "loaded " + nLoadedBoards + " saved boards and " + nLoadedGames + " saved games.");

		// load other misc data which isn't tied to any board or game
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
	}

	private int loadBoards() {
		int nLoaded = 0;
		for (File f : ChessConfig.getBoardPersistDirectory().listFiles(ymlFilter)) {
			if (loadBoard(f)) {
				++nLoaded;
			} else {
				moveBackup(new File(ChessConfig.getBoardPersistDirectory(), f.getName()));
			}
		}
		return nLoaded;
	}
	
	private boolean loadBoard(File f) {
		try {
			Configuration conf = YamlConfiguration.loadConfiguration(f);
			BoardView bv;
			if (conf.contains("board")) {
				bv = (BoardView) conf.get("board");
			} else {
				bv = new BoardView(conf);
				savePersistable("board", bv);
				ChessCraftLogger.info("migrated v4-format board save " + f.getName() + " to v5-format");
			}
			BoardView.addBoardView(bv);
			return true;
		} catch (Exception e) {
			ChessCraftLogger.log(Level.SEVERE, "can't load saved board from " + f.getName() + ": " + e.getMessage(), e);
			// TODO: restore terrain, if applicable?
			return false;
		}
	}

	protected void saveBoards() {
		for (BoardView b : BoardView.listBoardViews()) {
			savePersistable("board", b);
		}
	}

	/**
	 * v0.3 or later saved games - one save file per game in their own
	 * subdirectory
	 * 
	 * @return the number of games that were sucessfully loaded
	 */
	private int loadGames() {
		// TODO: validation - in particular ensure boards aren't used by multiple games
		int nLoaded = 0;
		for (File f : ChessConfig.getGamesPersistDirectory().listFiles(ymlFilter)) {
			if (loadGame(f)) {
				++nLoaded;
			} else {
				moveBackup(new File(ChessConfig.getGamesPersistDirectory(), f.getName()));
			}
		}
		return nLoaded;
	}

	private boolean loadGame(File f) {
		try {
			Configuration conf = YamlConfiguration.loadConfiguration(f);
			ChessGame game = null;
			if (conf.contains("game")) {
				game = (ChessGame) conf.get("game");
			} else if (conf.getKeys(false).size() > 0) {
				game = new ChessGame(conf);
				savePersistable("game", game);
				ChessCraftLogger.info("migrated v4-format game save " + f.getName() + " to v5-format");
			}
			if (game != null) {
				ChessGame.addGame(game.getName(), game);
			}
			return game != null;
		} catch (Exception e) {
			ChessCraftLogger.log(Level.SEVERE, "can't load saved game from " + f.getName() + ": " + e.getMessage(), e);
			return false;
		}
	}

	protected void saveGames() {
		for (ChessGame game : ChessGame.listGames()) {
			savePersistable("game", game);
		}
	}

	public void savePersistable(String tag, ChessPersistable object) {
		YamlConfiguration conf = new YamlConfiguration();
		conf.set(tag, object);
		File file = new File(object.getSaveDirectory(), makeSafeFileName(object.getName()) + ".yml");
		try {
			conf.save(file);
		} catch (IOException e1) {
			ChessCraftLogger.severe("Can't save " + tag + " " + object.getName(), e1);
		}
	}
	
	public void unpersist(ChessPersistable object) {
		File f = new File(object.getSaveDirectory(), object.getName() + ".yml");
		if (!f.delete()) {
			ChessCraftLogger.log(Level.WARNING, "Can't delete save file " + f);
		}
	}

	/**
	 * move to a backup & delete original <br>
	 * (for if the file is considered corrupt)
	 * 
	 * @param original
	 *            file to backup
	 */
	private static void moveBackup(File original) {
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
	
	public static void requireSection(Configuration c, String key) throws ChessException {
		if (!c.contains(key))
			throw new ChessException("piece style file is missing required section '" + key + "'");
	}
	
	public static String makeSafeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}
}
