package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public class ChessPersistence {

	public void save() {
		savePersistedData();
	}

	public void reload() {
		List<BoardView> views = new ArrayList<BoardView>(BoardViewManager.getManager().listBoardViews());
		for (BoardView view : views) {
			// this will also do a temporary delete on any games
			BoardViewManager.getManager().deleteBoardView(view.getName(), false);
		}

		loadPersistedData();
		ChessGameManager.getManager().checkForUUIDMigration();
	}

	public static List<Object> freezeLocation(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	public static Location thawLocation(List<?> list) {
		World w = Bukkit.getServer().getWorld((String) list.get(0));
		return w == null ? null : new Location(w, (Integer) list.get(1), (Integer) list.get(2), (Integer) list.get(3));
	}

	private void savePersistedData() {
		saveGames();
		saveBoards();

		YamlConfiguration conf = new YamlConfiguration();
		for (Entry<UUID,String> e : ChessGameManager.getManager().getCurrentGames().entrySet()) {
			conf.set("current_games." + e.getKey(), e.getValue());
		}

		Location loc = BoardViewManager.getManager().getGlobalTeleportOutDest();
		if (loc != null) {
			conf.set("teleport_out_dest", new PersistableLocation(loc));
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
		for (File f : DirectoryStructure.getBoardPersistDirectory().listFiles(DirectoryStructure.ymlFilter)) {
			nLoaded += loadBoard(f) ? 1 : 0;
		}

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			Debugger.getInstance().debug(2, "repainting controls for board " + bv.getName());
			bv.getControlPanel().repaintControls();
		}

		Debugger.getInstance().debug("loaded " + nLoaded + " saved boards.");

		// load other misc data which isn't tied to any board or game
		File persistFile = DirectoryStructure.getPersistFile();
		if (persistFile.exists()) {
			try {
				YamlConfiguration conf = MiscUtil.loadYamlUTF8(DirectoryStructure.getPersistFile());
				ConfigurationSection current = conf.getConfigurationSection("current_games");
				if (current != null) {
					for (String playerId : current.getKeys(false)) {
						try {
							ChessGameManager.getManager().setCurrentGame(UUID.fromString(playerId), current.getString(playerId));
						} catch (ChessException e) {
							LogUtils.warning("can't set current game for player " + playerId + ": " + e.getMessage());
						}
					}
				}
				if (conf.contains("teleport_out_dest")) {
					PersistableLocation pLoc = (PersistableLocation) conf.get("teleport_out_dest");
					BoardViewManager.getManager().setGlobalTeleportOutDest(pLoc.getLocation());
				}
			} catch (Exception e) {
				LogUtils.severe("Unexpected Error while loading " + DirectoryStructure.getPersistFile().getName());
				LogUtils.severe("Message: " + e.getMessage());
			}
		}
	}

	/**
	 * Load one board file, plus the game on that board, if there is one.
	 *
	 * @param f the file to load from
	 * @return true if the board was loaded, false otherwise
	 */
	public boolean loadBoard(File f) {
		Debugger.getInstance().debug("loading board: " + f);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(f);

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
			if (bv.isWorldAvailable()) {
				BoardViewManager.getManager().registerView(bv);
				// load the board's game too, if there is one
				if (!bv.getSavedGameName().isEmpty()) {
					File gameFile = new File(DirectoryStructure.getGamesPersistDirectory(), bv.getSavedGameName() + ".yml");
					loadGame(gameFile);
				}
				return true;
			} else {
				BoardViewManager.getManager().deferLoading(bv.getWorldName(), f);
				LogUtils.info("board loading for board '" + bv.getName() + "' deferred (world '" + bv.getWorldName() + "' not available)");
				return false;
			}
		} catch (Exception e) {
			LogUtils.severe("can't load saved board from " + f.getName() + ": " + e.getMessage(), e);
			// TODO: restore terrain, if applicable?
			return false;
		}
	}

	private boolean loadGame(File f) {
		Debugger.getInstance().debug("loading game: " + f);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(f);
			ChessGame game = null;
			if (conf.contains("game")) {
				game = (ChessGame) conf.get("game");
			} else if (conf.getKeys(false).size() > 0) {
				game = new ChessGame(conf);
				savePersistable("game", game);
				LogUtils.info("migrated v4-format game save " + f.getName() + " to v5-format");
			}
			if (game != null) {
				ChessGameManager.getManager().registerGame(game);
			}
			return game != null;
		} catch (Exception e) {
			LogUtils.severe("can't load saved game from " + f.getName() + ": " + e.getMessage(), e);
			return false;
		}
	}

	private void saveBoards() {
		for (BoardView b : BoardViewManager.getManager().listBoardViews()) {
			savePersistable("board", b);
		}
	}

	private void saveGames() {
		for (ChessGame game : ChessGameManager.getManager().listGames()) {
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

	public static void requireSection(ConfigurationSection c, String key) throws ChessException {
		if (!c.contains(key))
			throw new ChessException("missing required section '" + key + "'");
	}

	public static String makeSafeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}
}
