/**
 * Programmer: Jacob Scott
 * Program Name: ChessConfig
 * Description: class for organizing configuration settings
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * @author jacob
 */
@SuppressWarnings("serial")
public class ChessConfig {

	private static File pluginDir = new File("plugins", "ChessCraft");
	private static File pgnDir, boardStyleDir, pieceStyleDir, schematicsDir, dataDir, gamePersistDir, boardPersistDir;
	private static final String pgnFoldername = "pgn";
	private static final String boardStyleFoldername = "board_styles";
	private static final String pieceStyleFoldername = "piece_styles";
	private static final String schematicsFoldername = "schematics";
	private static final String datasaveFoldername = "data";
	private static final String gamesFoldername = "games";
	private static final String boardsFoldername = "boards";
	private static File persistFile;
	private static final String persistFilename = "persist.yml";
	private static ChessCraft plugin = null;
	private static final Map<String, Object> configDefaults = new HashMap<String, Object>() {

		{
			put("autosave", true);
			put("tick_interval", 1);
			put("broadcast_results", true);
			put("auto_delete.finished", 30);
			put("auto_delete.not_started", 180);
			put("ai.min_move_wait", 3);
			put("ai.max_ai_games", 3);
			put("ai.name_prefix", "[AI]");
			put("no_building", true);
			put("no_creatures", true);
			put("no_explosions", true);
			put("no_burning", true);
			put("no_pvp", true);
			put("no_monster_attacks", true);
			put("no_misc_damage", true);
			put("wand_item", "air");
			put("auto_teleport_on_join", true);
			put("highlight_last_move", true);
			put("timeout_forfeit", 60);
			put("stake.default", 0.0);
			put("stake.smallIncrement", 1.0);
			put("stake.largeIncrement", 10.0);
		}
	};

	public static void init(ChessCraft chessplugin) {
		plugin = chessplugin;
		if (plugin != null) {
			pluginDir = plugin.getDataFolder();
		}

		setupDirectoryStructure();

		configFileInitialise();

		ChessAI.initAI_Names();

	}

	public static File getPluginDirectory() {
		return pluginDir;
	}

	public static File getPGNDirectory() {
		return pgnDir;
	}

	public static File getBoardStyleDirectory() {
		return boardStyleDir;
	}

	public static File getPieceStyleDirectory() {
		return pieceStyleDir;
	}

	public static File getSchematicsDirectory() {
		return schematicsDir;
	}

	public static File getGamesPersistDirectory() {
		return gamePersistDir;
	}

	public static File getBoardPersistDirectory() {
		return boardPersistDir;
	}

	public static File getPersistFile() {
		return persistFile;
	}

	private static void setupDirectoryStructure() {
		// directories
		pgnDir = new File(pluginDir, pgnFoldername);
		boardStyleDir = new File(pluginDir, boardStyleFoldername);
		pieceStyleDir = new File(pluginDir, pieceStyleFoldername);
		dataDir = new File(pluginDir, datasaveFoldername);
		gamePersistDir = new File(dataDir, gamesFoldername);
		boardPersistDir = new File(dataDir, boardsFoldername);
		schematicsDir = new File(boardPersistDir, schematicsFoldername);

		// files
		persistFile = new File(dataDir, persistFilename);

		// [plugins]/ChessCraft
		createDir(pluginDir);
		// [plugins]/ChessCraft/pgn
		createDir(pgnDir);
		// [plugins]/ChessCraft/board_styles
		createDir(boardStyleDir);
		// [plugins]/ChessCraft/piece_styles
		createDir(pieceStyleDir);
		// [plugins]/ChessCraft/data
		createDir(dataDir);
		// [plugins]/ChessCraft/data/games
		createDir(gamePersistDir);
		// [plugins]/ChessCraft/data/boards
		createDir(boardPersistDir);

		// saved board schematics may need to be moved into their new location
		File oldSchematicsDir = new File(pluginDir, "schematics");
		if (oldSchematicsDir.isDirectory()) {
			if (!oldSchematicsDir.renameTo(schematicsDir)) {
				ChessCraft.log(Level.WARNING, "Can't move " + oldSchematicsDir + " to " + schematicsDir);
			}
		} else {
			// [plugins]/ChessCraft/data/boards/schematics
			createDir(schematicsDir);
		}

		extractResource("/AI_settings.yml", pluginDir);

		extractResource("/datafiles/board_styles/Standard.yml", boardStyleDir);
		extractResource("/datafiles/board_styles/open.yml", boardStyleDir);
		extractResource("/datafiles/board_styles/sandwood.yml", boardStyleDir);
		extractResource("/datafiles/board_styles/large.yml", boardStyleDir);

		extractResource("/datafiles/piece_styles/Standard.yml", pieceStyleDir);
		extractResource("/datafiles/piece_styles/twist.yml", pieceStyleDir);
		extractResource("/datafiles/piece_styles/sandwood.yml", pieceStyleDir);
		extractResource("/datafiles/piece_styles/large.yml", pieceStyleDir);
	}

	private static void createDir(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			ChessCraft.log(Level.WARNING, "Can't make directory " + dir.getName());
		}
	}

	private static void extractResource(String from, File toDir) {
		String fname = new File(from).getName();
		File of = new File(toDir, fname);
		if (of.exists()) {
			return;
		}
		OutputStream out = null;
		try {
			InputStream in = ChessCraft.class.getResourceAsStream(from);
			if (in == null) {
				ChessCraft.log(Level.WARNING, "can't extract resource " + from + " from plugin JAR");
			} else {
				out = new FileOutputStream(of);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		} catch (FileNotFoundException ex) {
			ChessCraft.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			ChessCraft.log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ChessCraft.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Load the existing config file (config.yml) and see if there are any items
	 * in configDefaults which are not in the file. If so, update the config
	 * with defaults from configDefaults (preserving existing settings) and
	 * re-write the file.
	 */
	private static void configFileInitialise() {
		Boolean saveNeeded = false;
		Configuration config = plugin.getConfiguration();
		for (String k : configDefaults.keySet()) {
			if (config.getProperty(k) == null) {
				saveNeeded = true;
				config.setProperty(k, configDefaults.get(k));
			}
		}
		if (saveNeeded) {
			config.save();
		}
	}

	/**
	 * @return a sorted list of all config keys
	 */
	static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configDefaults.keySet()) {
			res.add(k + " = '" + plugin.getConfiguration().getString(k) + "'");
		}
		Collections.sort(res);
		return res;
	}

	static void setConfigItem(Player player, String key, String val) {
		Configuration config = plugin.getConfiguration();

		if (configDefaults.get(key) == null) {
			ChessUtils.errorMessage(player, "No such config key: " + key);
			ChessUtils.errorMessage(player, "Use '/chess getcfg' to list all valid keys");
			return;
		}
		if (configDefaults.get(key) instanceof Boolean) {
			Boolean bVal = false;
			if (val.equals("false") || val.equals("no")) {
				bVal = false;
			} else if (val.equals("true") || val.equals("yes")) {
				bVal = true;
			} else {
				ChessUtils.errorMessage(player, "Invalid boolean value " + val + " - use true/yes or false/no.");
				return;
			}
			config.setProperty(key, bVal);
		} else if (configDefaults.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				config.setProperty(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, "Invalid numeric value: " + val);
			}
		} else if (configDefaults.get(key) instanceof Double) {
			try {
				double nVal = Double.parseDouble(val);
				config.setProperty(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, "Invalid numeric value: " + val);
			}
		} else {
			config.setProperty(key, val);
		}

		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) {
			plugin.util.setupRepeatingTask(0);
		}

		config.save();
	}
} // end class ChessConfig

