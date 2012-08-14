package me.desht.chesscraft;

import java.io.File;
import java.io.IOException;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.event.ChessGameCreatedEvent;
import me.desht.chesscraft.event.ChessGameDeletedEvent;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.DirectoryStructure;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SMSIntegration implements Listener {

	// menu names
	private static final String TP_GAME = "cc_tp-game"; //$NON-NLS-1$
	private static final String CREATE_GAME = "cc_create-game"; //$NON-NLS-1$
	private static final String BOARD_INFO = "cc_board-info"; //$NON-NLS-1$
	private static final String GAME_INFO = "cc_game-info"; //$NON-NLS-1$
	private static final String DEL_GAME = "cc_delete-game"; //$NON-NLS-1$

	private final SMSHandler smsHandler;

	public SMSIntegration(ScrollingMenuSign sms) {
		smsHandler = sms.getHandler();
		Bukkit.getPluginManager().registerEvents(this, ChessCraft.getInstance());
		createMenus();
	}

	public void setAutosave(boolean autosave) {
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith("cc_")) { //$NON-NLS-1$
				menu.setAutosave(autosave);
			}
		}
	}

	@EventHandler
	public void boardCreated(ChessBoardCreatedEvent event) {
		BoardView bv = event.getBoardView();

		addItem(BOARD_INFO, bv.getName(), "/chess list board " + bv.getName()); //$NON-NLS-1$
	}

	@EventHandler
	public void boardDeleted(ChessBoardDeletedEvent event) {
		BoardView bv = event.getBoardView();

		removeItem(BOARD_INFO, bv.getName());
		if (bv.getGame() == null) {
			removeItem(CREATE_GAME, bv.getName());
		}
	}

	@EventHandler
	public void gameCreated(ChessGameCreatedEvent event) {
		ChessGame game = event.getGame();
		
		addItem(GAME_INFO, game.getName(), "/chess list game " + game.getName()); //$NON-NLS-1$
		addItem(TP_GAME, game.getName(), "/chess tp " + game.getName()); //$NON-NLS-1$
		addItem(DEL_GAME, game.getName(), "/chess delete game " + game.getName()); //$NON-NLS-1$
		
		removeItem(CREATE_GAME, game.getView().getName());
	}

	@EventHandler
	public void gameDeleted(ChessGameDeletedEvent event) {
		ChessGame game = event.getGame();
		
		removeItem(GAME_INFO, game.getName());
		removeItem(TP_GAME, game.getName());
		removeItem(DEL_GAME, game.getName());

		addItem(CREATE_GAME, game.getView().getName(), "/chess create game - " + game.getView().getName());  //$NON-NLS-1$
	}

	private void createMenus() {
		createMenu(BOARD_INFO, Messages.getString("SMSIntegration.boardInfo")); //$NON-NLS-1$
		createMenu(CREATE_GAME, Messages.getString("SMSIntegration.createGame")); //$NON-NLS-1$
		createMenu(TP_GAME, Messages.getString("SMSIntegration.gotoGame")); //$NON-NLS-1$
		createMenu(GAME_INFO, Messages.getString("SMSIntegration.gameInfo")); //$NON-NLS-1$
		createMenu(DEL_GAME, Messages.getString("SMSIntegration.deleteGame")); //$NON-NLS-1$
	
		setAutosave(false);
	
		cleanupOldMenuNames();
	}

	private void createMenu(String name, String title) {
		SMSMenu menu = null;
		if (!smsHandler.checkMenu(name)) {
			menu = smsHandler.createMenu(name, title, "&ChessCraft"); //$NON-NLS-1$
			menu.setAutosort(true);
		} else {
			try {
				// clear all menu items - start with a clean slate
				menu = smsHandler.getMenu(name);
				menu.setTitle(MiscUtil.parseColourSpec(title));
				menu.removeAllItems();
			} catch (SMSException e) {
				// shouldn't get here - we already checked that the menu exists
				LogUtils.warning(null, e); //$NON-NLS-1$
			}
		}
	}

	private void addItem(String menuName, String label, String command) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.addItem(label, command, ""); //$NON-NLS-1$
				menu.notifyObservers();
			} catch (SMSException e) {
				// shouldn't get here
				LogUtils.warning(null, e); //$NON-NLS-1$
			}
		}
	}

	private void removeItem(String menuName, String label) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.removeItem(label);
				menu.notifyObservers();
			} catch (SMSException e) {
				LogUtils.warning(null, e); //$NON-NLS-1$
			}
		}
	}

	private void cleanupOldMenuNames() {
		if (smsHandler.checkMenu("cc:tp-game")) {
			// clean up the old-style named menus (they didn't work on Windows due to the colon... oops)
			try {
				boolean smsReloadNeeded = false;

				// update any existing views to attach to the new menus
				for (SMSView view : SMSView.listViews()) {
					if (view.getName().startsWith("cc:")) {
						smsReloadNeeded = true;
						migrateView(view.getName(), view.getMenu().getName());
					}
				}
				// get rid of the old menus
				smsHandler.deleteMenu("cc:tp-game");
				smsHandler.deleteMenu("cc:create-game");
				smsHandler.deleteMenu("cc:board-info");
				smsHandler.deleteMenu("cc:game-info");
				smsHandler.deleteMenu("cc:delete-game");

				if (smsReloadNeeded) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sms reload");
				}

				LogUtils.info("Updated all ChessCraft/ScrollingMenuSign menus & view to new naming standard ('cc_' prefix)");
			} catch (SMSException e) {
				LogUtils.warning("Caught exception while cleaning up obsolete SMS menus", e);
			}
		}
	}

	private void migrateView(String viewName, String menuName) {
		File f1 = new File(DirectoryStructure.getViewsFolder(), viewName + ".yml");
		viewName = viewName.replaceFirst("cc:", "cc_");
		menuName = menuName.replaceFirst("cc:", "cc_");
		File f2 = new File(DirectoryStructure.getViewsFolder(), viewName + ".yml");

		YamlConfiguration conf = YamlConfiguration.loadConfiguration(f1);
		conf.set("name", viewName);
		conf.set("menu", menuName);
		try {
			conf.save(f2);
		} catch (IOException e) {
			LogUtils.warning("Can't rewrite view: " + f2, e);
		}
	}
}
