/**
 * Programmer: Jacob Scott
 * Program Name: ChessConfig
 * Description: class for organizing configuration settings
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft;

import me.desht.util.ChessUtils;
import me.desht.chesscraft.chess.ChessAI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.util.Duration;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * @author jacob
 */
@SuppressWarnings("serial")
public class ChessConfig {

	private static File pluginDir = new File("plugins", "ChessCraft"); //$NON-NLS-1$ //$NON-NLS-2$
	private static File pgnDir, boardStyleDir, pieceStyleDir, schematicsDir, 
	dataDir, gamePersistDir, boardPersistDir, languagesDir, resultsDir;
	private static final String pgnFoldername = "pgn"; //$NON-NLS-1$
	private static final String boardStyleFoldername = "board_styles"; //$NON-NLS-1$
	private static final String pieceStyleFoldername = "piece_styles"; //$NON-NLS-1$
	private static final String schematicsFoldername = "schematics"; //$NON-NLS-1$
	private static final String languageFoldername = "lang"; //$NON-NLS-1$
	private static final String datasaveFoldername = "data"; //$NON-NLS-1$
	private static final String gamesFoldername = "games"; //$NON-NLS-1$
	private static final String boardsFoldername = "boards"; //$NON-NLS-1$
	private static final String resultsFoldername = "results"; //$NON-NLS-1$
	private static File persistFile;
	private static final String persistFilename = "persist.yml"; //$NON-NLS-1$
	private static ChessCraft plugin = null;
	private static final Map<String, Object> configDefaults = new HashMap<String, Object>() {

		{
			put("locale", "default");
			put("autosave", true); //$NON-NLS-1$
			put("tick_interval", 1); //$NON-NLS-1$
			put("broadcast_results", true); //$NON-NLS-1$
			put("autostart", true); //$NON-NLS-1$
			put("auto_delete.finished", new Duration("30 s")); //$NON-NLS-1$
			put("auto_delete.not_started", new Duration("3 mins")); //$NON-NLS-1$
			put("auto_delete.running", new Duration("7 days")); //$NON-NLS-1$
			put("ai.min_move_wait", 3); //$NON-NLS-1$
			put("ai.max_ai_games", 3); //$NON-NLS-1$
			put("ai.name_prefix", "[AI]"); //$NON-NLS-1$ //$NON-NLS-2$
			put("ai.use_opening_book", false); //$NON-NLS-1$
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
			put("timeout_forfeit", new Duration("1 min")); //$NON-NLS-1$
			put("stake.default", 0.0); //$NON-NLS-1$
			put("stake.smallIncrement", 1.0); //$NON-NLS-1$
			put("stake.largeIncrement", 10.0); //$NON-NLS-1$
			put("ladder.initial_pos", 1000); //$NON-NLS-1$
			put("league.win_points", 2); //$NON-NLS-1$
			put("league.draw_points", 1); //$NON-NLS-1$
			put("league.loss_points", 0); //$NON-NLS-1$
			// 0.3.3 is the last released version which didn't put a version number in the config
			put("version", "0.3.3"); //$NON-NLS-1$
		}
	};

	public static void init(ChessCraft chessplugin) {
		plugin = chessplugin;
		if (plugin != null) {
			pluginDir = plugin.getDataFolder();
		}
		
		setupDirectoryStructure();

		configFileInitialise();

		try {
			Messages.init();
		} catch (IOException e) {
			ChessCraftLogger.severe("Can't load messages file", e);
		}

		ChessAI.initAI_Names();
	}

	public static File getJarFile() {
		return new File("plugins", "ChessCraft.jar");
	}
	
	public static Configuration getConfiguration() {
		return plugin.getConfiguration();
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

	public static File getLanguagesDirectory() {
		return languagesDir;
	}

	public static File getPersistFile() {
		return persistFile;
	}
	
	public static File getResultsDir() {
		return resultsDir;
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
		resultsDir = new File(dataDir, resultsFoldername);

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
		// [plugins]/ChessCraft/data/results
		createDir(resultsDir);

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

		// message resources no longer extracted here - this is now done by Messages.loadMessages()
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
		extractResource(from, toDir, false);
	}

	static void extractResource(String from, File to, boolean force) {
		File of = to;
		if (to.isDirectory()) {
			String fname = new File(from).getName();
			of = new File(to, fname);
		} else if (!of.isFile()) {
			return;
		}
		if (of.exists() && !force) {
			return;
		}

		OutputStream out = null;
		try {
			// Got to jump through hoops to ensure we can still pull messages from a JAR
			// file after it's been reloaded...
			URL res = ChessCraft.class.getResource(from);
			if (res == null) {
				ChessCraftLogger.log(Level.WARNING, "can't find " + from + " in plugin JAR file"); //$NON-NLS-1$
				return;
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			InputStream in = resConn.getInputStream();

			if (in == null) {
				ChessCraftLogger.log(Level.WARNING, "can't get input stream from " + res); //$NON-NLS-1$
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
		} catch (Exception ex) {
			ChessCraftLogger.log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception ex) { //IOException
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
				setConfigItem(null, k, configDefaults.get(k).toString());
			}
		}
		String currentVersion = plugin.getDescription().getVersion();
		if (currentVersion != null && !config.getString("version").equals(currentVersion)) {
			setConfigItem(null, "version", currentVersion);
			saveNeeded = true;
			versionChanged(config.getString("version"), currentVersion);
		}
		if (saveNeeded) {
			config.save();
		}
	}

	/**
	 * Things to do if the version has changed since the last time we ran.
	 * 
	 * @param oldVersion		The previous version
	 * @param currentVersion	The current version
	 */
	private static void versionChanged(String oldVersion, String currentVersion) {
		int rel1 = getRelease(oldVersion);
		int rel2 = getRelease(currentVersion);
		if (rel1 < 4000 && rel2 >= 4000) {
			// "large" chess set definition is different in v0.4+ - block rotation is handled
			// by ChessCraft, not the definition file.  Force re-extraction.
			new File(pieceStyleDir, "large.yml").delete();
		}
	}

	/**
	 * Get the internal version number for the given string version, which is
	 * <major> * 1,000,000 + <minor> * 1,000 + <release>.  This assumes minor and
	 * release each won't go above 999, hopefully a safe assumption!
	 * 
	 * @param oldVersion
	 * @return
	 */
	private static int getRelease(String ver) {
		String[] a = ver.split("\\.");
		try {
			int major = Integer.parseInt(a[0]);
			int minor;
			int rel;
			if (a.length < 2) {
				minor = 0;
			} else {
				minor = Integer.parseInt(a[1]);
			}
			if (a.length < 3) {
				rel = 0;
			} else {
				rel = Integer.parseInt(a[2]);
			}
			return major * 1000000 + minor * 1000 + rel;
		} catch (NumberFormatException e) {
			ChessCraftLogger.warning("Version string [" + ver + "] doesn't look right!");
			return 0;
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

		if (!configDefaults.containsKey(key)) {
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
		} else if (configDefaults.get(key) instanceof Duration) {
			try {
				Duration d = new Duration(val);
				config.setProperty(key, d.toString());
			} catch (IllegalArgumentException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidDuration", val)); //$NON-NLS-1$
			}
		} else {
			config.setProperty(key, val);
		}

		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) { //$NON-NLS-1$
			plugin.util.setupRepeatingTask(plugin, 0);
		} else if (key.equalsIgnoreCase("locale")) {
			try {
				Messages.loadMessages();
			} catch (IOException e) {
				ChessCraftLogger.severe("Can't load messages file", e);
			}
		}

		config.save();
	}
} // end class ChessConfig

