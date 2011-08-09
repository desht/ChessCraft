package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

public class SMSIntegration {
	private static final String TP_GAME = "cc:tp-game";
	private static final String CREATE_GAME = "cc:create-game";
	private static final String BOARD_INFO = "cc:board-info";
	private static final String GAME_INFO = "cc:game-info";
	private static final String DEL_GAME = "cc:delete-game";
	
	private static SMSHandler smsHandler;
	
	static void setup(ScrollingMenuSign sms) {
		if (smsHandler == null) {
			smsHandler = sms.getHandler();
		}
	}
	
	static void createMenus() {
		// TODO: extract message strings
		createMenu(BOARD_INFO, "&1Board Info");
		createMenu(CREATE_GAME, "&1Create a Game");
		createMenu(TP_GAME, "&1Go to Game");
		createMenu(GAME_INFO, "&1Game Info");
		createMenu(DEL_GAME, "&4*Delete Game");
		
		for (BoardView bv : BoardView.listBoardViews(true)) {
			boardCreated(bv);
			
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

	static void boardCreated(BoardView bv) {
		addItem(BOARD_INFO, bv.getName(), "/chess list board " + bv.getName());
		boardNotInUse(bv);
		if (bv.getGame() != null) {
			gameCreated(bv.getGame());
		}
	}
	
	static void boardDeleted(BoardView bv) {
		removeItem(BOARD_INFO, bv.getName());
		if (bv.getGame() == null) {
			removeItem(CREATE_GAME, bv.getName());
		}
	}

	static void boardInUse(BoardView bv) {
		removeItem(CREATE_GAME, bv.getName());
	}
	
	static void boardNotInUse(BoardView bv) {
		addItem(CREATE_GAME, bv.getName(), "/chess create game - " + bv.getName());
	}
	
	static void gameCreated(Game game) {
		addItem(GAME_INFO, game.getName(), "/chess list game " + game.getName());
		addItem(TP_GAME, game.getName(), "/chess tp " + game.getName());
		addItem(DEL_GAME, game.getName(), "/chess delete game " + game.getName());

		boardInUse(game.getView());
	}
	
	static void gameDeleted(Game game) {
		removeItem(GAME_INFO, game.getName());
		removeItem(TP_GAME, game.getName());
		removeItem(DEL_GAME, game.getName());
		
		boardNotInUse(game.getView());
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
