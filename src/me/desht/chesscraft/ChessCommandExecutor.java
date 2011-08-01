package me.desht.chesscraft;

import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.ChessAI.AI_Def;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.expector.ExpectYesNoOffer;
import me.desht.chesscraft.enums.GameState;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import java.util.LinkedList;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.ChessPermission;
import me.desht.chesscraft.enums.ExpectAction;
import me.jascotty2.bukkit.MinecraftChatStr;

public class ChessCommandExecutor implements CommandExecutor {

	private ChessCraft plugin;

	public ChessCommandExecutor(ChessCraft plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}

		if (label.equalsIgnoreCase("chess")) { //$NON-NLS-1$
			if (args.length == 0) {
				return false;
			}
			try {
				if (partialMatch(args[0], "ga")) { // game //$NON-NLS-1$
					gameCommand(player, args);
				} else if (partialMatch(args[0], "c")) { // create //$NON-NLS-1$
					createCommands(player, args);
				} else if (partialMatch(args[0], "d")) { // delete //$NON-NLS-1$
					deleteCommands(player, args);
				} else if (partialMatch(args[0], "l")) { // list //$NON-NLS-1$
					listCommands(player, args);
				} else if (partialMatch(args[0], "i")) { // invite //$NON-NLS-1$
					inviteCommand(player, args);
				} else if (partialMatch(args[0], "j")) { // join //$NON-NLS-1$
					joinCommand(player, args);
				} else if (partialMatch(args[0], "star")) { // start //$NON-NLS-1$
					startCommand(player, args);
				} else if (partialMatch(args[0], "stak")) { // stake //$NON-NLS-1$
					stakeCommand(player, args);
				} else if (partialMatch(args[0], "res")) { // resign //$NON-NLS-1$
					resignCommand(player, args);
				} else if (partialMatch(args[0], "red")) { // redraw //$NON-NLS-1$
					redrawCommand(player, args);
				} else if (partialMatch(args[0], "m")) { // move //$NON-NLS-1$
					moveCommand(player, args);
				} else if (partialMatch(args[0], "pa")) { // page //$NON-NLS-1$
					pageCommand(player, args);
				} else if (partialMatch(args[0], "pr")) { // promotion //$NON-NLS-1$
					promoCommand(player, args);
				} else if (partialMatch(args[0], "sa")) { // save //$NON-NLS-1$
					saveCommand(player, args);
				} else if (partialMatch(args[0], "rel")) { // reload //$NON-NLS-1$
					reloadCommand(player, args);
				} else if (partialMatch(args[0], "t")) { // tp //$NON-NLS-1$
					teleportCommand(player, args);
				} else if (partialMatch(args[0], "a")) { // archive //$NON-NLS-1$
					archiveCommand(player, args);
				} else if (partialMatch(args[0], "o")) { // offer //$NON-NLS-1$
					offerCommand(player, args);
				} else if (partialMatch(args[0], "y")) { // yes //$NON-NLS-1$
					responseCommand(player, args);
				} else if (partialMatch(args[0], "n")) { // no //$NON-NLS-1$
					responseCommand(player, args);
				} else if (partialMatch(args[0], "set")) { // setcfg //$NON-NLS-1$
					setcfgCommand(player, args);
				} else if (partialMatch(args[0], "get")) { // getcfg //$NON-NLS-1$
					getcfgCommand(player, args);
				} else if (partialMatch(args[0], "fen")) { // fen //$NON-NLS-1$
					fenCommand(player, args);
				} else if (partialMatch(args[0], "w")) { // win //$NON-NLS-1$
					claimVictoryCommand(player, args);
				} else {
					return false;
				}
			} catch (IllegalArgumentException e) {
				ChessUtils.errorMessage(player, e.getMessage());
			} catch (ChessException e) {
				ChessUtils.errorMessage(player, e.getMessage());
			} catch (IllegalMoveException e) {
				ChessUtils.errorMessage(player, e.getMessage());
			}
		} else {
			return false;
		}
		return true;
	}

	private void claimVictoryCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_WIN);
		notFromConsole(player);

		Game game = Game.getCurrentGame(player, true);

		game.ensureGameState(GameState.RUNNING);

		String other = game.getOtherPlayer(player.getName());
		if (other.isEmpty()) {
			return;
		}

		int timeout = plugin.getConfiguration().getInt("forfeit_timeout", 60); //$NON-NLS-1$
		long leftAt = plugin.playerListener.getPlayerLeftAt(other);
		if (leftAt == 0) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.otherPlayerMustBeOffline")); //$NON-NLS-1$
		}
		long now = System.currentTimeMillis();
		long elapsed = (now - leftAt) / 1000;
		if (elapsed >= timeout) {
			game.winByDefault(player.getName());
		} else {
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.needToWait", timeout - elapsed)); //$NON-NLS-1$
		}
	}

	private void fenCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_FEN);
		notFromConsole(player);

		if (args.length < 2) {
			return;
		}
		Game game = Game.getCurrentGame(player, true);

		game.setFen(combine(args, 1));

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.positionUpdatedFEN", //$NON-NLS-1$ 
		                                                    game.getName(), Game.getColour(game.getPosition().getToPlay())));
	}

	private void gameCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_GAME);
		notFromConsole(player);

		if (args.length >= 2) {
			Game.setCurrentGame(player.getName(), args[1]);
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameChanged", args[1])); //$NON-NLS-1$
		} else {
			Game game = Game.getCurrentGame(player, false);
			if (game == null) {
				ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.noActiveGame")); //$NON-NLS-1$
			} else {
				ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameIs", game.getName())); //$NON-NLS-1$
			}
		}
	}

	private void listCommands(Player player, String[] args) throws ChessException {

		if (partialMatch(args, 1, "g")) { // game //$NON-NLS-1$
			if (args.length > 2) {
				showGameDetail(player, args[2]);
			} else {
				listGames(player);
			}
		} else if (partialMatch(args, 1, "b")) { // board //$NON-NLS-1$
			if (args.length > 2) {
				showBoardDetail(player, args[2]);
			} else {
				listBoards(player);
			}
		} else if (partialMatch(args, 1, "a")) { // ai //$NON-NLS-1$
			listAIs(player);
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess list board"); //$NON-NLS-1$
			ChessUtils.errorMessage(player, "       /chess list game"); //$NON-NLS-1$
		}
	}

	private void deleteCommands(Player player, String[] args) throws ChessException {

		if (partialMatch(args, 1, "g")) { // game //$NON-NLS-1$
			tryDeleteGame(player, args);
		} else {
			if (partialMatch(args, 1, "b")) { // board //$NON-NLS-1$
				tryDeleteBoard(player, args);
			} else {
				ChessUtils.errorMessage(player, "Usage: /chess delete board <board-name>"); //$NON-NLS-1$
				ChessUtils.errorMessage(player, "       /chess delete game <game-name>"); //$NON-NLS-1$
			}
		}
	}

	private void createCommands(Player player, String[] args) throws ChessException {
		notFromConsole(player);

		if (partialMatch(args, 1, "g")) { // game //$NON-NLS-1$
			String gameName = args.length >= 3 ? args[2] : null;
			String boardName = args.length >= 4 ? args[3] : null;
			tryCreateGame(player, gameName, boardName);
		} else if (partialMatch(args, 1, "b")) { // board //$NON-NLS-1$
			tryCreateBoard(player, args);
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess create board <board-name> [-style <style>]"); //$NON-NLS-1$
			ChessUtils.errorMessage(player, "       /chess create game [<game-name>] [<board-name>]"); //$NON-NLS-1$
		}
	}

	private void saveCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_SAVE);

		plugin.persistence.save();
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.chessSaved")); //$NON-NLS-1$
	}

	private void reloadCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_RELOAD);

		boolean reloadPersisted = false;
		boolean reloadAI = false;
		boolean reloadConfig = false;

		if (partialMatch(args, 1, "a")) { //$NON-NLS-1$
			reloadAI = true;
		} else if (partialMatch(args, 1, "c")) { //$NON-NLS-1$
			reloadConfig = true;
		} else if (partialMatch(args, 1, "p")) { //$NON-NLS-1$
			reloadPersisted = true;
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess reload <ai|config|persist>"); //$NON-NLS-1$
		}

		if (reloadConfig) {
			plugin.getConfiguration().load();
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.configReloaded")); //$NON-NLS-1$
		}
		if (reloadAI) {
			ChessAI.initAI_Names();
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.AIdefsReloaded")); //$NON-NLS-1$
		}
		if (reloadPersisted) {
			plugin.persistence.reload();
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.persistedReloaded")); //$NON-NLS-1$
		}
	}

	private void startCommand(Player player, String[] args) throws ChessException {
		notFromConsole(player);
		if (args.length >= 2) {
			tryStartGame(player, Game.getGame(args[1]));
		} else {
			tryStartGame(player, Game.getCurrentGame(player));
		}
	}

	private void resignCommand(Player player, String[] args) throws ChessException {
		notFromConsole(player);
		if (args.length >= 2) {
			tryResignGame(player, Game.getGame(args[1]));
		} else {
			tryResignGame(player, Game.getCurrentGame(player, true));
		}
	}

	private void moveCommand(Player player, String[] args) throws IllegalMoveException, ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_MOVE);
		notFromConsole(player);

		if (args.length < 2) {
			ChessUtils.errorMessage(player, "Usage: /chess move <from> <to>" //$NON-NLS-1$
					+ Messages.getString("ChessCommandExecutor.algebraicNotation")); //$NON-NLS-1$
			return;
		}
		Game game = Game.getCurrentGame(player, true);

		String move = combine(args, 1).replaceFirst(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (move.length() != 4) {
			ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidMoveString", move)); //$NON-NLS-1$ 
			return;
		}
		int from = Chess.strToSqi(move.substring(0, 2));
		if (from == Chess.NO_SQUARE) {
			ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidFromSquare", move)); //$NON-NLS-1$
			return;
		}
		int to = Chess.strToSqi(move.substring(2, 4));
		if (to == Chess.NO_SQUARE) {
			ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidToSquare", move)); //$NON-NLS-1$
			return;
		}
		game.setFromSquare(from);
		game.doMove(player.getName(), to);
	}

	private void inviteCommand(Player player, String[] args) throws ChessException {
		notFromConsole(player);

		Game game = Game.getCurrentGame(player, true);
		String invitee = args.length >= 2 ? args[1] : null;
		tryInvitePlayer(player, game, invitee);
	}

	private void joinCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_JOIN);
		notFromConsole(player);

		String gameName = null;
		if (args.length >= 2) {
			gameName = args[1];
			Game.getGame(gameName).addPlayer(player.getName());
		} else {
			// find a game (or games) with an invitation for us
			for (Game game : Game.listGames()) {
				if (game.getInvited().equalsIgnoreCase(player.getName())) {
					game.addPlayer(player.getName());
					gameName = game.getName();
				}
			}
			if (gameName == null) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.noPendingInvitation")); //$NON-NLS-1$
			}
		}

		Game game = Game.getGame(gameName);
		Game.setCurrentGame(player.getName(), game);
		int playingAs = game.playingAs(player.getName());
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.joinedGame", //$NON-NLS-1$
		                                                    game.getName(), Game.getColour(playingAs)));
		
		if (plugin.getConfiguration().getBoolean("auto_teleport_on_join", true)) { //$NON-NLS-1$
			tryTeleportToGame(plugin.getServer().getPlayer(game.getPlayerWhite()), game);
			tryTeleportToGame(plugin.getServer().getPlayer(game.getPlayerBlack()), game);
		} else {
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.canTeleport", game.getName())); //$NON-NLS-1$
		}
	}

	private void redrawCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_REDRAW);

		if (args.length >= 2) {
			BoardView b = BoardView.getBoardView(args[1]);
			b.reloadStyle();
			b.paintAll();
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardRedrawn", b.getName())); //$NON-NLS-1$
		} else {
			for (BoardView bv : BoardView.listBoardViews()) {
				bv.paintAll();
			}
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.allBoardsRedrawn")); //$NON-NLS-1$
		}
	}

	private void teleportCommand(Player player, String[] args) throws ChessException {
		notFromConsole(player);

		if (args.length < 2) {
			// back to where we were
			tryTeleportOut(player);
		} else {
			// go to the named game
			Game game = Game.getGame(args[1]);
			tryTeleportToGame(player, game);
		}
	}

	private void archiveCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			tryArchiveGame(player, Game.getGame(args[1]));
		} else {
			notFromConsole(player);
			tryArchiveGame(player, Game.getCurrentGame(player));
		}
	}

	private void offerCommand(Player player, String[] args) throws ChessException {
		notFromConsole(player);

		Game game = Game.getCurrentGame(player, true);

		if (partialMatch(args, 1, "d")) { // draw //$NON-NLS-1$
			tryOfferDraw(player, game);
		} else if (partialMatch(args, 1, "s")) { // swap sides //$NON-NLS-1$
			tryOfferSwap(player, game);
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess offer (draw|swap)"); //$NON-NLS-1$
			return;
		}
	}

	private void responseCommand(Player player, String[] args) throws ChessException {
		boolean isAccepted = partialMatch(args, 0, "y") ? true : false; //$NON-NLS-1$

		doResponse(player, isAccepted);
	}

	private void promoCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_PROMOTE);
		notFromConsole(player);

		if (args.length >= 2) {
			Game game = Game.getCurrentGame(player, true);
			int piece = Chess.charToPiece(Character.toUpperCase(args[1].charAt(0)));
			game.setPromotionPiece(player.getName(), piece);
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.promotionPieceSet", //$NON-NLS-1$
			                                                    game.getName(),ChessUtils.pieceToStr(piece).toUpperCase()));
			game.getView().getControlPanel().repaintSignButtons();
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess promote <Q|N|B|R>"); //$NON-NLS-1$
		}
	}

	private void stakeCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_STAKE);
		notFromConsole(player);

		if (args.length >= 2) {
			try {
				Game game = Game.getCurrentGame(player);
				double amount = Double.parseDouble(args[1]);
				if (amount <= 0.0) {
					throw new ChessException(Messages.getString("ChessCommandExecutor.noNegativeStakes")); //$NON-NLS-1$
				}
				if (!Economy.canAfford(player.getName(), amount)) {
					throw new ChessException(Messages.getString("ChessCommandExecutor.cantAffordStake")); //$NON-NLS-1$
				}
				game.setStake(amount);
				game.getView().getControlPanel().repaintSignButtons();
				ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.stakeChanged", Economy.format(amount))); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", args[1])); //$NON-NLS-1$
			}
		} else {
			ChessUtils.errorMessage(player, "Usage: /chess stake <stake-amount>"); //$NON-NLS-1$
		}
	}

	private void getcfgCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_GETCONFIG);

		MessageBuffer.clear(player);
		if (args.length < 2) {
			for (String line : ChessConfig.getConfigList()) {
				MessageBuffer.add(player, line);
			}
			MessageBuffer.showPage(player);
		} else {
			String res = plugin.getConfiguration().getString(args[1]);
			if (res != null) {
				ChessUtils.statusMessage(player, args[1] + " = '" + res + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				ChessUtils.errorMessage(player, Messages.getString("ChessConfig.noSuchKey", args[1])); //$NON-NLS-1$
			}
		}
	}

	private void setcfgCommand(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_SETCONFIG);

		if (args.length < 3) {
			ChessUtils.errorMessage(player, "Usage: /chess setcfg <key> <value>"); //$NON-NLS-1$
		} else {
			String key = args[1], val = combine(args, 2);
			ChessConfig.setConfigItem(player, key, val);
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.configKeySet", key, val)); //$NON-NLS-1$
		}
	}

	private void pageCommand(Player player, String[] args) {
		if (args.length < 2) {
			// default is to advance one page and display
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (partialMatch(args, 1, "n")) { //$NON-NLS-1$
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (partialMatch(args, 1, "p")) { //$NON-NLS-1$
			MessageBuffer.prevPage(player);
			MessageBuffer.showPage(player);
		} else {
			try {
				int pageNum = Integer.parseInt(args[1]);
				MessageBuffer.showPage(player, pageNum);
			} catch (NumberFormatException e) {
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.invalidNumeric", args[1])); //$NON-NLS-1$
			}
		}
	}

	/*-------------------------------------------------------------------------------*/
	void tryTeleportToGame(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_TELEPORT);

		if (player == null) {
			return;
		}
		BoardView bv = game.getView();
		if (bv.isPartOfBoard(player.getLocation())) {
			return; // already there
		}
		Location loc;
		if (game.getPlayerWhite().equals(player.getName())) {
			loc = bv.getBounds().getUpperSW().clone();
			loc.setYaw(90.0f);
			loc.add(0.0, 1.5, -(1.0 + 4.5 * bv.getSquareSize()));
		} else if (game.getPlayerBlack().equals(player.getName())) {
			loc = bv.getBounds().getLowerNE().clone();
			loc.setYaw(270.0f);
			loc.add(0.0, 1.5, -1.0 + 4.5 * bv.getSquareSize());
		} else {
			loc = bv.getBounds().getLowerNE().clone();
			loc.setYaw(0.0f);
			loc.add(4.5 * bv.getSquareSize(), 1.5, 1.0);
		}
		System.out.println("teleport to " + loc); //$NON-NLS-1$
		if (loc.getBlock().getTypeId() != 0 || loc.getBlock().getRelative(BlockFace.UP).getTypeId() != 0) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.teleportDestObstructed")); //$NON-NLS-1$
		}
		doTeleport(player, loc);
		Game.setCurrentGame(player.getName(), game);
	}

	void tryTeleportOut(Player player) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_TELEPORT);

		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		Location prev = plugin.getLastPos(player);
		if (bv != null && (prev == null || BoardView.partOfChessBoard(prev) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				doTeleport(player, loc);
			} else {
				doTeleport(player, player.getWorld().getSpawnLocation());
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.goingToSpawn")); //$NON-NLS-1$
			}
		} else if (prev != null) {
			// go back to previous location
			doTeleport(player, prev);
		} else {
			ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.notOnChessboard")); //$NON-NLS-1$
		}
	}

	void tryOfferSwap(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_SWAP);

		game.ensurePlayerInGame(player.getName());

		String other = game.getOtherPlayer(player.getName());
		if (other.isEmpty()) {
			// no other player yet - just swap
			game.swapColours();
		} else {
			plugin.expecter.expectingResponse(player, ExpectAction.SwapResponse, new ExpectYesNoOffer(plugin, game,
					player.getName(), other), other);
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.sideSwapOfferedYou", other)); //$NON-NLS-1$ 
			game.alert(other, Messages.getString("ChessCommandExecutor.sideSwapOfferedOther", player.getName())); //$NON-NLS-1$ 
			game.alert(other, Messages.getString("ChessCommandExecutor.typeYesOrNo")); //$NON-NLS-1$
		}
		game.getView().getControlPanel().repaintSignButtons();
	}

	void tryOfferDraw(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_DRAW);

		game.ensurePlayerInGame(player.getName());
		game.ensurePlayerToMove(player.getName());
		game.ensureGameState(GameState.RUNNING);

		String other = game.getOtherPlayer(player.getName());
		plugin.expecter.expectingResponse(player, ExpectAction.DrawResponse,
				new ExpectYesNoOffer(plugin, game, player.getName(), other), other);
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.drawOfferedYou", other)); //$NON-NLS-1$
		game.alert(other, Messages.getString("ChessCommandExecutor.drawOfferedOther", player.getName())); //$NON-NLS-1$
		game.alert(other, Messages.getString("ChessCommandExecutor.typeYesOrNo")); //$NON-NLS-1$
		game.getView().getControlPanel().repaintSignButtons();
	}

	void listGames(Player player) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_LISTGAMES);

		if (Game.listGames().isEmpty()) {
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.noCurrentGames")); //$NON-NLS-1$
			return;
		}

		MessageBuffer.clear(player);
		for (Game game : Game.listGames(true)) {
			String name = game.getName();
			String curGameMarker = "  "; //$NON-NLS-1$
			if (player != null) {
				curGameMarker = game == Game.getCurrentGame(player) ? "+ " : "  "; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite(); //$NON-NLS-1$
			String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack(); //$NON-NLS-1$
			StringBuilder info = new StringBuilder(": &f" + curMoveW + white + " (W) v " + curMoveB + black + " (B) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			info.append("&e[").append(game.getState()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (game.getInvited().length() > 0) {
				info.append(Messages.getString("ChessCommandExecutor.invited", game.getInvited())); //$NON-NLS-1$
			}
			MessageBuffer.add(player, curGameMarker + name + info);
		}
		MessageBuffer.showPage(player);
	}

	void showGameDetail(Player player, String gameName) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_LISTGAMES);

		Game game = Game.getGame(gameName);

		String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite(); //$NON-NLS-1$
		String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack(); //$NON-NLS-1$

		String bullet = ChatColor.DARK_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		MessageBuffer.clear(player);
		MessageBuffer.add(player, Messages.getString("ChessCommandExecutor.gameDetail.name", gameName, game.getState())); //$NON-NLS-1$ 
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.gameDetail.players", white, black, game.getView().getName())); //$NON-NLS-1$ 
		MessageBuffer.add(player, bullet +  Messages.getString("ChessCommandExecutor.gameDetails.halfMoves", game.getHistory().size())); //$NON-NLS-1$
		if (Economy.active()) {
			MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.gameDetail.stake", Economy.format(game.getStake()))); //$NON-NLS-1$
		}
		MessageBuffer.add(player, bullet + (game.getPosition().getToPlay() == Chess.WHITE ? 
				Messages.getString("ChessCommandExecutor.gameDetail.whiteToPlay") :  //$NON-NLS-1$
				Messages.getString("ChessCommandExecutor.gameDetail.blackToPlay"))); //$NON-NLS-1$
		if (game.getState() == GameState.RUNNING) {
			MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.gameDetail.clock",
			                                                      Game.secondsToHMS(game.getTimeWhite()),
			                                                      Game.secondsToHMS(game.getTimeBlack())));
		}
		if (game.getInvited().equals("*")) { //$NON-NLS-1$
			MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.gameDetail.openInvitation")); //$NON-NLS-1$
		} else if (!game.getInvited().isEmpty()) {
			MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.gameDetail.invitation", game.getInvited())); //$NON-NLS-1$
		}
		MessageBuffer.add(player, Messages.getString("ChessCommandExecutor.gameDetail.moveHistory")); //$NON-NLS-1$
		List<Short> h = game.getHistory();
		for (int i = 0; i < h.size(); i += 2) {
			StringBuilder sb = new StringBuilder(String.format("&f%1$d. &-", (i / 2) + 1)); //$NON-NLS-1$
			sb.append(Move.getString(h.get(i)));
			if (i < h.size() - 1) {
				sb.append("  ").append(Move.getString(h.get(i + 1))); //$NON-NLS-1$
			}
			MessageBuffer.add(player, sb.toString());
		}

		MessageBuffer.showPage(player);
	}

	void showBoardDetail(Player player, String boardName) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_LISTBOARDS);

		BoardView bv = BoardView.getBoardView(boardName);

		String bullet = ChatColor.LIGHT_PURPLE + "* " + ChatColor.AQUA; //$NON-NLS-1$
		Cuboid bounds = bv.getOuterBounds();
		String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$

		MessageBuffer.clear(player);
		MessageBuffer.add(player, Messages.getString("ChessCommandExecutor.boardDetail.board", boardName)); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.lowerNE", //$NON-NLS-1$
		                                                      ChessUtils.formatLoc(bounds.getLowerNE())));
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.upperSW", //$NON-NLS-1$
		                                                      ChessUtils.formatLoc(bounds.getUpperSW())));
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.game", gameName)); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.boardStyle", bv.getBoardStyle())); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.pieceStyle", bv.getPieceStyle())); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.squareSize", bv.getSquareSize(),  //$NON-NLS-1$
		                                                      bv.getWhiteSquareMat(), bv.getBlackSquareMat()));
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.frameWidth", bv.getFrameWidth(), //$NON-NLS-1$
		                                                      bv.getFrameMat()));
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.enclosure", bv.getEnclosureMat())); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.Height", bv.getHeight())); //$NON-NLS-1$
		MessageBuffer.add(player, bullet + Messages.getString("ChessCommandExecutor.boardDetail.isLit", bv.getIsLit())); //$NON-NLS-1$

		MessageBuffer.showPage(player);
	}

	void listBoards(Player player) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_LISTBOARDS);

		if (BoardView.listBoardViews().isEmpty()) {
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.noBoards")); //$NON-NLS-1$
			return;
		}

		MessageBuffer.clear(player);
		for (BoardView bv : BoardView.listBoardViews(true)) {
			String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$
			MessageBuffer.add(player, Messages.getString("ChessCommandExecutor.boardList", bv.getName(), ChessUtils.formatLoc(bv.getA1Square()), //$NON-NLS-1$
			                                             bv.getBoardStyle(), gameName));
		}
		MessageBuffer.showPage(player);
	}

	void listAIs(Player player) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_LISTAI);

		MessageBuffer.clear(player);
		LinkedList<String> lines = new LinkedList<String>();
		for (AI_Def ai : ChessAI.listAIs(true)) {
			StringBuilder sb = new StringBuilder(Messages.getString("ChessCommandExecutor.AIList", //$NON-NLS-1$
			                                                        ai.getName(), ai.getEngine(), ai.getSearchDepth()));
			if (Economy.active()) {
				sb.append(player != null ? "<l>" : ", "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(Messages.getString("ChessCommandExecutor.AIpayout", (int) (ai.getPayoutMultiplier() * 100))); //$NON-NLS-1$
			}

			if (ai.getComment() != null && player != null && ((lines.size() + 1) % MessageBuffer.getPageSize()) == 0) {
				lines.add(""); // ensure description and comment are on the same page  $NON-NLS-1$
			}
			lines.add(sb.toString());
			if (ai.getComment() != null) {
				lines.add("  &2 - " + ai.getComment()); //$NON-NLS-1$
			}
		}
		lines = MinecraftChatStr.alignTags(lines, true);
		MessageBuffer.add(player, lines);
		MessageBuffer.showPage(player);
	}

	void tryCreateGame(Player player, String gameName, String boardName) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_NEWGAME);

		BoardView bv;
		if (boardName == null) {
			bv = BoardView.getFreeBoard();
		} else {
			bv = BoardView.getBoardView(boardName);
		}

		if (gameName == null) {
			gameName = Game.makeGameName(player);
		}

		Game game = new Game(plugin, gameName, bv, player.getName());
		Game.addGame(gameName, game);
		Game.setCurrentGame(player.getName(), game);
		bv.getControlPanel().repaintSignButtons();

		// plugin.persistence.saveGame(game);
		game.autoSave();

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.gameCreated", gameName, bv.getName())); //$NON-NLS-1$ 
	}

	void tryDeleteGame(Player player, String[] args) throws ChessException {
		String gameName = args[2];
		Game game = Game.getGame(gameName);
		gameName = game.getName();
		// allow delete if deleting a game player created
		if (!game.playerCanDelete(player)) {
			ChessPermission.requirePerms(player, ChessPermission.COMMAND_DELGAME);
		}
		String deleter = player == null ? "CONSOLE" : player.getName(); //$NON-NLS-1$
		game.alert(Messages.getString("ChessCommandExecutor.gameDeletedAlert", deleter)); //$NON-NLS-1$
		game.deletePermanently();
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.gameDeleted", gameName)); //$NON-NLS-1$
	}

	void tryCreateBoard(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_NEWBOARD);

		Map<String, String> options = parseCommand(args, 3);

		String name = null;
		if (args.length >= 3) {
			name = args[2];
		} else {
			throw new ChessException("Usage: /chess create board <name> [<options>]"); //$NON-NLS-1$
		}
		String style = options.get("style"); //$NON-NLS-1$
		String pieceStyle = options.get("pstyle"); //$NON-NLS-1$
		@SuppressWarnings("unused")
		// we create this temporary board only to check that the style & piece styles are valid & compatible
		BoardView testBoard = new BoardView(name, plugin, null, style, pieceStyle);

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardCreationPrompt", name)); //$NON-NLS-1$
		plugin.expecter.expectingResponse(player, ExpectAction.BoardCreation,
		                                  new ExpectBoardCreation(plugin, name,style, pieceStyle));
	}

	void tryDeleteBoard(Player player, String[] args) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_DELBOARD);
		if (args.length >= 3) {
			String name = args[2];
			BoardView view = BoardView.getBoardView(name);
			name = view.getName();
			if (view.getGame() == null) {
				view.restoreTerrain(player);
				BoardView.removeBoardView(name);
				plugin.persistence.removeBoardSavefile(view);
				ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardDeleted", name)); //$NON-NLS-1$
			} else {
				ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.boardCantBeDeleted", //$NON-NLS-1$
				                                                   name, view.getGame().getName()));
			}
		}
	}

	void nextPromotionPiece(Player player, int colour, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_PROMOTE);

		if (colour == Chess.WHITE && !player.getName().equals(game.getPlayerWhite())) {
			return;
		}
		if (colour == Chess.BLACK && !player.getName().equals(game.getPlayerBlack())) {
			return;
		}
		game.setPromotionPiece(player.getName(), game.getNextPromotionPiece(colour));
	}

	void doResponse(Player player, boolean isAccepted) throws ChessException {
		ExpectYesNoOffer a = null;
		if (plugin.expecter.isExpecting(player, ExpectAction.DrawResponse)) {
			a = (ExpectYesNoOffer) plugin.expecter.getAction(player, ExpectAction.DrawResponse);
			a.setReponse(isAccepted);
			plugin.expecter.handleAction(player, ExpectAction.DrawResponse);
		} else if (plugin.expecter.isExpecting(player, ExpectAction.SwapResponse)) {
			a = (ExpectYesNoOffer) plugin.expecter.getAction(player, ExpectAction.SwapResponse);
			a.setReponse(isAccepted);
			plugin.expecter.handleAction(player, ExpectAction.SwapResponse);
		}

		if (a != null) {
			a.getGame().getView().getControlPanel().repaintSignButtons();
		}
	}

	void tryInvitePlayer(Player player, Game game, String invitee) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_INVITE);

		if (invitee == null) {
			game.inviteOpen(player.getName());
		} else {
			game.invitePlayer(player.getName(), invitee);
		}
	}

	void tryStartGame(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_START);
		if (game != null) {
			game.start(player.getName());
		}

	}

	void tryResignGame(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_RESIGN);

		if (game != null) {
			game.resign(player.getName());
		}

	}

	void tryArchiveGame(Player player, Game game) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_ARCHIVE);

		File written = game.writePGN(false);
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.PGNarchiveWritten", written.getName())); //$NON-NLS-1$
	}

	void tryChangeStake(Player player, Game game, double stakeIncr) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_STAKE);

		double newStake = game.getStake() + stakeIncr;
		if (newStake < 0.0)
			return;
		if (newStake > Economy.getBalance(player.getName())) {
			newStake = Economy.getBalance(player.getName());
		}

		game.setStake(newStake);
	}

	private static String combine(String[] args, int idx) {
		return combine(args, idx, args.length - 1);
	}

	private static String combine(String[] args, int idx1, int idx2) {
		StringBuilder result = new StringBuilder();
		for (int i = idx1; i <= idx2; ++i) {
			result.append(args[i]);
			if (i < idx2) {
				result.append(" "); //$NON-NLS-1$
			}
		}
		return result.toString();
	}

	private static boolean partialMatch(String[] args, int index, String match) {
		if (index >= args.length) {
			return false;
		}
		return partialMatch(args[index], match);
	}

	private static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) {
			return false;
		}
		return str.substring(0, l).equalsIgnoreCase(match);
	}

	private void notFromConsole(Player p) throws ChessException {
		if (p == null) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.notFromConsole")); //$NON-NLS-1$
		}
	}

	private Map<String, String> parseCommand(String[] args, int start) {
		Map<String, String> res = new HashMap<String, String>();

		Pattern pattern = Pattern.compile("^-(.+)"); //$NON-NLS-1$

		for (int i = start; i < args.length; ++i) {
			Matcher matcher = pattern.matcher(args[i]);
			if (matcher.find()) {
				String opt = matcher.group(1);
				try {
					res.put(opt, args[++i]);
				} catch (ArrayIndexOutOfBoundsException e) {
					res.put(opt, null);
				}
			}
		}
		return res;
	}

	/*-------------------------------------------------------------------------------*/
	private void doTeleport(Player player, Location loc) throws ChessException {
		ChessPermission.requirePerms(player, ChessPermission.COMMAND_TELEPORT);

		plugin.setLastPos(player, player.getLocation());
		player.teleport(loc);
	}
}
