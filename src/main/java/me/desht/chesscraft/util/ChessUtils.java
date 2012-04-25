/**
 * Programmer: Jacob Scott
 * Program Name: ChessUtils
 * Description: misc. functions
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft.util;

import chesspresso.Chess;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author jacob
 */
public class ChessUtils {
	
	private static Map<String,String> prevColours = new HashMap<String,String>();

	public static String getColour(int c) {
		switch (c) {
		case Chess.WHITE:
			return Messages.getString("Game.white"); //$NON-NLS-1$
		case Chess.BLACK:
			return Messages.getString("Game.black"); //$NON-NLS-1$
		default:
			return "???"; //$NON-NLS-1$
		}
	}

	/**
	 * get PGN format of the date (the version in chesspresso.pgn.PGN gets the
	 * month wrong :( )
	 * 
	 * @param date
	 *            date to convert
	 * @return PGN format of the date
	 */
	public static String dateToPGNDate(long when) {
		return new SimpleDateFormat("yyyy.MM.dd").format(new Date(when)); //$NON-NLS-1$
	}

	public static String milliSecondsToHMS(long l) {
		l /= 1000;
	
		long secs = l % 60;
		long hrs = l / 3600;
		long mins = (l - (hrs * 3600)) / 60;
	
		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs); //$NON-NLS-1$
	}

	public static void errorMessage(Player player, String string) {
		message(player, string, ChatColor.RED, Level.WARNING);
	}

	public static void statusMessage(Player player, String string) {
		message(player, string, ChatColor.AQUA, Level.INFO);
	}

	public static void alertMessage(Player player, String string) {
		message(player, string, ChatColor.YELLOW, Level.INFO);
	}

	public static void generalMessage(Player player, String string, ChatColor colour) {
		message(player, string, Level.INFO);
	}

	public static void broadcastMessage(String string) {
		setPrevColour(null, ChatColor.YELLOW.toString());
		Bukkit.getServer().broadcastMessage(ChessUtils.parseColourSpec(null, "&6[ChessCraft]&e " + string)); //$NON-NLS-1$
	}

	private static void message(Player player, String string, Level level) {
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(parseColourSpec(player, line));
			} else {
				ChessCraftLogger.log(level, line);
			}
		}
	}

	private static void message(Player player, String string, ChatColor colour, Level level) {
		setPrevColour(player, colour.toString());
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(colour + parseColourSpec(player, line));
			} else {
				ChessCraftLogger.log(level, line);
			}
		}
	}

	private static void setPrevColour(Player player, String colour) {
		String name = player != null ? player.getName() : "*";
		prevColours.put(name, colour);
	}
	
	private static String getPrevColour(Player player) {
		String name = player != null ? player.getName() : "*";
		return prevColours.containsKey(name) ? prevColours.get(name) : "";
	}
	
	private static String parseColourSpec(Player player, String spec) {
		String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7"); //$NON-NLS-1$ //$NON-NLS-2$
		return res.replace("&-", getPrevColour(player)).replace("&&", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public static String parseColourSpec(String spec) {
		String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7"); //$NON-NLS-1$ //$NON-NLS-2$
		return res.replace("&-", "").replace("&&", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static String formatLoc(Location loc) {
		String str = "<" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ loc.getWorld().getName() + ">"; //$NON-NLS-1$
		return str;
	}

	public static String pieceToStr(int piece) {
		switch (piece) {
		case Chess.PAWN:
			return Messages.getString("ChessUtils.pawn"); //$NON-NLS-1$
		case Chess.ROOK:
			return Messages.getString("ChessUtils.rook"); //$NON-NLS-1$
		case Chess.KNIGHT:
			return Messages.getString("ChessUtils.knight"); //$NON-NLS-1$
		case Chess.BISHOP:
			return Messages.getString("ChessUtils.bishop"); //$NON-NLS-1$
		case Chess.KING:
			return Messages.getString("ChessUtils.king"); //$NON-NLS-1$
		case Chess.QUEEN:
			return Messages.getString("ChessUtils.queen"); //$NON-NLS-1$
		default:
			return "???"; //$NON-NLS-1$
		}
	}

	public static String[] fuzzyMatch(String search, String set[], int minDist) {
		ArrayList<String> matches = new ArrayList<String>();
		int dist = minDist;
		if (search != null) {
			for (String s : set) {
				if (s != null) {
					int d = ChessUtils.getLevenshteinDistance(s, search);
					if (d < dist) {
						dist = d;
						matches.clear();
						matches.add(s);
					} else if (d == dist) {
						matches.add(s);
					}
				}
			}
		}
		return matches.toArray(new String[0]);
	}

	/**
	 * Compute the Levenshtein distance between two strings. This is
	 * appropriated from the Apache Jakarta Commons project.
	 * 
	 * @param s
	 *            The first string
	 * @param t
	 *            The second string
	 * @return The distance between them
	 */
	public static int getLevenshteinDistance(String s, String t) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null"); //$NON-NLS-1$
		}

		/*
		 * The difference between this impl. and the previous is that, rather
		 * than creating and retaining a matrix of size s.length()+1 by
		 * t.length()+1, we maintain two single-dimensional arrays of length
		 * s.length()+1. The first, d, is the 'current working' distance array
		 * that maintains the newest distance cost counts as we iterate through
		 * the characters of String s. Each time we increment the index of
		 * String t we are comparing, d is copied to p, the second int[]. Doing
		 * so allows us to retain the previous cost counts as required by the
		 * algorithm (taking the minimum of the cost count to the left, up one,
		 * and diagonally up and to the left of the current cost count being
		 * calculated). (Note that the arrays aren't really copied anymore, just
		 * switched...this is clearly much better than cloning an array or doing
		 * a System.arraycopy() each time through the outer loop.)
		 * 
		 * Effectively, the difference between the two implementations is this
		 * one does not cause an out of memory condition when calculating the LD
		 * over two very large strings.
		 */

		int n = s.length(); // length of s
		int m = t.length(); // length of t

		if (n == 0) {
			return m;
		} else if (m == 0) {
			return n;
		}

		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int d[] = new int[n + 1]; // cost array, horizontally
		int _d[]; // placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t

		char t_j; // jth character of t

		int cost; // cost

		for (i = 0; i <= n; i++) {
			p[i] = i;
		}

		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;

			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
			}

			// copy current distance counts to 'previous row' distance counts
			_d = p;
			p = d;
			d = _d;
		}

		// our last action in the above loop was to switch d and p, so p now
		// actually has the most recent cost counts
		return p[n];
	}

	public static boolean partialMatch(String[] args, int index, String match) {
		if (index >= args.length) {
			return false;
		}
		return partialMatch(args[index], match);
	}

	public static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) {
			return false;
		}
		return str.substring(0, l).equalsIgnoreCase(match);
	}

	public static List<String> splitQuotedString(String s) {
		List<String> matchList = new ArrayList<String>();

		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(s);

		while (regexMatcher.find()) {
			if (regexMatcher.group(1) != null) {
				// Add double-quoted string without the quotes
				matchList.add(regexMatcher.group(1));
			} else if (regexMatcher.group(2) != null) {
				// Add single-quoted string without the quotes
				matchList.add(regexMatcher.group(2));
			} else {
				// Add unquoted word
				matchList.add(regexMatcher.group());
			}
		}

		return matchList;
	}


	public static int getWandId() {
		String wand = ChessConfig.getConfig().getString("wand_item"); //$NON-NLS-1$
		if (wand.equalsIgnoreCase("*")) {
			return -1;
		}
		MaterialWithData mat = MaterialWithData.get(wand);
		return mat == null ? 0 : mat.getMaterial();
	}
	
	public static String getWandDescription() {
		int id = getWandId();
		
		return id < 0 ? Messages.getString("ChessUtils.anything") : MaterialWithData.get(id).toString();
	}
	
	public static String formatStakeStr(double stake) {
		try {
			return ChessCraft.economy.format(stake);
		} catch (Exception e) {
			ChessCraftLogger.warning("Caught exception from " + ChessCraft.economy.getName() + " while trying to format quantity " + stake + ":");
			e.printStackTrace();
			ChessCraftLogger.warning("ChessCraft will continue but you should verify your economy plugin configuration.");
			return new DecimalFormat("#0.00").format(stake);
		}
	}
	
} // end class ChessUtils

