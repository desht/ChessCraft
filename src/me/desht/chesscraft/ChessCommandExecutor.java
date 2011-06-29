package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.ExpectResponse.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
//import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

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
    		if (!plugin.isAllowedTo(player, "chess.commands." + args[0])) {
    			plugin.errorMessage(player, "You are not allowed to do that.");
    			return true;
    		}
    		try {
    			if (partialMatch(args[0], "g")) {			// game
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
    			} else if (partialMatch(args[0], "p")) {	// page
    				pagedDisplay(player, args);
    			} else if (partialMatch(args[0], "sa")) {	// save
    				saveCommand(player, args);
    			} else if (partialMatch(args[0], "rel")) {	// reload
    				reloadCommand(player, args);
    			} else if (partialMatch(args[0], "t")) {	// tp
    				teleportCommand(player, args);
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
	
	private void gameCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			plugin.setCurrentGame(player.getName(), args[1]);
			plugin.statusMessage(player, "Your active game is now '" + args[1] + "'.");
		} else {
			plugin.errorMessage(player, "Usage: /chess game <game-name>");
		}
	}

	private void listCommands(Player player, String[] args) {
		if (partialMatch(args, 1, "g")) {			// game
			listGames(player);
		} else if (partialMatch(args, 1, "b")) {	// board
			listBoards(player);
		} else {
			plugin.errorMessage(player, "Usage: /chess list board");
			plugin.errorMessage(player, "       /chess list game");
		}
	}

	private void deleteCommands(Player player, String[] args) throws ChessException {
		if (partialMatch(args, 1, "g")) {			// game
			tryDeleteGame(player, args);
		} else if (partialMatch(args, 1, "b")) {	// board
			tryDeleteBoard(player, args);
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
		} else if (partialMatch(args, 1, "b")) {	// board
			tryCreateBoard(player, args);
		} else {
			plugin.errorMessage(player, "Usage: /chess create board <board-name> [-style <style>] [-loc <world,x,y,z>]");
			plugin.errorMessage(player, "       /chess create game [<game-name>] [<board-name>]");
		}
	}

	private void saveCommand(Player player, String[] args) {
		plugin.persistence.save();
		plugin.statusMessage(player, "Chess boards & games have been saved.");
	}
	
	private void reloadCommand(Player player, String[] args) throws ChessException {
		plugin.persistence.reload();
		plugin.statusMessage(player, "Chess boards & games have been reloaded.");
	}

	private void startCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			plugin.getGame(args[1]).start(player);
		} else {
			Game game = plugin.getCurrentGame(player);
			game.start(player);
		}
	}

	private void resignCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			plugin.getGame(args[1]).resign(player);
		} else {
			Game game = plugin.getCurrentGame(player);
			game.resign(player);
		}
	}

	private void moveCommand(Player player, String[] args) throws IllegalMoveException, ChessException {
		if (args.length < 2) {
			plugin.errorMessage(player, "Usage: /chess move <from> <to>" + ChatColor.DARK_PURPLE + " (standard algebraic notation)");
			return;
		}
		Game game = plugin.getCurrentGame(player);
		if (game == null) {
			plugin.errorMessage(player, "You're not playing a game right now so you can't make a move.");
			return;
		}

		String move = combine(args, 1);
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
		game.doMove(player, to);
	}

	private void inviteCommand(Player player, String[] args) throws ChessException {
		Game game = plugin.getCurrentGame(player);
		if (game == null) {
			plugin.errorMessage(player, "You're not playing a game right now so you can't invite anyone.");
			return;
		}
		if (args.length >= 2) {
			Player invitee = plugin.getServer().getPlayer(args[1]);
			if (invitee != null) {
				game.invitePlayer(player, invitee);
				plugin.statusMessage(player, "An invitation has been sent to " + args[1] + ".");
			} else {
				game.clearInvitation();
				plugin.errorMessage(player, args[1] + " is not online.");
			}
		} else {
			game.inviteOpen(player);
		}
	}

	private void joinCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			String gameName = args[1];
			plugin.getGame(gameName).addPlayer(player);
		} else {
			String added = null;
			for (Game game : plugin.listGames()) {
				if (game.getInvited().equalsIgnoreCase(player.getName())) {
					game.addPlayer(player);
					added = game.getName();
					break;
				}
			}
			if (added != null) {
				int playingAs = plugin.getGame(added).playingAs(player.getName());
				plugin.statusMessage(player, "You have been added to the chess game '" + added + "'.");
				plugin.statusMessage(player, "You will be playing as " + Game.getColour(playingAs) + ".");
			} else {
				plugin.errorMessage(player, "You don't have any open invitations.");
			}
		}
	}

	private void redrawCommand(Player player, String[] args) throws ChessException {
		if (args.length >= 2) {
			plugin.getBoardView(args[1]).paintAll();
		} else {
			for (BoardView bv : plugin.listBoardViews()) {
				bv.paintAll();
			}
		}
	}

	private void teleportCommand(Player player, String[] args) throws ChessException {
		if (args.length < 2) {
			// back to where we were
			BoardView bv = plugin.getBoardAt(player.getLocation());
			Location prev = plugin.getLastPos(player);
			if (bv != null && (prev == null || plugin.getBoardAt(prev) == bv)) {
				// try to get the player out of this board safely
				Location loc = bv.findSafeLocationOutside();
				if (loc != null) {
					doTeleport(player, loc);
					return;
				} else {
					plugin.errorMessage(player, "Can't find a safe place to send you!");
					return;
				}
			} else if (prev != null) {
				// go back to previous location
				doTeleport(player, prev);
			} else {
				plugin.errorMessage(player, "Not on a board");
			}
		} else {
			// go to the named game
			Game game = plugin.getGame(args[1]);
			BoardView bv = game.getView();
			Location a1 = bv.getA1Square().clone();
			if (game.getPlayerWhite().equals(player.getName())) {
				a1.setYaw(90.0f);
				a1.add(1.0, 2.0, 1.0 - 4 * bv.getSquareSize());
			} else if (game.getPlayerBlack().equals(player.getName())) {
				a1.setYaw(270.0f);
				a1.add(-1.0 - 8 * bv.getSquareSize(), 2.0, 1.0 - 4 * bv.getSquareSize());
			} else {
				a1.setYaw(0.0f);
				a1.add(1.0 - 4 * bv.getSquareSize(), 2.0, 1.0);
			}
			doTeleport(player, a1);
		}
	}

	private void doTeleport(Player player, Location loc) {
		plugin.setLastPos(player, player.getLocation());
		player.teleport(loc);
	}

	private void listGames(Player player) {
		messageBuffer.clear();
		for (Game game : plugin.listGames()) {
			String name = game.getName();
			String curGameMarker = "  ";
			if (player != null)
				curGameMarker = game == plugin.getCurrentGame(player) ? "+ " : "  ";
			String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? ChatColor.RED +  "*" + ChatColor.WHITE : "";
			String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? ChatColor.RED +  "*" + ChatColor.WHITE : "";
			String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite();
			String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack();
			StringBuilder info = new StringBuilder(": " + ChatColor.WHITE + curMoveW + white + " (W) v " + curMoveB + black + " (B) ");
			info.append(ChatColor.YELLOW + "[" + game.getState() + "]");
			if (game.getInvited().length() > 0)
				info.append(" invited: " + game.getInvited());
			messageBuffer.add(curGameMarker + name + info);
		}
		pagedDisplay(player, 1);
	}

	private void listBoards(Player player) {
		messageBuffer.clear();
		for (BoardView bv: plugin.listBoardViews()) {
			StringBuilder info = new StringBuilder();
			info.append(ChessCraft.formatLoc(bv.getA1Square()));
//			info.append(", bstyle=" + bv.getBoardStyle());
//			info.append(", pstyle=" + bv.getPieceStyle());
			String gameName = bv.getGame() != null ? bv.getGame().getName() : "(none)";
			info.append(", game=" + gameName);
//			info.append(" square=" + bv.getSquareSize() + " frwidth=" + bv.getFrameWidth() + " height=" + (bv.getHeight() + 2) + " lit=" + bv.getIsLit());
			messageBuffer.add(bv.getName() + ": " + info.toString());
		}
		pagedDisplay(player, 1);
	}

	private void tryCreateGame(Player player, String gameName, String boardName) throws ChessException {
		if (boardName == null) boardName = plugin.getFreeBoard();
		if (gameName == null) gameName = makeGameName(player);
		Game game = new Game(plugin, gameName, plugin.getBoardView(boardName), player);
		plugin.addGame(gameName, game);
		plugin.setCurrentGame(player.getName(), game);
		plugin.statusMessage(player, "Game '" + gameName + "' has been created on board '" + boardName + "'.");
	}

	private void tryDeleteGame(Player player, String[] args) throws ChessException {
		String gameName = args[2];
		Game game = plugin.getGame(gameName);
		String deleter = player == null ? "CONSOLE" : player.getName();
		game.alert("Game deleted by " + deleter + "!");
		game.getView().setGame(null);
		game.getView().paintAll();
		plugin.removeGame(gameName);
		plugin.statusMessage(player, "Game '" + gameName + "' has been deleted.");
	}

	private void tryCreateBoard(Player player, String[] args) throws ChessException {
		Map<String, String>options = parseCommand(args, 3);

		String name = args[2];
		String style = options.get("style");
		plugin.statusMessage(player, "Left-click a block: create board.  Right-click: cancel.");
		plugin.statusMessage(player, "This block will become the centre of the board's A1 square.");
		plugin.expecter.expectingResponse(player, ExpectAction.BoardCreation, new ExpectBoardCreation(plugin, name, style));
	}

	private void tryDeleteBoard(Player player, String[] args) throws ChessException {
		if (args.length >= 3) {
			String name = args[2];
			BoardView view = plugin.getBoardView(name);
			if (view.getGame() == null) {
				view.wipe();
				plugin.removeBoardView(name);
				plugin.statusMessage(player, "Deleted board '" + name + "'.");
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
			String footer = (nMessages > pageSize * pageNum) ? "Use /sms page [page#] to see more" : "";
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

	// Generate a game name based on the player's name and a possible index number
	private String makeGameName(Player player) {
		String base = player.getName();
		String res;
		int n = 1;
		do {
			res = base + "-" + n++;
		} while (plugin.checkGame(res));
		
		return res;
	}
}
