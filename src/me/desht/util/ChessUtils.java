/**
 * Programmer: Jacob Scott
 * Program Name: ChessUtils
 * Description: misc. functions
 * Date: Jul 23, 2011
 */
package me.desht.util;

import me.desht.chesscraft.chess.ChessGame;
import chesspresso.Chess;
import java.util.ArrayList;
import java.util.logging.Level;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.log.ChessCraftLogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author jacob
 */
public class ChessUtils {

	private int tickTaskId = -1;
	private static String prevColour = ""; //$NON-NLS-1$

	public void setupRepeatingTask(Plugin plugin, int initialDelay) {
		if (plugin == null) {
			return;
		}
		BukkitScheduler s = plugin.getServer().getScheduler();
		if (tickTaskId != -1) {
			s.cancelTask(tickTaskId);
		}
		tickTaskId = s.scheduleSyncRepeatingTask(plugin, new Runnable() {

			@Override
			public void run() {
				for (BoardView bv : BoardView.listBoardViews()) {
					bv.doLighting();
				}
				for (ChessGame game : ChessGame.listGames()) {
					game.clockTick();
					game.checkForAutoDelete();
				}
			}
		}, 20L * initialDelay, 20L * plugin.getConfiguration().getInt("tick_interval", 1)); //$NON-NLS-1$
	}

	public static void errorMessage(Player player, String string) {
		prevColour = ChatColor.RED.toString();
		message(player, string, ChatColor.RED, Level.WARNING);
	}

	public static void statusMessage(Player player, String string) {
		prevColour = ChatColor.AQUA.toString();
		message(player, string, ChatColor.AQUA, Level.INFO);
	}

	public static void alertMessage(Player player, String string) {
		if (player == null) {
			return;
		}
		prevColour = ChatColor.YELLOW.toString();
		message(player, string, ChatColor.YELLOW, Level.INFO);
	}

	public static void generalMessage(Player player, String string) {
		prevColour = ChatColor.WHITE.toString();
		message(player, string, Level.INFO);
	}
	
	public static void broadcastMessage(String string) {
		prevColour = ChatColor.YELLOW.toString();
		Bukkit.getServer().broadcastMessage(ChessUtils.parseColourSpec("&4::&-" + string)); //$NON-NLS-1$
	}

	private static void message(Player player, String string, Level level) {
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(parseColourSpec(line));
			} else {
				ChessCraftLogger.log(level, line);
			}
		}
	}

	private static void message(Player player, String string, ChatColor colour, Level level) {
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(colour + parseColourSpec(line));
			} else {
				ChessCraftLogger.log(level, line);
			}
		}
	}

	public static String parseColourSpec(String spec) {
		String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7"); //$NON-NLS-1$ //$NON-NLS-2$
		return res.replace("&-", prevColour).replace("&&", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
} // end class ChessUtils

