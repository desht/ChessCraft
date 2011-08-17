package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

public class SMSIntegration {
	private static final String TP_GAME = "cc:tp-game"; //$NON-NLS-1$
	private static final String CREATE_GAME = "cc:create-game"; //$NON-NLS-1$
	private static final String BOARD_INFO = "cc:board-info"; //$NON-NLS-1$
	private static final String GAME_INFO = "cc:game-info"; //$NON-NLS-1$
	private static final String DEL_GAME = "cc:delete-game"; //$NON-NLS-1$
	
	private static SMSHandler smsHandler;
	
	static void setup(ScrollingMenuSign sms) {
		if (smsHandler == null) {
			smsHandler = sms.getHandler();
		}
	}
	
	static void createMenus() {
		createMenu(BOARD_INFO, Messages.getString("SMSIntegration.boardInfo")); //$NON-NLS-1$
		createMenu(CREATE_GAME, Messages.getString("SMSIntegration.createGame")); //$NON-NLS-1$
		createMenu(TP_GAME, Messages.getString("SMSIntegration.gotoGame")); //$NON-NLS-1$
		createMenu(GAME_INFO, Messages.getString("SMSIntegration.gameInfo")); //$NON-NLS-1$
		createMenu(DEL_GAME, Messages.getString("SMSIntegration.deleteGame")); //$NON-NLS-1$
		
		for (BoardView bv : BoardView.listBoardViews(true)) {
			boardCreated(bv);
			
		}
		
		// now enable autosaving
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith("cc:")) { //$NON-NLS-1$
				menu.setAutosave(true);
			}
		}
	}
	
	static void deleteMenus() {
		List<String> toDelete = new ArrayList<String>();
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith("cc:")) { //$NON-NLS-1$
				toDelete.add(menu.getName());
			}
		}
		for (String name : toDelete) {
			deleteMenu(name);
		}
	}

	static void boardCreated(BoardView bv) {
		addItem(BOARD_INFO, bv.getName(), "/chess list board " + bv.getName()); //$NON-NLS-1$
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
		addItem(CREATE_GAME, bv.getName(), "/chess create game - " + bv.getName()); //$NON-NLS-1$
	}
	
	static void gameCreated(Game game) {
		addItem(GAME_INFO, game.getName(), "/chess list game " + game.getName()); //$NON-NLS-1$
		addItem(TP_GAME, game.getName(), "/chess tp " + game.getName()); //$NON-NLS-1$
		addItem(DEL_GAME, game.getName(), "/chess delete game " + game.getName()); //$NON-NLS-1$

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
				menu.addItem(label, command, ""); //$NON-NLS-1$
				menu.updateSigns();
			} catch (SMSException e) {
				// shouldn't get here
				ChessCraftLogger.warning("No such SMS menu", e); //$NON-NLS-1$
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
				ChessCraftLogger.warning("No such SMS menu", e); //$NON-NLS-1$
			}
		}
	}
	
	private static void createMenu(String name, String title) {
		SMSMenu menu = null;
		if (!smsHandler.checkMenu(name)) {
			menu = smsHandler.createMenu(name, title, "&ChessCraft"); //$NON-NLS-1$
			menu.setAutosort(true);	
		} else {
			try {
				// clear all menu items - start with a clean slate
				menu = smsHandler.getMenu(name);
				menu.setTitle(ChessUtils.parseColourSpec(title));
				menu.removeAllItems();
			} catch (SMSException e) {
				// shouldn't get here - we already checked that the menu exists
				ChessCraftLogger.warning("No such SMS menu", e); //$NON-NLS-1$
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
				ChessCraftLogger.warning("No such SMS menu", e); //$NON-NLS-1$
			}
		}
	}
}
