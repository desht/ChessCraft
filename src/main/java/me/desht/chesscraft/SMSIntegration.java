package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.event.ChessBoardCreatedEvent;
import me.desht.chesscraft.event.ChessBoardDeletedEvent;
import me.desht.chesscraft.event.ChessGameCreatedEvent;
import me.desht.chesscraft.event.ChessGameDeletedEvent;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SMSIntegration implements Listener {

	// menu names
	private static final String TP_GAME = "cc_tp-game"; //$NON-NLS-1$
	private static final String CREATE_GAME = "cc_create-game"; //$NON-NLS-1$
	private static final String BOARD_INFO = "cc_board-info"; //$NON-NLS-1$
	private static final String GAME_INFO = "cc_game-info"; //$NON-NLS-1$
	private static final String DEL_GAME = "cc_delete-game"; //$NON-NLS-1$
	private static final String TP_BOARD = "cc_tp-board"; //$NON-NLS-1$

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

		addItem(TP_BOARD, bv.getName(), "/chess tp -b " + bv.getName()); //$NON-NLS-1$
		addItem(BOARD_INFO, bv.getName(), "/chess list board " + bv.getName()); //$NON-NLS-1$
		addItem(CREATE_GAME, bv.getName(), "/chess create game - " + bv.getName()); //$NON-NLS-1$
	}

	@EventHandler
	public void boardDeleted(ChessBoardDeletedEvent event) {
		BoardView bv = event.getBoardView();

		removeItem(TP_BOARD, bv.getName());
		removeItem(BOARD_INFO, bv.getName());
		removeItem(CREATE_GAME, bv.getName());
	}

	@EventHandler
	public void gameCreated(ChessGameCreatedEvent event) {
		ChessGame game = event.getGame();

		addItem(GAME_INFO, game.getName(), "/chess list game " + game.getName()); //$NON-NLS-1$
		addItem(TP_GAME, game.getName(), "/chess tp " + game.getName()); //$NON-NLS-1$
		addItem(DEL_GAME, game.getName(), "/chess delete game " + game.getName()); //$NON-NLS-1$

        BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
        if (bv != null) {
            removeItem(CREATE_GAME, bv.getName());
        }
	}

	@EventHandler
	public void gameDeleted(ChessGameDeletedEvent event) {
		ChessGame game = event.getGame();

		removeItem(GAME_INFO, game.getName());
		removeItem(TP_GAME, game.getName());
		removeItem(DEL_GAME, game.getName());

        BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
        if (bv != null) {
            addItem(CREATE_GAME, bv.getName(), "/chess create game - " + bv.getName());
        }
	}

	private void createMenus() {
		createMenu(BOARD_INFO, Messages.getString("SMSIntegration.boardInfo"));
		createMenu(CREATE_GAME, Messages.getString("SMSIntegration.createGame"));
		createMenu(TP_GAME, Messages.getString("SMSIntegration.gotoGame"));
		createMenu(GAME_INFO, Messages.getString("SMSIntegration.gameInfo"));
		createMenu(DEL_GAME, Messages.getString("SMSIntegration.deleteGame"));
		createMenu(TP_BOARD, Messages.getString("SMSIntegration.gotoBoard"));

		setAutosave(false);
	}

	private void createMenu(String name, String title) {
		SMSMenu menu;
		if (!smsHandler.checkMenu(name)) {
			menu = smsHandler.createMenu(name, title, ChessCraft.getInstance());
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
				menu.addItem(new SMSMenuItem(menu, label, command, ""));
				menu.notifyObservers(SMSMenuAction.REPAINT);
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
				menu.notifyObservers(SMSMenuAction.REPAINT);
			} catch (SMSException e) {
				LogUtils.warning(null, e); //$NON-NLS-1$
			}
		}
	}
}
