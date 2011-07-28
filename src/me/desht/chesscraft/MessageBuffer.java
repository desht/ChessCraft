/**
 * 
 */
package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 */
public class MessageBuffer {
	private static final Map<String, List<String>> bufferMap = new HashMap<String, List<String>>();
	private static final Map<String, Integer> currentPage = new HashMap<String, Integer>();
	private static final int pageSize = 18;
	private static final String bar = "------------------------------------------------";
	private static final String footerBar = "---Use /chess page [#|n|p] to see other pages---";
	
	/**
	 * Initialise the buffer for the player if necessary
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
		
		if (messages.length > pageSize) {
			// block is bigger than a page, just add it
		} else if ((getSize(p) % pageSize) + messages.length > pageSize) {
			int amount = pageSize - (getSize(p) % pageSize);
			System.out.println("pad needed: size=" + getSize(p) + ", len=" + messages.length + " amount=" + amount);
			// add padding to keep the block on one page
			for (int i = 1; i <= amount; i++) {
				System.out.println("pad " + i);
				bufferMap.get(name(p)).add("");
			}
		}
		for (String line : messages) {
			bufferMap.get(name(p)).add(line);
		}
	}
	
	/**
	 * Clear the player's message buffer
	 * @param p	The player
	 */
	static void clear(Player p) {
		if (!bufferMap.containsKey(name(p)))
			return;
		
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
	static int getSize(Player p)	{
		if (!bufferMap.containsKey(name(p)))
			return 0;
		
		return bufferMap.get(name(p)).size();
	}
	
	static int getPageCount(Player p) {
		return (getSize(p) - 1) / pageSize + 1;
	}
	
	static String getLine(Player p, int i) {
		if (!bufferMap.containsKey(name(p)))
			return null;
		
		return bufferMap.get(name(p)).get(i);
	}
	
	static void setPage(Player player, int page) {
		if (page < 1 || page > getPageCount(player))
			return;
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
		if (!bufferMap.containsKey(name(player)))
			return;
		
        if (player != null) {
            // pretty paged display
    		if (pageNum < 1 || pageNum > getPageCount(player))
    			throw new IllegalArgumentException("page number " + pageNum + " is out of range");
    		
            int nMessages = getSize(player);
            String headerLine = "---" + nMessages + " lines (page " + pageNum + "/" + getPageCount(player) + ")";
            String headerBar = headerLine + bar.substring(0, bar.length() - headerLine.length());
            ChessUtils.statusMessage(player, ChatColor.GREEN + headerBar);
            for (int i = (pageNum - 1) * pageSize; i < nMessages && i < pageNum * pageSize; ++i) {
                ChessUtils.statusMessage(player, getLine(player, i));
            }
            String footer = (nMessages > pageSize * pageNum) ? footerBar : bar;
            ChessUtils.statusMessage(player, ChatColor.GREEN + footer);
            
            setPage(player, pageNum);
        } else {
            // just dump the whole message buffer to the console
            for (String s : bufferMap.get(name(player))) {
                ChessUtils.statusMessage(null, ChatColor.stripColor(ChessUtils.parseColourSpec(s)));
            }
        }
    }
}
