package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import me.jascotty2.bukkit.MinecraftChatStr;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageBuffer {

	private static final Map<String, List<String>> bufferMap = new HashMap<String, List<String>>();
	private static final Map<String, Integer> currentPage = new HashMap<String, Integer>();
	private static final int pageSize = 18;
	private static final String bar = "------------------------------------------------";
	private static final String footerBar = "---Use /chess page [#|n|p] to see other pages---";

	/**
	 * initialize the buffer for the player if necessary
	 * @param p
	 */
	static private void init(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			bufferMap.put(name(p), new ArrayList<String>());
			currentPage.put(name(p), 1);
		}
	}

	/**
	 * Get the player's name
	 * 
	 * @param p		The player, may be null
	 * @return		Player's name, or &CONSOLE if the player is null
	 */
	static private String name(Player p) {
		return p == null ? "&CONSOLE" : p.getName();
	}

	/**
	 * Add a message to the buffer.
	 * @param p			The player
	 * @param message	The message line to add
	 */
	static void add(Player p, String message) {
		init(p);
		bufferMap.get(name(p)).add(message);
	}

	/**
	 * Add a block of messages.  All message should stay on the same page if possible - add
	 * padding to ensure this where necessary.  If block is larger than the page size, then just
	 * add it.
	 * @param p			The player
	 * @param messages	List of message lines to add
	 */
	static void add(Player p, String[] messages) {
		init(p);
		bufferMap.get(name(p)).addAll(Arrays.asList(messages));
	}

	static void add(Player p, LinkedList<String> lines) {
		init(p);
		//TODO: apply MinecraftChatStr.alignTags(lines, true)
		//		in pagesize segments before adding to buffer
		bufferMap.get(name(p)).addAll(lines);
	}

	/**
	 * Clear the player's message buffer
	 * @param p	The player
	 */
	static void clear(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			return;
		}

		bufferMap.get(name(p)).clear();
		currentPage.put(name(p), 1);
	}

	/**
	 * Delete the message buffer for the player.  Should be called when the player logs out.
	 * @param p	The player
	 */
	static void delete(Player p) {
		bufferMap.remove(name(p));
		currentPage.remove(name(p));
	}

	/**
	 * Get the number of lines in the player's message buffer.
	 * @param p	The player
	 * @return	The number of lines
	 */
	static int getSize(Player p) {
		if (!bufferMap.containsKey(name(p))) {
			return 0;
		}

		return bufferMap.get(name(p)).size();
	}

	public static int getPageSize() {
		return pageSize;
	}

	static int getPageCount(Player p) {
		return (getSize(p) - 1) / pageSize + 1;
	}

	static String getLine(Player p, int i) {
		if (!bufferMap.containsKey(name(p))) {
			return null;
		}

		return bufferMap.get(name(p)).get(i);
	}

	static void setPage(Player player, int page) {
		if (page < 1 || page > getPageCount(player)) {
			return;
		}
		currentPage.put(name(player), page);
	}

	static void nextPage(Player player) {
		setPage(player, getPage(player) + 1);
	}

	static void prevPage(Player player) {
		setPage(player, getPage(player) - 1);
	}

	static int getPage(Player player) {
		return currentPage.get(name(player));
	}

	static void showPage(Player player) {
		showPage(player, currentPage.get(name(player)));
	}

	static void showPage(Player player, String pageStr) {
		try {
			int pageNum = Integer.parseInt(pageStr);
			showPage(player, pageNum);
		} catch (NumberFormatException e) {
			ChessUtils.errorMessage(player, "invalid argument '" + pageStr + "'");
		}
	}

	static void showPage(Player player, int pageNum) {
		if (!bufferMap.containsKey(name(player))) {
			return;
		}

		if (player != null) {
			// pretty paged display
			if (pageNum < 1 || pageNum > getPageCount(player)) {
				throw new IllegalArgumentException("page number " + pageNum + " is out of range");
			}

			int nMessages = getSize(player);
			ChessUtils.statusMessage(player, ChatColor.GREEN + MinecraftChatStr.strPadCenterChat(
					(pageSize > nMessages ? nMessages : pageSize)
					+ " of " + nMessages + " lines (page " + pageNum 
					+ "/" + getPageCount(player) + ")", 310, '-'));
			int i = (pageNum - 1) * pageSize;
			for (; i < nMessages && i < pageNum * pageSize; ++i) {
				ChessUtils.statusMessage(player, getLine(player, i));
			}
			
			// if block is smaller than a page, add padding to keep the block on one page
			for (; i < pageNum * pageSize; ++i) {
				ChessUtils.statusMessage(player, "");
			}
			
			ChessUtils.statusMessage(player, ChatColor.GREEN.toString()
					+ (nMessages > pageSize * pageNum ? footerBar : bar));

			setPage(player, pageNum);
		} else {
			// just dump the whole message buffer to the console
			for (String s : bufferMap.get(name(player))) {
				ChessUtils.statusMessage(null, ChatColor.stripColor(ChessUtils.parseColourSpec(s)));
			}
		}
	}
}
