/**
 * Programmer: Jacob Scott
 * Program Name: ChessUtils
 * Description: misc. functions
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import chesspresso.Chess;

/**
 * @author jacob
 */
public class ChessUtils {

	/**
	 * Get the colour string for the given Chesspresso colour.
	 * 
	 * @param c
	 * @return
	 */
	public static String getColour(int c) {
		switch (c) {
		case Chess.WHITE: return Messages.getString("Game.white"); //$NON-NLS-1$
		case Chess.BLACK: return Messages.getString("Game.black"); //$NON-NLS-1$
		default: throw new IllegalArgumentException("Invalid colour: " + c);
		}
	}
	
	/**
	 * Get the colour string for the given colour, with markup for display purposes.
	 * 
	 * @param c
	 * @return
	 */
	public static String getDisplayColour(int c) {
		String s = c == Chess.WHITE ? "&f" : "&8";
		return s + getColour(c) + ChatColor.RESET;
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

	/**
	 * Format an elapsed time.
	 * 
	 * @param l		time in milliseconds
	 * @return	A time string in HH:MM:SS format
	 */
	public static String milliSecondsToHMS(long l) {
		l /= 1000;

		long secs = l % 60;
		long hrs = l / 3600;
		long mins = (l - (hrs * 3600)) / 60;

		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs); //$NON-NLS-1$
	}

	/**
	 * Get a piece name from a Chesspresso piece number.
	 * 
	 * @param piece
	 * @return
	 */
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

	public static int getWandId() {
		String wand = ChessCraft.getInstance().getConfig().getString("wand_item"); //$NON-NLS-1$
		if (wand.equalsIgnoreCase("*")) {
			return -1;
		}
		MaterialWithData mat = MaterialWithData.get(wand);
		return mat == null ? 0 : mat.getId();
	}

	public static String getWandDescription() {
		int id = getWandId();

		return id < 0 ? Messages.getString("ChessUtils.anything") : MaterialWithData.get(id).toString();
	}

	public static String formatStakeStr(double stake) {
		try {
			if (ChessCraft.economy != null && ChessCraft.economy.isEnabled()) {
				return ChessCraft.economy.format(stake);
			}
		} catch (Exception e) {
			LogUtils.warning("Caught exception from " + ChessCraft.economy.getName() + " while trying to format quantity " + stake + ":");
			e.printStackTrace();
			LogUtils.warning("ChessCraft will continue but you should verify your economy plugin configuration.");
		}
		return new DecimalFormat("#0.00").format(stake);
	}

	public static void playEffect(Location loc, String effectName) {
		Configuration cfg = ChessCraft.getInstance().getConfig();
		String effect = cfg.getString("effects." + effectName);
		if (effect == null) {
			LogUtils.warning("unknown effect name '" + effectName + "'");
			return;
		}
		if (effect.equals("$explosion")) {
			loc.getWorld().createExplosion(loc, 0.0f);
		} else if (effect.equals("$lightning")) {
			loc.getWorld().strikeLightningEffect(loc);
		} else if (effect.startsWith("effect/")) {
			String[] a = effect.substring(7).split("/");
			Effect e = Effect.valueOf(a[0].toUpperCase());
			if (e != null) {
				try {
					if (a.length > 2) {	
						loc.getWorld().playEffect(loc, e, Integer.parseInt(a[1]), Integer.parseInt(a[2]));
					} else if (a.length > 1) {
						loc.getWorld().playEffect(loc, e, Integer.parseInt(a[1]));
					} else {
						loc.getWorld().playEffect(loc, e, 0);
					}
				} catch (NumberFormatException ex) {
					LogUtils.warning("invalid effect specifier: " + effect + ": " + ex.getMessage());
				}
			} else {
				LogUtils.warning("unknown effect '" + effect + "'");
			}
		} else {
			MiscUtil.playNamedSound(loc, effect, (float)cfg.getDouble("effects.volume", 1.0f), 1.0f);
		}
	}
	
} // end class ChessUtils

