/**
 * Programmer: Jacob Scott
 * Program Name: ChessCraftLogger
 * Description: logging class
 * Date: Jul 29, 2011
 */

package me.desht.chesscraft.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChessCraftLogger {

	protected static final Logger logger = Logger.getLogger("Minecraft");
	//protected static final String name = "ChessCraft";
	protected static final String messageFormat = "ChessCraft: %s";

	/*----------------- generic logging functions ---------------------*/
	public static void log(String message) {
		logger.log(Level.INFO, String.format(messageFormat, message));
	}

	public static void log(Level level, String message) {
		logger.log(level, String.format(messageFormat, message));
	}

	public static void log(Level level, String message, Exception err) {
		logger.log(level, String.format(messageFormat,
				message == null ? (err == null ? "?" : err.getMessage()) : message), err);
	}
	
	/*------------------- logging levels ------------------------------*/

	public static void fine(String message){
		logger.log(Level.FINE, String.format(messageFormat, message));
	}
	
	public static void info(String message) {
		logger.log(Level.INFO, String.format(messageFormat, message));
	}
	
	public static void warning(String message) {
		logger.log(Level.WARNING, String.format(messageFormat, message));
	}
	
	public static void severe(String message){
		logger.log(Level.SEVERE, String.format(messageFormat, message));
	}

	public static void info(String message, Exception err) {
		logger.log(Level.INFO, String.format(messageFormat, message), err);
	}
	
	public static void warning(String message, Exception err) {
		logger.log(Level.WARNING, String.format(messageFormat, message), err);
	}
	
	public static void severe(String message, Exception err){
		logger.log(Level.SEVERE, String.format(messageFormat, message), err);
	}
} // end class ChessCraftLogger
