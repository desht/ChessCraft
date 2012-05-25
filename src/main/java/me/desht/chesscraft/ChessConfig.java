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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import me.desht.dhutils.LogUtils;

import org.bukkit.configuration.Configuration;

/**
 * @author jacob
 */
public class ChessConfig {

	private static ChessCraft plugin;

	public static void init() {
		ChessConfig.plugin = ChessCraft.getInstance();

		configFileInitialise();

		LogUtils.setLogLevel(getConfig().getString("log_level"));

		Messages.init(getConfig().getString("locale"));

		ChessAI.initAINames();
	}

	public static Configuration getConfig() {
		return plugin.getConfig();
	}

	/**
	 * Load the existing config file (config.yml) and see if there are any items
	 * in configDefaults which are not in the file. If so, update the config
	 * with defaults from configDefaults (preserving existing settings) and
	 * re-write the file.
	 */
	private static void configFileInitialise() {
		plugin.getConfig().options().copyDefaults(true);
		Configuration config = plugin.getConfig();

		String currentVersion = plugin.getDescription().getVersion();
		if (currentVersion != null && !config.getString("version").equals(currentVersion)) {
			versionChanged(config.getString("version"), currentVersion);
			try {
				setConfigItem(config, "version", currentVersion);
			} catch (ChessException e) {
				// shouldn't ever get here...
				LogUtils.severe("Can't update version in configuration file", e);
			}
		}

		plugin.saveConfig();
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
		if (rel1 < 5000 && rel2 >= 5000) {
			// remove old upper-cased style files
			new File(DirectoryStructure.getPieceStyleDirectory(), "Standard.yml").delete();
			new File(DirectoryStructure.getBoardStyleDirectory(), "Standard.yml").delete();
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
			LogUtils.warning("Version string [" + ver + "] doesn't look right!");
			return 0;
		}
	}

	public static List<String> getPluginConfiguration() {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = getConfig();
		for (String k : config.getDefaults().getKeys(true)) {
			if (config.isConfigurationSection(k))
				continue;
			res.add("&f" + k + "&- = '&e" + config.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
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

	public static void setPluginConfiguration(String key, String val) throws ChessException {
		
		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) { //$NON-NLS-1$
			ChessCraft.tickTask.start(0L);
		} else if (key.equalsIgnoreCase("locale")) {
			Messages.setMessageLocale(val);
			// redraw control panel signs in the right language
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("log_level")) {
			LogUtils.setLogLevel(val);
		} else if (key.equalsIgnoreCase("teleporting")) {
			updateAllControlPanels();
		}

		setConfigItem(getConfig(), key, val);

		ChessCraft.getInstance().saveConfig();
	}

	private static void updateAllControlPanels() {
		for (BoardView bv : BoardView.listBoardViews()) {
			bv.getControlPanel().repaintSignButtons();
			bv.getControlPanel().repaintClocks();
		}
	}

	public static <T> void setPluginConfiguration(String key, List<T> list) throws ChessException {
		setConfigItem(getConfig(), key, list);

		ChessCraft.getInstance().saveConfig();
	}

	/**
	 * Sets a configuration item in the given config object.  The key and value are both strings; the value
	 * will be converted into an object of the correct type, if possible (where the type is discovered from
	 * the config's default object).  The type's class must provide a constructor which takes a single string
	 * or an exception will be thrown.
	 * 
	 * @param config	The configuration object
	 * @param key		The configuration key
	 * @param val		The value
	 * @throws SMSException	if the key is unknown or a bad numeric value is passed
	 */
	public static void setConfigItem(Configuration config, String key, String val) throws ChessException {
		Configuration defaults = config.getDefaults();
		if (!defaults.contains(key)) {
			throw new ChessException(Messages.getString("ChessConfig.noSuchKey", key));
		}
		if (defaults.get(key) instanceof List<?>) {
			List<String>list = new ArrayList<String>(1);
			list.add(val);
			handleListValue(config, key, list);
		} else if (defaults.get(key) instanceof String) {
			// should be marginally quicker than going through the following method...
			config.set(key, val);
		} else {
			// the class we're converting to needs to have a constructor taking a single String argument
			Class<?> c = null;
			try {
				c = defaults.get(key).getClass();
				Constructor<?> ctor = c.getDeclaredConstructor(String.class);
				config.set(key, ctor.newInstance(val));
			} catch (NoSuchMethodException e) {
				throw new ChessException("Don't know how to convert '" + val + "' into a " + c.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof NumberFormatException) {
					throw new ChessException("Invalid numeric value: " + val);
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	public static <T> void setConfigItem(Configuration config, String key, List<T> list) throws ChessException {
		if (config.getDefaults().get(key) == null) {
			throw new ChessException(Messages.getString("ChessConfig.noSuchKey", key));
		}
		if (!(config.getDefaults().get(key) instanceof List<?>)) {
			throw new ChessException("Key '" + key + "' does not accept a list of values");
		}
		handleListValue(config, key, list);
	}

	@SuppressWarnings("unchecked")
	private static <T> void handleListValue(Configuration config, String key, List<T> list) {
		HashSet<T> current;

		List<T> cList = (List<T>) config.getList(key);
		if (list.get(0).equals("-")) {
			// remove specifed item from list
			list.remove(0);
			current = new HashSet<T>(cList);
			current.removeAll(list);
		} else if (list.get(0).equals("=")) {
			// replace list
			list.remove(0);
			current = new HashSet<T>(list);
		} else if (list.get(0).equals("+")) {
			// append to list
			list.remove(0);
			current = new HashSet<T>(cList);
			current.addAll(list);
		} else {
			// append to list
			current = new HashSet<T>(cList);
			current.addAll(list);
		}

		config.set(key, new ArrayList<T>(current));
	}
} // end class ChessConfig

