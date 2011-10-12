package me.desht.chesscraft;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.desht.chesscraft.log.ChessCraftLogger;

//import org.bukkit.util.config.Configuration;

public class Messages {

	static Configuration fallbackMessages = null;
	static Configuration messages = null;

	public static void init() throws IOException {
		if (fallbackMessages == null) {
			File langDir = ChessConfig.getLanguagesDirectory();
			File def = new File(langDir, "default.yml");
			fallbackMessages = checkUpToDate(locateMessageFile(def));
		}
		loadMessages();
	}

	public static void loadMessages() throws IOException {
		File langDir = ChessConfig.getLanguagesDirectory();
		String locale = ChessConfig.getConfiguration().getString("locale", "default").toLowerCase();
		File wanted = new File(langDir, locale + ".yml");

		if (wanted.isFile() && wanted.lastModified() > ChessConfig.getJarFile().lastModified()) {
			// file exists on disk and is newer than the JAR
			messages = checkUpToDate(wanted);
		} else if (!wanted.isFile()) {
			// file does not exist on disk, attempt to extract from the JAR
			ChessConfig.extractResource("/datafiles/lang/" + wanted.getName(), langDir, true);
			if (wanted.isFile()) {
				messages = YamlConfiguration.loadConfiguration(wanted);
			} else {
				// find the best match (could be default.yml)
				File actual = locateMessageFile(wanted);
				messages = actual.getName().equals("default.yml") ? fallbackMessages : checkUpToDate(actual);
			}
		} else {
			// file exists on disk but we have a newer version in the JAR
			messages = checkUpToDate(wanted);
		}

		// ensure we actually have some messages - if not, fall back to default
		if (messages.getKeys(false).isEmpty()) {
			messages = fallbackMessages;
		}
	}

	/**
	 * Ensure that the extracted file on disk (if any) has all the messages that the
	 * shipped file (in the JAR) has.  But don't modify any messages in the extracted
	 * file that have already been changed (i.e. allow users to set custom messages if
	 * they wish).
	 *  
	 * @param f
	 * @throws IOException 
	 */
	private static Configuration checkUpToDate(File f) throws IOException {
		File langDir = ChessConfig.getLanguagesDirectory();

		// extract the shipped file to a temporary file
		File tmpFile = File.createTempFile("msg", ".tmp", langDir);
		ChessConfig.extractResource("/datafiles/lang/" + f.getName(), tmpFile, true);

		// load the temporary file into a temp configuration object
		Configuration tmpCfg = YamlConfiguration.loadConfiguration(tmpFile);

		// load the real (extracted) file
		YamlConfiguration actualCfg = YamlConfiguration.loadConfiguration(f);

		// merge the temp config into the actual one, adding any non-existent keys
		for (String key : tmpCfg.getKeys(true)) {
			if (!actualCfg.contains(key) && !tmpCfg.isConfigurationSection(key)) {
				actualCfg.set(key, tmpCfg.get(key));
			}
		}

		// ensure that the config we're loading has all of the messages that the fallback has
		if (fallbackMessages != null && actualCfg.getKeys(true).size() != fallbackMessages.getKeys(true).size()) {
			List<String> missingKeys = new ArrayList<String>();
			for (String key : fallbackMessages.getKeys(true)) {
				if (!actualCfg.contains(key) && !fallbackMessages.isConfigurationSection(key)) {
					actualCfg.set(key, fallbackMessages.get(key));
					missingKeys.add(key);
				}
			}
			actualCfg.set("NEEDS_TRANSLATION", missingKeys);
		}

		tmpFile.delete();
		actualCfg.save(f);

		return actualCfg;
	}

	private static File locateMessageFile(File wanted) {
		if (wanted == null) {
			return null;
		}
		if (wanted.isFile() && wanted.canRead()) {
			return wanted;
		} else {
			String basename = wanted.getName().replaceAll("\\.yml$", "");
			if (basename.contains("_")) {
				basename = basename.replaceAll("_.+$", "");
			}
			File actual = new File(wanted.getParent(), basename + ".yml");
			if (actual.isFile() && actual.canRead()) {
				return actual;
			} else {
				String locale = ChessConfig.getConfiguration().getString("locale", "default");
				ChessCraftLogger.warning("no messages catalog for " + locale + " found - falling back to default");
				return new File(wanted.getParent(), "default.yml");
			}
		}
	}

	public static String getString(String key) {
		if (messages == null) {
			return "!" + key + "!";
		}
		String s = getKey(messages, key);
		if (s == null) {
			ChessCraftLogger.warning(null, new Exception("Unexpected missing key '" + key + "'"));
			s = getKey(fallbackMessages, key);
			if (s == null) {
				s = "!" + key + "!";
			}
		}
		return s;
	}

	private static String getKey(Configuration conf, String key) {
		String s = null;
		Object o = conf.get(key);
		if (o instanceof String) {
			s = o.toString();
		} else if (o instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<String> l = (List<String>) o;
			StringBuilder add = new StringBuilder();
			for (int i = 0; i < l.size(); ++i) {
				add.append(l.get(i));
				if (i + 1 < l.size()) {
					add.append("\n");
				}
			}
			s = add.toString();
		}
		return s;
	}

	public static String getString(String key, Object... args) {
		try {
			return MessageFormat.format(getString(key), args);
		} catch (Exception e) {
			ChessCraftLogger.severe("Error fomatting message for " + key + ": " + e.getMessage());
			return getString(key);
		}
	}
}
