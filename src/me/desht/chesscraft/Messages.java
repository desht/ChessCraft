package me.desht.chesscraft;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map.Entry;

import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.util.config.Configuration;

public class Messages {

	static Configuration messages = null;

	public static void loadMessages() throws IOException {
		File langDir = ChessConfig.getLanguagesDirectory();
		String locale = ChessConfig.getConfiguration().getString("locale", "default").toLowerCase();
		File wanted = new File(langDir, locale + ".yml");

		if (wanted.isFile()) {
			// just load it (but pull in any new messages from the shipped file if possible)
			messages = checkUpToDate(wanted);
		} else {
			// first see if there's a shipped file for this exact locale in the JAR
			ChessConfig.extractResource("/datafiles/lang/" + wanted.getName(), langDir, true);
			if (wanted.isFile()) {
				// we found an exact match - just load that
				// we don't need to compare because we've only just extracted the file
				messages = new Configuration(wanted);
				messages.load();
			} else {
				// try to find the closest matching locale (which might just be "default")
				File actual = locateMessageFile(wanted);
				messages = checkUpToDate(actual);
			}
		}

		// ensure we actually have some messages - if not, fall back to default
		if (messages.getAll().isEmpty()) {
			ChessCraftLogger.warning("can't find any messages for " + locale + ": falling back to default");
			File def = new File(langDir, "default.yml");
			messages = checkUpToDate(locateMessageFile(def));
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
		tmpFile.delete();
		ChessConfig.extractResource("/datafiles/lang/" + f.getName(), tmpFile, true);

		// load the temporary file into a temp configuration object
		Configuration tmpCfg = new Configuration(tmpFile);
		tmpCfg.load();

		// load the real (extracted) file
		Configuration actualCfg = new Configuration(f);
		actualCfg.load();

		// merge the temp config into the actual one, adding any non-existent keys
		for (Entry<String, Object> e : tmpCfg.getAll().entrySet()) {
			if (actualCfg.getProperty(e.getKey()) == null) {
				actualCfg.setProperty(e.getKey(), e.getValue());
			}
		}

		actualCfg.save();

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
		String s = messages.getString(key);
		if (s == null) {
			ChessCraftLogger.warning(null, new Exception("Unexpected missing key '" + key + "'"));
			return "!" + key + "!";
		} else {
			return s;
		}

	}

	public static String getString(String key, Object... args) {
		return MessageFormat.format(getString(key), args);
	}
}
