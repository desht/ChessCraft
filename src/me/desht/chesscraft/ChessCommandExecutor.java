package me.desht.chesscraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.ChessCraft.Privilege;
import me.desht.chesscraft.ExpectResponse.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

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

public class ChessCommandExecutor implements CommandExecutor {

	private ChessCraft plugin;
	private final List<String> messageBuffer = new ArrayList<String>();
	private static int pageSize = 16;
	
	public ChessCommandExecutor(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		
    	if (label.equalsIgnoreCase("chess")) {
    		if (args.length == 0) {
    			return false;
    		}
    		try {
    			if (partialMatch(args[0], "ga")) {			// game
    				gameCommand(player, args);
    			} else if (partialMatch(args[0], "c")) {	// create
    				createCommands(player, args);
    			} else if (partialMatch(args[0], "d")) {	// delete
    				deleteCommands(player, args);
    			} else if (partialMatch(args[0], "l")) {	// list
    				listCommands(player, args);
    			} else if (partialMatch(args[0], "i")) {	// invite
    				inviteCommand(player, args);
    			} else if (partialMatch(args[0], "j")) {	// join
    				joinCommand(player, args);
    			} else if (partialMatch(args[0], "st")) {	// start
    				startCommand(player, args);
    			} else if (partialMatch(args[0], "res")) {	// resign
    				resignCommand(player, args);
    			} else if (partialMatch(args[0], "red")) {	// redraw
    				redrawCommand(player, args);
    			} else if (partialMatch(args[0], "m")) {	// move
    				moveCommand(player, args);
    			} else if (partialMatch(args[0], "pa")) {	// page
    				pagedDisplay(player, args);
    			} else if (partialMatch(args[0], "pr")) {	// promotion
    				promoCommand(player, args);
    			} else if (partialMatch(args[0], "sa")) {	// save
    				saveCommand(player, args);
    			} else if (partialMatch(args[0], "rel")) {	// reload
    				reloadCommand(player, args);
    			} else if (partialMatch(args[0], "t")) {	// tp
    				teleportCommand(player, args);
    			} else if (partialMatch(args[0], "a")) {	// archive
    				archiveCommand(player, args);
    			} else if (partialMatch(args[0], "o")) {	// offer
    				offerCommand(player, args);
    			} else if (partialMatch(args[0], "y")) {	// yes
    				responseCommand(player, args);
    			} else if (partialMatch(args[0], "n")) {	// no
    				responseCommand(player, args);
    			} else if (partialMatch(args[0], "set")) {	// setcfg
    				setcfgCommand(player, args);
    			} else if (partialMatch(args[0], "get")) {	// getcfg
    				getcfgCommand(player, args);
    			} else if (partialMatch(args[0], "fen")) {	// fen
    				fenCommand(player, args);
    			} else if (partialMatch(args[0], "w")) {	// win
    				claimVictoryCommand(player, args);
    			} else {
    				return false;
    			}
    		} catch (IllegalArgumentException e) {
    			plugin.errorMessage(player, e.getMessage());
    		} catch (ChessException e) {
    			plugin.errorMessage(player, e.getMessage());
    		} catch (IllegalMoveException e ) {
    			plugin.errorMessage(player, e.getMessage());
    		}
    	} else {
    		return false;
    	}
		return true;
	}
	
	private void claimVictoryCommand(Player player, String[] args) throws ChessException {
		Game game = plugin.getCurrentGame(player, true);
		
		String other = game.getOtherPlayer(player.getName());
		if (other.isEmpty())
			return;
		
		int timeout = plugin.getConfiguration().getInt("forfeit_timeout", 60);
		long leftAt = plugin.getPlayerLeftAt(other);
		if (leftAt == 0)
			return;
		long now = new Date().getTime();
		long elapsed = (now - leftAt) / 1000;
		if (elapsed >= timeout) {
			game.winByDefault(player.getName());
		} else {
			plugin.statusMessage(player, "You need to wait " + (timeout - elapsed) + " seconds more.");
		}
	}

	private void fenCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.fen", Privilege.Admin);
		
		if (args.length < 2)
			return;
		Game game = plugin.getCurrentGame(player, true);
		
		game.setFen(combine(args, 1));
		
		plugin.statusMessage(player, "Game position for &6" + game.getName() + "&- has been updated.");
		plugin.statusMessage(player, "&f" + Game.getColour(game.getPosition().getToPlay()) + "&- to play.");
		plugin.statusMessage(player, "&4NOTE: &-move history invalidated, this game can no longer be saved.");
	}

	private void gameCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.game", Privilege.Basic);
		
		if (args.length >= 2) {
			plugin.setCurrentGame(player.getName(), args[1]);
			plugin.statusMessage(player, "Your active game is now '" + args[1] + "'.");
		} else {
			Game game = plugin.getCurrentGame(player, false);
			if (game == null) {
				plugin.statusMessage(player, "You have no active game. Use &f/chess game <name>&- to set one.");
			} else {
				plugin.statusMessage(player, "Your active game is &6" + game.getName() + "&-.");
			}
		}
	}

	private void listCommands(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.list", Privilege.Basic);
		
		if (partialMatch(args, 1, "g")) {			// game
			if (args.length > 2) {
				showGameDetail(player, args[2]);
			} else {
				listGames(player);
			}
		} else if (partialMatch(args, 1, "b")) {	// board
			if (args.length > 2) {
				showBoardDetail(player, args[2]);
			} else {
				listBoards(player);
			}
		} else {
			plugin.errorMessage(player, "Usage: /chess list board");
			plugin.errorMessage(player, "       /chess list game");
		}
	}

	private void deleteCommands(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.delete", Privilege.Admin);
		
		if (partialMatch(args, 1, "g")) {			// game
			tryDeleteGame(player, args);
			plugin.maybeSave();
		} else if (partialMatch(args, 1, "b")) {	// board
			tryDeleteBoard(player, args);
			plugin.maybeSave();
		} else {
			plugin.errorMessage(player, "Usage: /chess delete board <board-name>");
			plugin.errorMessage(player, "       /chess delete game <game-name>");
		}
	}

	private void createCommands(Player player, String[] args) throws ChessException {
		if (partialMatch(args, 1, "g")) {			// game
			String gameName  = args.length >= 3 ? args[2] : null;
			String boardName = args.length >= 4 ? args[3] : null;
			tryCreateGame(player, gameName, boardName);
			plugin.maybeSave();
		} else if (partialMatch(args, 1, "b")) {	// board
			tryCreateBoard(player, args);
			plugin.maybeSave();
		} else {
			plugin.errorMessage(player, "Usage: /chess create board <board-name> [-style <style>]");
			plugin.errorMessage(player, "       /chess create game [<game-name>] [<board-name>]");
		}
	}

	private void saveCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.save", Privilege.Admin);
		
		plugin.persistence.save();
		plugin.statusMessage(player, "Chess boards & games have been saved.");
	}
	
	private void reloadCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.reload", Privilege.Admin);
		
		plugin.getConfiguration().load();
		plugin.persistence.reload();
		plugin.statusMessage(player, "Chess boards & games have been reloaded.");
	}

	private void startCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.start", Privilege.Basic);
		
		if (args.length >= 2) {
			plugin.getGame(args[1]).start(player.getName());
		} else {
			Game game = plugin.getCurrentGame(player, true);
			game.start(player.getName());
		}
	}

	private void resignCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.resign", Privilege.Basic);
		
		if (args.length >= 2) {
			plugin.getGame(args[1]).resign(player.getName());
		} else {
			Game game = plugin.getCurrentGame(player, true);
			game.resign(player.getName());
		}
	}

	private void moveCommand(Player player, String[] args) throws IllegalMoveException, ChessException {
		plugin.requirePerms(player, "chesscraft.commands.move", Privilege.Basic);
		
		if (args.length < 2) {
			plugin.errorMessage(player, "Usage: /chess move <from> <to>" + ChatColor.DARK_PURPLE + " (standard algebraic notation)");
			return;
		}
		Game game = plugin.getCurrentGame(player, true);

		String move = combine(args, 1).replaceFirst(" ", "");
		if (move.length() != 4) {
			plugin.errorMessage(player, "Invalid move string '" + move + "'.");
			return;
		}
		int from = Chess.strToSqi(move.substring(0, 2));
		if (from == Chess.NO_SQUARE) {
			plugin.errorMessage(player, "Invalid FROM square in '" + move + "'.");
			return;
		}
		int to = Chess.strToSqi(move.substring(2, 4));
		if (to == Chess.NO_SQUARE) {
			plugin.errorMessage(player, "Invalid TO square in '" + move + "'.");
			return;
		}
		game.setFromSquare(from);
		game.doMove(player.getName(), to);
	}

	private void inviteCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.invite", Privilege.Basic);
		
		Game game = plugin.getCurrentGame(player, true);
		if (args.length >= 2) {
			Player invitee = plugin.getServer().getPlayer(args[1]);
			if (invitee != null) {
				game.invitePlayer(player.getName(), invitee.getName());
				plugin.statusMessage(player, "An invitation has been sent to &6" + args[1] + "&-.");
			} else {
				game.clearInvitation();
				plugin.errorMessage(player, args[1] + " is not online.");
			}
		} else {
			game.inviteOpen(player.getName());
		}
	}

	private void joinCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.join", Privilege.Basic);
		
		String gameName = null;
		if (args.length >= 2) {
			gameName = args[1];
			plugin.getGame(gameName).addPlayer(player.getName());
		} else {
			// find a game (or games) with an invitation for us
			for (Game game : plugin.listGames()) {
				if (game.getInvited().equalsIgnoreCase(player.getName())) {
					game.addPlayer(player.getName());
					gameName = game.getName();
				}
			}
			if (gameName == null)
				throw new ChessException("You don't have any pending invitations right now.");
		}
		
		Game game = plugin.getGame(gameName);
		plugin.setCurrentGame(player.getName(), game);
		int playingAs = game.playingAs(player.getName());
		plugin.statusMessage(player, "You have joined the chess game &6" + gameName + "&-.");
		plugin.statusMessage(player, "You will be playing as &f" + Game.getColour(playingAs) + "&-.");
		
		if (plugin.getConfiguration().getBoolean("auto_teleport_on_join", true)) {
			tryTeleportToGame(plugin.getServer().getPlayer(game.getPlayerWhite()), game);
			tryTeleportToGame(plugin.getServer().getPlayer(game.getPlayerBlack()), game);
		} else {
			plugin.statusMessage(player, "You can teleport to your game with &f/chess tp " + game.getName());
		}
	}

	private void redrawCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.redraw", Privilege.Admin);
		
		if (args.length >= 2) {
			plugin.getBoardView(args[1]).paintAll();
		} else {
			for (BoardView bv : plugin.listBoardViews()) {
				bv.paintAll();
			}
		}
	}

	private void teleportCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.tp", Privilege.Basic);
		
		if (args.length < 2) {
			// back to where we were
			tryTeleportOut(player);
		} else {
			// go to the named game
			Game game = plugin.getGame(args[1]);
			tryTeleportToGame(player, game);
		}
	}

	private void archiveCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.archive", Privilege.Basic);
		
		Game game = plugin.getCurrentGame(player, true);
		File written = game.writePGN(false);
		plugin.statusMessage(player, "Wrote PGN archive to " + written.getName() + ".");
	}

	private void offerCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.offer", Privilege.Basic);
		
		Game game = plugin.getCurrentGame(player, true);
		
		
		if (partialMatch(args, 1, "d")) {			// draw
			tryOfferDraw(player, game);
		} else if (partialMatch(args, 1, "s")) { 	// swap sides
			tryOfferSwap(player, game);
		} else {
			plugin.errorMessage(player, "Usage: /chess offer (draw|swap)");
			return;
		}
	}

	private void responseCommand(Player player, String[] args) throws ChessException {
		boolean isAccepted = partialMatch(args, 0, "y") ? true : false;
		
		if (plugin.expecter.isExpecting(player, ExpectAction.DrawResponse)) {
			ExpectYesNoOffer a = (ExpectYesNoOffer) plugin.expecter.getAction(player, ExpectAction.DrawResponse);
			a.setReponse(isAccepted);
			plugin.expecter.handleAction(player, ExpectAction.DrawResponse);
		} else if (plugin.expecter.isExpecting(player, ExpectAction.SwapResponse)) {
			ExpectYesNoOffer a = (ExpectYesNoOffer) plugin.expecter.getAction(player, ExpectAction.SwapResponse);
			a.setReponse(isAccepted);
			plugin.expecter.handleAction(player, ExpectAction.SwapResponse);
		}
	}

	private void promoCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.promote", Privilege.Basic);
		
		if (args.length >= 2) {
			Game game = plugin.getCurrentGame(player, true);
			int piece = Chess.charToPiece(Character.toUpperCase(args[1].charAt(0)));
			game.setPromotionPiece(player.getName(), piece);
			plugin.statusMessage(player, "Promotion piece for game &6" + game.getName() + "&- has been set to " + ChessCraft.pieceToStr(piece).toUpperCase());
		}
	}

	private void getcfgCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.getcfg", Privilege.Admin);
		
		messageBuffer.clear();
		if (args.length < 2) {
			for (String line : plugin.getConfigList()) {
				messageBuffer.add(line);
			}
			pagedDisplay(player, 1);
		} else {
			String res = plugin.getConfiguration().getString(args[1]);
			if (res != null) {
				plugin.statusMessage(player, args[1] + " = '" + res + "'");
			} else {
				plugin.errorMessage(player, "No such config item " + args[1]);
			}
		}
	}

	private void setcfgCommand(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.setcfg", Privilege.Admin);
		
		if (args.length < 3) {
			plugin.errorMessage(player, "Usage: /chess setcfg <key> <value>");
			return;
		}
		plugin.setConfigItem(player, args[1], combine(args, 2));
	}

	private void doTeleport(Player player, Location loc) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.tp", Privilege.Basic);
		
		plugin.setLastPos(player, player.getLocation());
		player.teleport(loc);
	}

	void tryTeleportToGame(Player player, Game game) throws ChessException {
		if (player == null)
			return;
		BoardView bv = game.getView();
		if (bv.isPartOfBoard(player.getLocation()))
			return;
		
		Location loc;
		if (game.getPlayerWhite().equals(player.getName())) {
			loc = bv.getBounds().getUpperSW().clone();
			loc.setYaw(90.0f);
			loc.add(0.0, 2.0, -(1.0 + 4.5 * bv.getSquareSize()));
		} else if (game.getPlayerBlack().equals(player.getName())) {
			loc = bv.getBounds().getLowerNE().clone();
			loc.setYaw(270.0f);
			loc.add(0.0, 2.0, -1.0 + 4.5 * bv.getSquareSize());
		} else {
			loc = bv.getBounds().getLowerNE().clone();
			loc.setYaw(0.0f);
			loc.add(-4.5 * bv.getSquareSize(), 2.0, 0.0);
		}
		System.out.println("go to " + loc);
		if (loc.getBlock().getTypeId() != 0 || loc.getBlock().getFace(BlockFace.UP).getTypeId() != 0) {
			throw new ChessException("Teleport destination obstructed!");
		}
		doTeleport(player, loc);
		plugin.setCurrentGame(player.getName(), game);
	}

	void tryTeleportOut(Player player) throws ChessException {
		BoardView bv = plugin.partOfChessBoard(player.getLocation());
		Location prev = plugin.getLastPos(player);
		if (bv != null && (prev == null || plugin.partOfChessBoard(prev) == bv)) {
			// try to get the player out of this board safely
			Location loc = bv.findSafeLocationOutside();
			if (loc != null) {
				doTeleport(player, loc);
			} else {
				doTeleport(player, player.getWorld().getSpawnLocation());
				plugin.errorMessage(player, "Can't find a safe place to send you - going to spawn point.");
			}
		} else if (prev != null) {
			// go back to previous location
			doTeleport(player, prev);
		} else {
			plugin.errorMessage(player, "Not on a board");
		}
	}

	void tryOfferSwap(Player player, Game game) throws ChessException {
		String other = game.getOtherPlayer(player.getName());
		if (other.isEmpty()) {
			// no other player yet - just swap
			game.swapColours();
		} else {
			plugin.requirePerms(player, "chesscraft.commands.offer.swap", Privilege.Basic);
			plugin.expecter.expectingResponse(player, ExpectAction.SwapResponse,
					new ExpectYesNoOffer(plugin, game, player.getName(), other), other);
			plugin.statusMessage(player, "You have offered to swap sides with &6" + other + "&-.");
			game.alert(other, "&6" + player.getName() + "&- has offered to swap sides.");
			game.alert(other, "Type &f/chess yes&- to accept, or &f/chess no&- to decline.");
		}
	}

	void tryOfferDraw(Player player, Game game)	throws ChessException {
		String other = game.getOtherPlayer(player.getName());
		if (!player.getName().equals(game.getPlayerToMove()))
			throw new ChessException("You can only offer a draw on your turn.");
		if (game.getState() != GameState.RUNNING) 
			throw new ChessException("The game must be running to offer a draw!");
		plugin.requirePerms(player, "chesscraft.commands.offer.draw", Privilege.Basic);
		plugin.expecter.expectingResponse(player, ExpectAction.DrawResponse,
				new ExpectYesNoOffer(plugin, game, player.getName(), other), other);
		plugin.statusMessage(player, "You have offered a draw to &6" + other + "&-.");
		game.alert(other, "&6" + player.getName() + "&- has offered a draw.");
		game.alert(other, "Type &f/chess yes&- to accept, or &f/chess no&- to decline.");
	}

	void listGames(Player player) throws ChessException {
		messageBuffer.clear();
		for (Game game : plugin.listGames()) {
			String name = game.getName();
			String curGameMarker = "  ";
			if (player != null)
				curGameMarker = game == plugin.getCurrentGame(player) ? "+ " : "  ";
			String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? "&4*&-" : "";
			String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? "&4*&-" : "";
			String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite();
			String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack();
			StringBuilder info = new StringBuilder(": &f" + curMoveW + white + " (W) v " + curMoveB + black + " (B) ");
			info.append("&e[" + game.getState() + "]");
			if (game.getInvited().length() > 0)
				info.append(" invited: &6" + game.getInvited());
			messageBuffer.add(curGameMarker + name + info);
		}
		pagedDisplay(player, 1);
	}
	
	void showGameDetail(Player player, String gameName) throws ChessException {
		Game game = plugin.getGame(gameName);
		
		String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite();
		String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack();
		
		String bullet = ChatColor.DARK_PURPLE + "* " + ChatColor.AQUA;
		messageBuffer.clear();
		messageBuffer.add("&eGame " + gameName + " [" + game.getState() + "] :");
		messageBuffer.add(bullet + "&6" + white + "&- (White) vs. &6" + black + "&- (Black) on board &6" + game.getView().getName());
		messageBuffer.add(bullet + game.getHistory().size() + " half-moves made");
		messageBuffer.add(bullet + (game.getPosition().getToPlay() == Chess.WHITE ? "White" : "Black") + " to play");
		if (game.getState() == GameState.RUNNING) {
			messageBuffer.add(bullet +
					"Clock: White: " + Game.secondsToHMS(game.getTimeWhite()) +
					", Black: " + Game.secondsToHMS(game.getTimeBlack()));
		}
		if (game.getInvited().equals("*"))
			messageBuffer.add(bullet + "Game has an open invitation");
		else if (!game.getInvited().isEmpty()) 
			messageBuffer.add(bullet + "&6" + game.getInvited() + "&- has been invited.  Awaiting response.");
		messageBuffer.add("&eMove history:");
		List<Short> h = game.getHistory();
		for (int i = 0; i < h.size(); i += 2) {
			StringBuilder sb = new StringBuilder(String.format("&f%1$d. &-", (i/2) + 1));
			sb.append(Move.getString(h.get(i)));
			if (i < h.size() - 1) {
				sb.append("  " + Move.getString(h.get(i + 1)));
			}
			messageBuffer.add(sb.toString());
		}
		
		pagedDisplay(player, 1);
	}
	
	void showBoardDetail(Player player, String boardName) throws ChessException {
		BoardView bv = plugin.getBoardView(boardName);
		
		String bullet = ChatColor.LIGHT_PURPLE + "* " + ChatColor.AQUA;
		String w = ChatColor.WHITE.toString();
		Cuboid bounds = bv.getOuterBounds();
		String gameName = bv.getGame() != null ? bv.getGame().getName() : "(none)";
		
		messageBuffer.clear();
		messageBuffer.add(ChatColor.YELLOW + "Board " + boardName + ":");
		messageBuffer.add(bullet + "Lower NE corner: " + w + ChessCraft.formatLoc(bounds.getLowerNE()));
		messageBuffer.add(bullet + "Upper SW corner: " + w + ChessCraft.formatLoc(bounds.getUpperSW()));
		messageBuffer.add(bullet + "Game: " + w + gameName);
		messageBuffer.add(bullet + "Board Style: " + w + bv.getBoardStyle());
		messageBuffer.add(bullet + "Piece Style: " + w + bv.getPieceStyle());
		messageBuffer.add(bullet + "Square size: " + w + bv.getSquareSize() + 
				" (" + bv.getWhiteSquareMat() + "/" + bv.getBlackSquareMat() + ")");
		messageBuffer.add(bullet + "Frame width: " + w + bv.getFrameWidth() + " (" + bv.getFrameMat() + ")");
		messageBuffer.add(bullet + "Enclosure: " + w + bv.getEnclosureMat());
		messageBuffer.add(bullet + "Height: " + w + bv.getHeight());
		messageBuffer.add(bullet + "Lit: " + w + bv.getIsLit());
		
		pagedDisplay(player, 1);
	}

	void listBoards(Player player) {
		messageBuffer.clear();
		for (BoardView bv: plugin.listBoardViews()) {
			String gameName = bv.getGame() != null ? bv.getGame().getName() : "(none)";
			messageBuffer.add("&6" + bv.getName() + ": &-loc=&f" + ChessCraft.formatLoc(bv.getA1Square()) + 
					"&-, style=&6" + bv.getBoardStyle() + "&-, game=&6" + gameName);
		}
		pagedDisplay(player, 1);
	}

	void tryCreateGame(Player player, String gameName, String boardName) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.create.game", Privilege.Basic);
		
		BoardView bv;
		if (boardName == null)
			bv = plugin.getFreeBoard();
		else
			bv = plugin.getBoardView(boardName);
		
		if (gameName == null)
			gameName = plugin.makeGameName(player);
		
		Game game = new Game(plugin, gameName, bv, player.getName());
		plugin.addGame(gameName, game);
		plugin.setCurrentGame(player.getName(), game);
		plugin.statusMessage(player, "Game &6" + gameName + "&- has been created on board &6" + bv.getName() + "&-.");
		plugin.statusMessage(player, "Now type &f/chess invite <playername>&- to invite someone,");
		plugin.statusMessage(player, "or &f/chess invite&- to create an open invitation.");
	}

	void tryDeleteGame(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.delete.game", Privilege.Admin);
		
		String gameName = args[2];
		Game game = plugin.getGame(gameName);
		String deleter = player == null ? "CONSOLE" : player.getName();
		game.alert("Game deleted by " + deleter + "!");
		game.cancelAutoDelete();
		game.getView().setGame(null);
		game.getView().paintAll();
		plugin.removeGame(gameName);
		plugin.statusMessage(player, "Game &6" + gameName + "&- has been deleted.");
	}

	void tryCreateBoard(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.create.board", Privilege.Admin);
		
		Map<String, String>options = parseCommand(args, 3);

		String name = null;
		if (args.length >= 3) {
			name = args[2];
		} else {
			throw new ChessException("Usage: /chess create board <name> [<options>]");
		}
		String style = options.get("style");
		plugin.statusMessage(player, "Left-click a block: create board &6" + name + "&-. Right-click: cancel.");
		plugin.statusMessage(player, "This block will become the centre of the board's A1 square.");
		plugin.expecter.expectingResponse(player, ExpectAction.BoardCreation, new ExpectBoardCreation(plugin, name, style));
	}

	void tryDeleteBoard(Player player, String[] args) throws ChessException {
		plugin.requirePerms(player, "chesscraft.commands.delete.board", Privilege.Admin);
		if (args.length >= 3) {
			String name = args[2];
			BoardView view = plugin.getBoardView(name);
			if (view.getGame() == null) {
				view.wipe();
				plugin.removeBoardView(name);
				plugin.statusMessage(player, "Deleted board &6" + name + "&-.");
			} else {
				plugin.errorMessage(player, "Cannot delete board '" + name + "': it is being used by game '" + view.getGame().getName() + "'.");
			}
		}
	}

	private void pagedDisplay(Player player, String[] args) {
		int pageNum = 1;
		if (args.length < 2) return;
		try {
			pageNum = Integer.parseInt(args[1]);
			pagedDisplay(player, pageNum);
		} catch (NumberFormatException e) {
			plugin.errorMessage(player, "invalid argument '" + args[1] + "'");
		}
	}
	
	private void pagedDisplay(Player player, int pageNum) {
		if (player != null) {
			// pretty paged display
			int nMessages = messageBuffer.size();
			plugin.statusMessage(player, ChatColor.GREEN + "" +  nMessages +
					" lines (page " + pageNum + "/" + ((nMessages-1) / pageSize + 1) + ")");
			plugin.statusMessage(player, ChatColor.GREEN + "---------------");
			for (int i = (pageNum -1) * pageSize; i < nMessages && i < pageNum * pageSize; i++) {
				plugin.statusMessage(player, messageBuffer.get(i));
			}
			plugin.statusMessage(player, ChatColor.GREEN + "---------------");
			String footer = (nMessages > pageSize * pageNum) ? "Use /chess page [page#] to see more" : "";
			plugin.statusMessage(player, ChatColor.GREEN + footer);
		} else {
			// just dump the whole message buffer to the console
			for (String s: messageBuffer) {
				plugin.statusMessage(null, ChatColor.stripColor(s));
			}
		}
	}
	
	private static String combine(String[] args, int idx) {
		return combine(args, idx, args.length - 1);
	}
	private static String combine(String[] args, int idx1, int idx2) {
		StringBuilder result = new StringBuilder();
		for (int i = idx1; i <= idx2; i++) {
			result.append(args[i]);
			if (i < idx2) {
				result.append(" ");
			}
		}
		return result.toString();
	}

	private static boolean partialMatch(String[] args, int index, String match) {
		if (index >= args.length) return false;
		return partialMatch(args[index], match);
	}
	
	private static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) return false;
		return str.substring(0, l).equalsIgnoreCase(match);
	}

//	private boolean onConsole(Player player) {
//		if (player == null) {
//			plugin.errorMessage(player, "This command cannot be run from the console.");
//			return true;
//		} else {
//			return false;
//		}
//	}
	
//	private Location parseLocation(String arglist, Player player) {
//		String s = player == null ? ",worldname" : "";
//		String args[] = arglist.split(",");
//		try {
//			int x = Integer.parseInt(args[0]);
//			int y = Integer.parseInt(args[1]);
//			int z = Integer.parseInt(args[2]);
//			World w = (player == null) ?
//					plugin.getServer().getWorld(args[3]) :
//					player.getWorld();
//			if (w == null) throw new IllegalArgumentException("Unknown world: " + args[3]);
//			return new Location(w, x, y, z);
//		} catch (ArrayIndexOutOfBoundsException e) {
//			throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
//		} catch (NumberFormatException e) {
//			throw new IllegalArgumentException("Invalid location: x,y,z" + s + ".");
//		}
//	}
	
	private Map<String, String> parseCommand(String[] args, int start) {
		Map<String,String> res = new HashMap<String,String>();
		
		Pattern pattern = Pattern.compile("^-(.+)");
	
		for (int i = start; i < args.length; i++) {
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

	
}
