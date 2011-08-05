package me.desht.chesscraft;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SMSIntegration {
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
		createMenu("all-boards", "&1All Boards");
		createMenu("free-boards", "&1Free Boards");
		for (BoardView bv : BoardView.listBoardViews(true)) {
			addItem("all-boards", bv.getName(), "/chess list board " + bv.getName());
			if (bv.getGame() == null) {
				addItem("free-boards", bv.getName(), "/chess create game - " + bv.getName());
			}
		}
	}
	
	static void deleteMenus() {
		deleteMenu("all-boards");
		deleteMenu("free-boards");
	}

	static void boardCreated(String boardName) {
		addItem("all-boards", boardName, "/chess list board " + boardName);
		addItem("free-boards", boardName, "/chess create game - " + boardName);
	}
	
	static void boardInUse(String boardName) {
		removeItem("free-boards", boardName);
	}
	
	static void boardNotInUse(String boardName) {
		addItem("free-boards", boardName, "/chess create game - " + boardName);
	}
	
	static void boardDeleted(String boardName) {
		removeItem("all-boards", boardName);
		removeItem("free-boards", boardName);
	}

	private static void addItem(String menuName, String label, String command) {
		if (smsHandler.checkMenu(menuName)) {
			SMSMenu menu;
			try {
				menu = smsHandler.getMenu(menuName);
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
		if (!smsHandler.checkMenu(name)) {
			smsHandler.createMenu(name, title, "&ChessCraft");
		} else {
			try {
				// clear all menu items - start with a clean slate
				smsHandler.getMenu(name).removeAllItems();
			} catch (SMSException e) {
				// shouldn't get here - we already checked that the menu exists
				ChessCraftLogger.warning("No such SMS menu", e);
			}
		}
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
