package me.desht.chesscraft;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.base.Joiner;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.log.ChessCraftLogger;

public class Messages {

	static Configuration fallbackMessages = null;
	static Configuration messages = null;

	public static void init(String locale) {
		File langDir = DirectoryStructure.getLanguagesDirectory();

		DirectoryStructure.extractResource("/datafiles/lang/default.yml", langDir);
		DirectoryStructure.extractResource("/datafiles/lang/es_es.yml", langDir);
		DirectoryStructure.extractResource("/datafiles/lang/de_de.yml", langDir);
		DirectoryStructure.extractResource("/datafiles/lang/ru_ru.yml", langDir);

		try {
			fallbackMessages = loadMessageFile("default");
		} catch (ChessException e) {
			ChessCraftLogger.severe("can't load fallback messages file!", e);
		}
		
		try {
			setMessageLocale(locale);
		} catch (ChessException e) {
			ChessCraftLogger.warning("can't load messages for " + locale + ": using default");
			messages = fallbackMessages;
		}
	}

	public static void setMessageLocale(String wantedLocale) throws ChessException {
		messages = loadMessageFile(wantedLocale);
	}

	private static Configuration loadMessageFile(String wantedLocale) throws ChessException {
		File langDir = DirectoryStructure.getLanguagesDirectory();
		File wanted = new File(langDir, wantedLocale + ".yml");
		File located = locateMessageFile(wanted);
		if (located == null) {
			throw new ChessException("Unknown locale '" + wantedLocale + "'");
		}
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(located);

		// ensure that the config we're loading has all of the messages that the fallback has
		// make a note of any missing translations
		if (fallbackMessages != null && conf.getKeys(true).size() != fallbackMessages.getKeys(true).size()) {
			List<String> missingKeys = new ArrayList<String>();
			for (String key : fallbackMessages.getKeys(true)) {
				if (!conf.contains(key) && !fallbackMessages.isConfigurationSection(key)) {
					conf.set(key, fallbackMessages.get(key));
					missingKeys.add(key);
				}
			}
			conf.set("NEEDS_TRANSLATION", missingKeys);
			try {
				conf.save(located);
			} catch (IOException e) {
				ChessCraftLogger.warning("Can't write " + located + ": " + e.getMessage());
			}
		}

		return conf;
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
				return null;
			}
		}
	}

	private static String getString(Configuration conf, String key) {
		String s = null;
		Object o = conf.get(key);
		if (o instanceof String) {
			s = o.toString();
		} else if (o instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<String> l = (List<String>) o;
			s = Joiner.on("\n").join(l);
		}
		return s;
	}

	public static String getString(String key) {
		if (messages == null) {
			ChessCraftLogger.warning("No messages catalog!?!");
			return "!" + key + "!";
		}
		String s = getString(messages, key);
		if (s == null) {
			ChessCraftLogger.warning("Missing message key '" + key + "'");
			s = getString(fallbackMessages, key);
			if (s == null) {
				s = "!" + key + "!";
			}
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
