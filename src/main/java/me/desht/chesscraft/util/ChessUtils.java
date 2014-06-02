/**
 * Programmer: Jacob Scott
 * Program Name: ChessUtils
 * Description: misc. functions
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft.util;

import chesspresso.Chess;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MaterialWithData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author jacob
 */
public class ChessUtils {

	/**
	 * Get the colour string for the given Chesspresso colour.
	 *
	 * @param c the colour
	 * @return string for the colour, appropriately translated
	 */
	public static String getColour(int c) {
		switch (c) {
		case Chess.WHITE: return Messages.getString("Game.white");
		case Chess.BLACK: return Messages.getString("Game.black");
		default: throw new IllegalArgumentException("Invalid colour: " + c);
		}
	}

	/**
	 * Get the colour string for the given colour, with markup for display purposes.
	 *
	 * @param c the colour
	 * @return a marked-up display string
	 */
	public static String getDisplayColour(int c) {
		String s = c == Chess.WHITE ? "&f" : "&8";
		return s + getColour(c) + "&-";
	}

	/**
	 * get PGN format of the date (the version in chesspresso.pgn.PGN gets the
	 * month wrong :( )
	 *
	 * @param when date to convert
	 * @return PGN format of the date
	 */
	public static String dateToPGNDate(long when) {
		return new SimpleDateFormat("yyyy.MM.dd").format(new Date(when));
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

		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs);
	}

	/**
	 * Get a piece name from a Chesspresso piece number.
	 *
	 * @param piece the Chesspresso piece number
	 * @return a string representation for the piece, appropriately translated
	 */
	public static String pieceToStr(int piece) {
		switch (piece) {
		case Chess.PAWN:
			return Messages.getString("ChessUtils.pawn");
		case Chess.ROOK:
			return Messages.getString("ChessUtils.rook");
		case Chess.KNIGHT:
			return Messages.getString("ChessUtils.knight");
		case Chess.BISHOP:
			return Messages.getString("ChessUtils.bishop");
		case Chess.KING:
			return Messages.getString("ChessUtils.king");
		case Chess.QUEEN:
			return Messages.getString("ChessUtils.queen");
		default:
			return "???";
		}
	}

	/**
	 * Do a fuzzy match (Levenshtein) for the given string in the given set.
	 *
	 * @param search the string to find
	 * @param set the set of strings to search
	 * @param minDist the minimum Levenshtein distance
	 * @return the closest matching string
	 */
	public static String[] fuzzyMatch(String search, String set[], int minDist) {
		ArrayList<String> matches = new ArrayList<String>();
		int dist = minDist;
		if (search != null) {
			for (String s : set) {
				if (s != null) {
					int d = StringUtils.getLevenshteinDistance(s, search);
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
		return matches.toArray(new String[matches.size()]);
	}

	public static Material getWandMaterial() {
		String wand = ChessCraft.getInstance().getConfig().getString("wand_item");
		if (wand.isEmpty() || wand.equalsIgnoreCase("*")) {
			return null;
		} else {
			MaterialWithData mat = MaterialWithData.get(wand);
			return mat == null ? null : mat.getBukkitMaterial();
		}
	}

	public static String getWandDescription() {
		Material mat = getWandMaterial();
		return mat == null ? Messages.getString("ChessUtils.anything") : mat.toString();
	}




} // end class ChessUtils

