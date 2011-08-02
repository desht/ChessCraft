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
import me.desht.chesscraft.log.ChessCraftLogger;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * @author jacob
 */
@SuppressWarnings("serial")
public class ChessConfig {

	private static File pluginDir = new File("plugins", "ChessCraft"); //$NON-NLS-1$ //$NON-NLS-2$
	private static File pgnDir, boardStyleDir, pieceStyleDir, schematicsDir, 
			dataDir, gamePersistDir, boardPersistDir, languagesDir;
	private static final String pgnFoldername = "pgn"; //$NON-NLS-1$
	private static final String boardStyleFoldername = "board_styles"; //$NON-NLS-1$
	private static final String pieceStyleFoldername = "piece_styles"; //$NON-NLS-1$
	private static final String schematicsFoldername = "schematics"; //$NON-NLS-1$
	private static final String languageFoldername = "lang";
	private static final String datasaveFoldername = "data"; //$NON-NLS-1$
	private static final String gamesFoldername = "games"; //$NON-NLS-1$
	private static final String boardsFoldername = "boards"; //$NON-NLS-1$
	private static File persistFile;
	private static final String persistFilename = "persist.yml"; //$NON-NLS-1$
	private static ChessCraft plugin = null;
	private static final Map<String, Object> configDefaults = new HashMap<String, Object>() {

		{
			put("languagefile", "en_us");
			put("autosave", true); //$NON-NLS-1$
			put("tick_interval", 1); //$NON-NLS-1$
			put("broadcast_results", true); //$NON-NLS-1$
			put("auto_delete.finished", 30); //$NON-NLS-1$
			put("auto_delete.not_started", 180); //$NON-NLS-1$
			put("ai.min_move_wait", 3); //$NON-NLS-1$
			put("ai.max_ai_games", 3); //$NON-NLS-1$
			put("ai.name_prefix", "[AI]"); //$NON-NLS-1$ //$NON-NLS-2$
			put("no_building", true); //$NON-NLS-1$
			put("no_creatures", true); //$NON-NLS-1$
			put("no_explosions", true); //$NON-NLS-1$
			put("no_burning", true); //$NON-NLS-1$
			put("no_pvp", true); //$NON-NLS-1$
			put("no_monster_attacks", true); //$NON-NLS-1$
			put("no_misc_damage", true); //$NON-NLS-1$
			put("wand_item", "air"); //$NON-NLS-1$ //$NON-NLS-2$
			put("auto_teleport_on_join", true); //$NON-NLS-1$
			put("highlight_last_move", true); //$NON-NLS-1$
			put("timeout_forfeit", 60); //$NON-NLS-1$
			put("stake.default", 0.0); //$NON-NLS-1$
			put("stake.smallIncrement", 1.0); //$NON-NLS-1$
			put("stake.largeIncrement", 10.0); //$NON-NLS-1$
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

		Messages.load(new File(languagesDir, 
				plugin.getConfiguration().getString("languagefile", "en_us") + ".yml"));

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
		languagesDir = new File(pluginDir, languageFoldername);

		// files
		persistFile = new File(dataDir, persistFilename);

		// [plugins]/ChessCraft
		createDir(pluginDir);
		// [plugins]/ChessCraft/pgn
		createDir(pgnDir);
		// [plugins]/ChessCraft/lang
		createDir(languagesDir);
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
		File oldSchematicsDir = new File(pluginDir, "schematics"); //$NON-NLS-1$
		if (oldSchematicsDir.isDirectory()) {
			if (!oldSchematicsDir.renameTo(schematicsDir)) {
				ChessCraftLogger.log(Level.WARNING, "Can't move " + oldSchematicsDir + " to " + schematicsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// [plugins]/ChessCraft/data/boards/schematics
			createDir(schematicsDir);
		}

		extractResource("/AI_settings.yml", pluginDir); //$NON-NLS-1$

		extractResource("/datafiles/board_styles/Standard.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/open.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/sandwood.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/large.yml", boardStyleDir); //$NON-NLS-1$

		extractResource("/datafiles/piece_styles/Standard.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/twist.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/sandwood.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/large.yml", pieceStyleDir); //$NON-NLS-1$
		
		extractResource("/datafiles/lang/en_us.yml", languagesDir);
	}

	private static void createDir(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			ChessCraftLogger.log(Level.WARNING, "Can't make directory " + dir.getName()); //$NON-NLS-1$
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
				ChessCraftLogger.log(Level.WARNING, "can't extract resource " + from + " from plugin JAR"); //$NON-NLS-1$ //$NON-NLS-2$
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
			ChessCraftLogger.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			ChessCraftLogger.log(Level.SEVERE, null, ex);
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
			res.add(k + " = '" + plugin.getConfiguration().getString(k) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Collections.sort(res);
		return res;
	}

	static void setConfigItem(Player player, String key, String val) {
		Configuration config = plugin.getConfiguration();

		if (configDefaults.get(key) == null) {
			ChessUtils.errorMessage(player, Messages.getString("ChessConfig.noSuchKey", key)); //$NON-NLS-1$
			ChessUtils.errorMessage(player, "Use '/chess getcfg' to list all valid keys"); //$NON-NLS-1$
			return;
		}
		if (configDefaults.get(key) instanceof Boolean) {
			Boolean bVal = false;
			if (val.equals("false") || val.equals("no")) { //$NON-NLS-1$ //$NON-NLS-2$
				bVal = false;
			} else if (val.equals("true") || val.equals("yes")) { //$NON-NLS-1$ //$NON-NLS-2$
				bVal = true;
			} else {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidBoolean", val)); //$NON-NLS-1$ 
				return;
			}
			config.setProperty(key, bVal);
		} else if (configDefaults.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				config.setProperty(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidInteger", val)); //$NON-NLS-1$
			}
		} else if (configDefaults.get(key) instanceof Double) {
			try {
				double nVal = Double.parseDouble(val);
				config.setProperty(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidFloat", val)); //$NON-NLS-1$
			}
		} else {
			config.setProperty(key, val);
		}

		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) { //$NON-NLS-1$
			plugin.util.setupRepeatingTask(0);
		}

		config.save();
	}
} // end class ChessConfig

