/**
 * Programmer: Jacob Scott
 * Program Name: ChessCraftLogger
 * Description: logging class
 * Date: Jul 29, 2011
 */
package me.desht.chesscraft.log;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;

public class ChessCraftLogger {

	protected static final Logger logger = Logger.getLogger("Minecraft");
	//protected static final String name = "ChessCraft";
	protected static final String messageFormat = "ChessCraft: %s";

	/*----------------- generic logging functions ---------------------*/
	public static void log(String message) {
		if (message != null) {
			logger.log(Level.INFO, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void log(Level level, String message) {
		if (level == null) {
			level = Level.INFO;
		}
		if (message != null) {
			logger.log(level, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void log(Level level, String message, Exception err) {
		if (err == null) {
			log(level, message);
		} else {
			logger.log(level, String.format(messageFormat,
					message == null ? (err == null ? "?" : err.getMessage()) : ChatColor.stripColor(message)), err);
		}
	}

	/*------------------- logging levels ------------------------------*/
	public static void fine(String message) {
		if (message != null) {
			logger.log(Level.FINE, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void info(String message) {
		if (message != null) {
			logger.log(Level.INFO, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void warning(String message) {
		if (message != null) {
			logger.log(Level.WARNING, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void severe(String message) {
		if (message != null) {
			logger.log(Level.SEVERE, String.format(messageFormat, ChatColor.stripColor(message)));
		}
	}

	public static void info(String message, Exception err) {
		if (err == null) {
			info(message);
		} else {
			logger.log(Level.INFO, String.format(messageFormat, 
					message == null ? err.getMessage() : ChatColor.stripColor(message)), err);
		}
	}

	public static void warning(String message, Exception err) {
		if (err == null) {
			warning(message);
		} else {
			logger.log(Level.WARNING, String.format(messageFormat, 
					message == null ? err.getMessage() : ChatColor.stripColor(message)), err);
		}
	}

	public static void severe(String message, Exception err) {
		if (err == null) {
			severe(message);
		} else {
			logger.log(Level.SEVERE, String.format(messageFormat, 
					message == null ? err.getMessage() : ChatColor.stripColor(message)), err);
		}
	}
} // end class ChessCraftLogger

