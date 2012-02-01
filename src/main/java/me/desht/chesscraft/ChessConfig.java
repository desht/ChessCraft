/**
 * Programmer: Jacob Scott
 * Program Name: ChessConfig
 * Description: class for organizing configuration settings
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.exceptions.ChessException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.Duration;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

/**
 * @author jacob
 */
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
	
	public static Configuration getConfig() {
		return plugin.getConfig();
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
		createDir(new File(boardStyleDir, "custom"));
		// [plugins]/ChessCraft/piece_styles
		createDir(pieceStyleDir);
		createDir(new File(pieceStyleDir, "custom"));
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

		extractResource("/datafiles/board_styles/standard.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/open.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/sandwood.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/large.yml", boardStyleDir); //$NON-NLS-1$

		extractResource("/datafiles/piece_styles/standard.yml", pieceStyleDir); //$NON-NLS-1$
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
		
		// if the file exists and is newer than the JAR, then we'll leave it alone
		if (of.exists() && of.lastModified() > getJarFile().lastModified() && !force) {
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
	 * Find a YAML resource file in the given directory.  Look first in the custom/ subdirectory
	 * and then in the directory itself.
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 * @throws ChessException
	 */
	public static File getResourceFile(File dir, String filename) throws ChessException {
		File f = new File(dir, "custom" + File.separator + filename.toLowerCase() + ".yml");
		if (!f.canRead()) {
			f = new File(dir, filename.toLowerCase() + ".yml");
			if (!f.canRead()) {
				throw new ChessException("resource file '" + filename + "' is not readable");
			}
		}
		return f;
	}
	
	/**
	 * Load the existing config file (config.yml) and see if there are any items
	 * in configDefaults which are not in the file. If so, update the config
	 * with defaults from configDefaults (preserving existing settings) and
	 * re-write the file.
	 */
	private static void configFileInitialise() {
		Boolean saveNeeded = false;

		plugin.getConfig().options().copyDefaults(true);
		Configuration config = plugin.getConfig();

		for (String k : config.getDefaults().getKeys(true)) {
			if (!config.contains(k)) {
				saveNeeded = true;
			}
		}
		
		String currentVersion = plugin.getDescription().getVersion();
		if (currentVersion != null && !config.getString("version").equals(currentVersion)) {
			setConfigItem(null, "version", currentVersion);
			saveNeeded = true;
			versionChanged(config.getString("version"), currentVersion);
		}
		if (saveNeeded) {
			plugin.saveConfig();
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
		if (rel1 < 4000 && rel2 >= 4000 || rel1 < 5000 && rel2 >= 5000) {
			// "large" chess set definition is different in v0.4+ and again in v0.5+
			new File(pieceStyleDir, "large.yml").delete();
		}
		if (rel1 < 5000 && rel2 >= 5000) {
			// remove old upper-cased style files
			new File(pieceStyleDir, "Standard.yml").delete();
			new File(boardStyleDir, "Standard.yml").delete();
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
	public static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : plugin.getConfig().getDefaults().getKeys(true)) {
			if (plugin.getConfig().isConfigurationSection(k))
				continue;
			res.add(k + " = '" + plugin.getConfig().get(k) + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Collections.sort(res);
		return res;
	}

	public static void setConfigItem(Player player, String key, String val) {
		Configuration config = plugin.getConfig();
		Configuration configDefaults = config.getDefaults();
		
		if (!configDefaults.contains(key)) {
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
			config.set(key, bVal);
		} else if (configDefaults.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				config.set(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidInteger", val)); //$NON-NLS-1$
			}
		} else if (configDefaults.get(key) instanceof Double) {
			try {
				double nVal = Double.parseDouble(val);
				config.set(key, nVal);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidFloat", val)); //$NON-NLS-1$
			}
		} else if (configDefaults.get(key) instanceof Duration) {
			try {
				Duration d = new Duration(val);
				config.set(key, d.toString());
			} catch (IllegalArgumentException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.invalidDuration", val)); //$NON-NLS-1$
			}
		} else {
			config.set(key, val);
		}

		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) { //$NON-NLS-1$
			plugin.util.setupRepeatingTask(plugin, 0);
		} else if (key.equalsIgnoreCase("locale")) {
			try {
				Messages.loadMessages();
				// redraw control panel signs in the right language
				for (BoardView bv : BoardView.listBoardViews()) {
					bv.getControlPanel().repaint();
				}
			} catch (IOException e) {
				ChessCraftLogger.severe("Can't load messages file", e);
			}
		}

		plugin.saveConfig();
	}
} // end class ChessConfig

