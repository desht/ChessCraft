package me.desht.chesscraft;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

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
			conf.save(DirectoryStructure.getPersistFile());
		} catch (IOException e1) {
			LogUtils.severe("Can't save persist.yml", e1);
		}
	}

	private void loadPersistedData() {
		int nLoaded = 0;
		
		// load the boards, and any games on those boards
		for (File f : DirectoryStructure.getBoardPersistDirectory().listFiles(ymlFilter)) {
			nLoaded += loadBoard(f) ? 1 : 0;
		}
		
		for (BoardView bv : BoardView.listBoardViews()) {
			bv.getControlPanel().repaintSignButtons();
		}

		LogUtils.fine("loaded " + nLoaded + " saved boards.");

		// load other misc data which isn't tied to any board or game
		try {
			YamlConfiguration conf = YamlConfiguration.loadConfiguration(DirectoryStructure.getPersistFile());
			ConfigurationSection current = conf.getConfigurationSection("current_games");
			if (current != null) {
				for (String player : current.getKeys(false)) {
					try {
						ChessGame.setCurrentGame(player, current.getString(player));
					} catch (ChessException e) {
						LogUtils.warning("can't set current game for player " + player + ": "
								+ e.getMessage());
					}
				}
			}
		} catch (Exception e) {
			LogUtils.severe("Unexpected Error while loading " + DirectoryStructure.getPersistFile().getName());
		}
	}

	/**
	 * Load one board file, plus the game on that board, if there is one.
	 * 
	 * @param f
	 * @return
	 */
	public boolean loadBoard(File f) {
		LogUtils.fine("loading board: " + f);
		try {
			Configuration conf = YamlConfiguration.loadConfiguration(f);
			
			BoardView bv;
			if (conf.contains("board")) {
				bv = (BoardView) conf.get("board");
			} else if (conf.getKeys(false).size() > 0) {
				bv = new BoardView(conf);
				savePersistable("board", bv);
				LogUtils.info("migrated v4-format board save " + f.getName() + " to v5-format");
			} else {
				// empty config returned - probably due to corrupted save file of some kind
				return false;
			}
			if (bv.getChessBoard() != null) {
				BoardView.registerView(bv);
				// load the board's game too, if there is one
				if (!bv.getSavedGameName().isEmpty()) {
					File gameFile = new File(DirectoryStructure.getGamesPersistDirectory(), bv.getSavedGameName() + ".yml");
					loadGame(gameFile);
				}
			} else {
				BoardView.deferLoading(bv.getWorldName(), f);
				LogUtils.info("board loading for board '" + bv.getName() + "' deferred (world '" + bv.getWorldName() + "' not available)");
			}
			return true;
		} catch (Exception e) {
			LogUtils.severe("can't load saved board from " + f.getName() + ": " + e.getMessage(), e);
			// TODO: restore terrain, if applicable?
			return false;
		}
	}

	private boolean loadGame(File f) {
		LogUtils.fine("loading game: " + f);
		try {
			Configuration conf = YamlConfiguration.loadConfiguration(f);
			ChessGame game = null;
			if (conf.contains("game")) {
				game = (ChessGame) conf.get("game");
			} else if (conf.getKeys(false).size() > 0) {
				game = new ChessGame(conf);
				savePersistable("game", game);
				LogUtils.info("migrated v4-format game save " + f.getName() + " to v5-format");
			}
			if (game != null) {
				ChessGame.addGame(game.getName(), game);
			}
			return game != null;
		} catch (Exception e) {
			LogUtils.severe("can't load saved game from " + f.getName() + ": " + e.getMessage(), e);
			return false;
		}
	}

	private void saveBoards() {
		for (BoardView b : BoardView.listBoardViews()) {
			savePersistable("board", b);
		}
	}

	private void saveGames() {
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
			LogUtils.severe("Can't save " + tag + " " + object.getName(), e1);
		}
	}
	
	public void unpersist(ChessPersistable object) {
		File f = new File(object.getSaveDirectory(), makeSafeFileName(object.getName()) + ".yml");
		if (!f.delete()) {
			LogUtils.warning("Can't delete save file " + f);
		}
	}

	
	public static void requireSection(Configuration c, String key) throws ChessException {
		if (!c.contains(key))
			throw new ChessException("missing required section '" + key + "'");
	}
	
	public static String makeSafeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}
}
