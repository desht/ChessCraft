package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SMSIntegration {
	private static final String TP_GAME = "cc:tp-game";
	private static final String CREATE_GAME = "cc:create-game";
	private static final String BOARD_INFO = "cc:board-info";
	private static final String GAME_INFO = "cc:game-info";
	private static final String DEL_GAME = "cc:delete-game";
	
	private static SMSHandler smsHandler;
	
	static void setup() {
		if (smsHandler == null) {
			Plugin p = Bukkit.getServer().getPluginManager().getPlugin("ScrollingMenuSign");
			if (p != null && p instanceof ScrollingMenuSign) {
				ScrollingMenuSign sms = (ScrollingMenuSign) p;
				smsHandler = sms.getHandler();
				ChessCraftLogger.info("ScrollingMenuSign integration is enabled");
			} else {
				ChessCraftLogger.info("ScrollingMenuSign integration is not enabled");
			}
		}
	}
	
	static boolean isActive() {
		return smsHandler != null;
	}
	
	static void createMenus() {
		// TODO: extract message strings
		createMenu(BOARD_INFO, "&1Board Info");
		createMenu(CREATE_GAME, "&1Create a Game");
		createMenu(TP_GAME, "&1Go to Game");
		createMenu(GAME_INFO, "&1Game Info");
		createMenu(DEL_GAME, "&4*Delete Game");
		
		for (BoardView bv : BoardView.listBoardViews(true)) {
			boardCreated(bv.getName());
			if (bv.getGame() != null) {
				String gameName = bv.getGame().getName();
				gameCreated(gameName);
				boardInUse(bv.getName());
			}
		}
		
		// now enable autosaving
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith("cc:")) {
				menu.setAutosave(true);
			}
		}
	}
	
	static void deleteMenus() {
		List<String> toDelete = new ArrayList<String>();
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith("cc:")) {
				toDelete.add(menu.getName());
			}
		}
		for (String name : toDelete) {
			deleteMenu(name);
		}
	}

	static void boardCreated(String boardName) {
		addItem(BOARD_INFO, boardName, "/chess list board " + boardName);
		addItem(CREATE_GAME, boardName, "/chess create game - " + boardName);
	}
	
	static void boardInUse(String boardName) {
		removeItem(CREATE_GAME, boardName);
	}
	
	static void boardNotInUse(String boardName) {
		addItem(CREATE_GAME, boardName, "/chess create game - " + boardName);
	}
	
	static void boardDeleted(String boardName) {
		removeItem(BOARD_INFO, boardName);
		removeItem(CREATE_GAME, boardName);
	}
	
	static void gameCreated(String gameName) {
		addItem(GAME_INFO, gameName, "/chess list game " + gameName);
		addItem(TP_GAME, gameName, "/chess tp " + gameName);
		addItem(DEL_GAME, gameName, "/chess delete game " + gameName);
	}
	
	static void gameDeleted(String gameName) {
		removeItem(GAME_INFO, gameName);
		removeItem(TP_GAME, gameName);
		removeItem(DEL_GAME, gameName);
	}

	private static void addItem(String menuName, String label, String command) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.addItem(label, command, "");
				menu.updateSigns();
			} catch (SMSException e) {
				// shouldn't get here
				ChessCraftLogger.warning("No such SMS menu", e);
			}
		}
	}
	
	private static void removeItem(String menuName, String label) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.removeItem(label);
				menu.updateSigns();
			} catch (SMSException e) {
				ChessCraftLogger.warning("No such SMS menu", e);
			}
		}
	}
	
	private static void createMenu(String name, String title) {
		SMSMenu menu = null;
		if (!smsHandler.checkMenu(name)) {
			menu = smsHandler.createMenu(name, title, "&ChessCraft");
			menu.setAutosort(true);	
		} else {
			try {
				// clear all menu items - start with a clean slate
				menu = smsHandler.getMenu(name);
				menu.setTitle(ChessUtils.parseColourSpec(title));
				menu.removeAllItems();
			} catch (SMSException e) {
				// shouldn't get here - we already checked that the menu exists
				ChessCraftLogger.warning("No such SMS menu", e);
			}
		}
		if (menu != null)
			menu.setAutosave(false);
	}

	private static void deleteMenu(String name) {
		if (smsHandler.checkMenu(name)) {
			try {
				smsHandler.deleteMenu(name);
			} catch (SMSException e) {
				ChessCraftLogger.warning("No such SMS menu", e);
			}
		}
	}
}
