package me.desht.chesscraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
    			if (partialMatch(args[0], "b")) {			// board
    				boardCommands(player, args);
    			} else if (partialMatch(args[0], "g")) {	// game
    				gameCommands(player, args);
    			} else if (partialMatch(args[0], "i")) {	// invite
    				inviteCommand(player, args);
    			} else if (partialMatch(args[0], "j")) {	// join
    				joinCommand(player, args);
    			} else if (partialMatch(args[0], "st")) {	// start
    				startCommand(player, args);
    			} else if (partialMatch(args[0], "res")) {	// resign
    				resignCommand(player, args);
    			} else if (partialMatch(args[0], "m")) {	// move
    				moveCommand(player, args);
    			} else if (partialMatch(args[0], "p")) {	// page
    				pagedDisplay(player, args);
    			} else if (partialMatch(args[0], "sa")) {	// save
    				saveCommand(player, args);
    			} else if (partialMatch(args[0], "rel")) {	// reload
    				reloadCommand(player, args);
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
	
	private void saveCommand(Player player, String[] args) {
		plugin.persistence.saveAll();
	}
	
	private void reloadCommand(Player player, String[] args) {
		plugin.persistence.reloadAll();
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
			// TODO: usage
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
		int to   = Chess.strToSqi(move.substring(2, 4));
		if (to == Chess.NO_SQUARE) {
			plugin.errorMessage(player, "Invalid TO square in '" + move + "'.");
			return;
		}
		game.setFromSquare(from);
		game.doMove(player, to);
	}

	private void gameCommands(Player player, String[] args) throws ChessException {
		if (partialMatch(args, 1, "c")) {			// create
			String gameName;
			String boardName;
			if (args.length >= 4) {
				boardName = args[3];
			} else {
				boardName = plugin.getFreeBoard();
			}
			if (args.length >= 3) {
				gameName = args[2];
			} else {
				gameName = makeGameName(player);
			}
			Game game = new Game(plugin, gameName, plugin.getBoardView(boardName), player);
			plugin.addGame(gameName, game);
			plugin.setCurrentGame(player, game);
			plugin.statusMessage(player, "Game '" + gameName + "' has been created on board '" + boardName + "'.");
		} else if (partialMatch(args, 1, "l")) {	// list
			messageBuffer.clear();
			for (Game game : plugin.listGames()) {
				String name = game.getName();
				String curGame = name.equals(plugin.getCurrentGame(player).getName()) ? "+ " : "  ";
				String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? ChatColor.RED +  "*" + ChatColor.WHITE : "";
				String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? ChatColor.RED +  "*" + ChatColor.WHITE : "";
				String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite();
				String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack();
				StringBuilder info = new StringBuilder(": " + ChatColor.WHITE + curMoveW + white + " (W) v " + curMoveB + black + " (B) ");
				info.append(ChatColor.YELLOW + "[" + game.getState() + "]");
				if (game.getInvited().length() > 0)
					info.append(" invited: " + game.getInvited());
				messageBuffer.add(curGame + name + info);
			}
			pagedDisplay(player, 1);
		} else if (args.length >= 3 && partialMatch(args, 1, "d")) { 	// delete
			String gameName = args[2];
			Game game = plugin.getGame(gameName);
			game.alert(game.getPlayerWhite(), "Game deleted by " + player.getName() + "!");
			game.alert(game.getPlayerBlack(), "Game deleted by " + player.getName() + "!");
			game.getView().setGame(null);
			game.getView().paintAll();
			plugin.removeGame(gameName);
			plugin.statusMessage(player, "Game '" + gameName + "' has been deleted.");
		} else if (args.length >= 3 && partialMatch(args, 1, "p")) {	// play (set current)
			String gameName = args[2];
			Game game = plugin.getGame(gameName);
			plugin.setCurrentGame(player, game.getName());
			plugin.statusMessage(player, "Game '" + gameName + "' is now your current game.");
		} else if (partialMatch(args, 1, "x")) {	// temp debugging
			Game game = plugin.getGame(args[2]);
			game.show_all();
		} else {
			plugin.errorMessage(player, "Usage: /chess game create [<gamename>] [<boardname>]");
			plugin.errorMessage(player, "       /chess game delete <gamename>");
			plugin.errorMessage(player, "       /chess game play <gamename>");
			plugin.errorMessage(player, "       /chess game list");
		}
	}

	private void boardCommands(Player player, String[] args) throws ChessException {
		if (partialMatch(args, 1, "c")) {		// create
			tryCreateBoard(player, args);
		} else if (partialMatch(args, 1, "d")) {	// delete
			tryDeleteBoard(player, args);
		} else if (partialMatch(args, 1, "l")) {	// list
			messageBuffer.clear();
			for (BoardView bv: plugin.listBoardViews()) {
				StringBuilder info = new StringBuilder();
				info.append(formatLoc(bv.getA1Square()));
				info.append(" square=" + bv.getSquareSize() + " frwidth=" + bv.getFrameWidth() + " height=" + (bv.getHeight() + 2) + " lit=" + bv.getIsLit());
				messageBuffer.add(bv.getName() + ": " + info.toString());
			}
			pagedDisplay(player, 1);
		} else {
			plugin.errorMessage(player, "Usage: /chess board create <boardname> [-style <style>] [-loc <location>]");
			plugin.errorMessage(player, "       /chess board delete <boardname>");
			plugin.errorMessage(player, "       /chess board list");
		}
	}

	private void tryCreateBoard(Player player, String[] args) throws ChessException {
		Map<String, String>options = parseCommand(args, 3);

		String name = args[2];
		Location l = options.containsKey("loc") ? parseLocation(options.get("loc"), player) : player.getLocation();
		String style = options.get("style");
		
		if (!plugin.checkBoardView(name)) {
			// TODO: check it doesn't overlap any other board
			BoardView view = new BoardView(name, plugin, l, style);
			plugin.addBoardView(name, view);
			view.paintAll();
			plugin.statusMessage(player, "Board '" + name + "' has been created at " + formatLoc(view.getA1Square()) + ".");
		} else {
			plugin.errorMessage(player, "Board '" + name + "' already exists.");
		}
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
				plugin.errorMessage(player, invitee + " is not online.");
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
	
	private Location parseLocation(String arglist, Player player) {
		String s = player == null ? ",worldname" : "";
		String args[] = arglist.split(",");
		try {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			int z = Integer.parseInt(args[2]);
			World w = (player == null) ?
					plugin.getServer().getWorld(args[3]) :
					player.getWorld();
			if (w == null) throw new IllegalArgumentException("Unknown world: " + args[3]);
			return new Location(w, x, y, z);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid location: x,y,z" + s + ".");
		}
	}
	
	private Map<String, String> parseCommand(String[] args, int start) {
		Map<String,String> res = new HashMap<String,String>();
		
		Pattern pattern = Pattern.compile("^-(.+)");
	
		for (int i = start; i < args.length; i++) {
			Matcher matcher = pattern.matcher(args[i]);
			if (matcher.find()) {
				String opt = matcher.group();
				try {
					res.put(opt, args[++i]);
				} catch (ArrayIndexOutOfBoundsException e) {
					res.put(opt, null);
				}
			}
		}
		return res;
	}

	private String formatLoc(Location loc) {
		StringBuilder str = new StringBuilder(ChatColor.WHITE + "@ " +
			loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," +
			loc.getWorld().getName());
		return str.toString();
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
