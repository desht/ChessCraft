package me.desht.chesscraft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import chesspresso.Chess;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

@SuppressWarnings("serial")
public class ChessCraft extends JavaPlugin {
	
	static PluginDescriptionFile description;
	static final String directory = "plugins" + File.separator + "ChessCraft";
	final Logger logger = Logger.getLogger("Minecraft");

	PermissionHandler permissionHandler;
	
	ChessPieceLibrary library;
	private final Map<String,Game> chessGames = new HashMap<String,Game>();
	private final Map<String,BoardView> chessBoards = new HashMap<String,BoardView>();
	private final Map<String,Game> currentGame = new HashMap<String,Game>();
	
	private final ChessPlayerListener playerListener = new ChessPlayerListener(this);
	private final ChessBlockListener blockListener = new ChessBlockListener(this);
	private final ChessEntityListener entityListener = new ChessEntityListener(this);
	private final ChessCommandExecutor commandExecutor = new ChessCommandExecutor(this);
	final ChessPersistence persistence = new ChessPersistence(this);
	
	private static final Map<String, Object> configItems = new HashMap<String, Object>() {{
		put("test", true);
	}};
	
	@Override
	public void onDisable() {
		persistence.saveAll();
		logger.info(description.getName() + " version " + description.getVersion() + " is disabled!");
	}

	@Override
	public void onEnable() {
		description = this.getDescription();
		
		configInitialise();
		setupPermissions();
		getCommand("chess").setExecutor(commandExecutor);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
//		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
		
		library = new ChessPieceLibrary(this);
		
		if (!getDataFolder().exists()) getDataFolder().mkdir();
	
		persistence.reloadAll();
		
		logger.info(description.getName() + " version " + description.getVersion() + " is enabled!" );
	}

	private void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionHandler == null) {
			if (permissionsPlugin != null) {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				log(Level.INFO, "Permissions detected");
			} else {
				log(Level.INFO, "Permissions not detected, using ops");
			}
		}
	}

	void log(Level level, String message) {
		String logMsg = this.getDescription().getName() + ": " + message;
		logger.log(level, logMsg);
	}

	private void configInitialise() {
		Boolean saveNeeded = false;
		Configuration config = getConfiguration();
		for (String k : configItems.keySet()) {
			if (config.getProperty(k) == null) {
				saveNeeded = true;
				config.setProperty(k, configItems.get(k));
			}
		}
		if (saveNeeded) config.save();
	}
	
	boolean isAllowedTo(Player player, String node) {
		if (player == null) return true;
		// if Permissions is in force, then it overrides op status
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return player.isOp();
		}
	}
	boolean isAllowedTo(Player player, String node, Boolean okNotOp) {
		if (player == null) return true;
		// if Permissions is in force, then it overrides op status
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return okNotOp ? true : player.isOp();
		}
	}

	void errorMessage(Player player, String string) {
		message(player, string, ChatColor.RED, Level.WARNING);
	}

	void statusMessage(Player player, String string) {
		message(player, string, ChatColor.AQUA, Level.INFO);
	}
	
	void alertMessage(Player player, String string) {
		if (player == null) return;
		message(player, string, ChatColor.YELLOW, Level.INFO);
	}
	
	private void message(Player player, String string, ChatColor colour, Level level) {
		if (player != null) {
			player.sendMessage(colour + string);
		} else {
			log(level, string);
		}
	}

	void addBoardView(String name, BoardView view) {
		chessBoards.put(name, view);
	}
	
	void removeBoardView(String name) {
		chessBoards.remove(name);
	}
	
	Boolean checkBoardView(String name) {
		return chessBoards.containsKey(name);
	}
	
	BoardView getBoardView(String name) throws ChessException {
		if (!chessBoards.containsKey(name))
			throw new ChessException("No such board '" + name + "'");
		return chessBoards.get(name);
	}

	List<BoardView> listBoardViews() {
		SortedSet<String> sorted = new TreeSet<String>(chessBoards.keySet());
		List<BoardView> res = new ArrayList<BoardView>();
		for (String name : sorted) { res.add(chessBoards.get(name)); }
		return res;
	}
	
	public void addGame(String gameName, Game game) {
		chessGames.put(gameName, game);
	}
	
	public void removeGame(String gameName) {
		chessGames.remove(gameName);
	}
	
	boolean checkGame(String name) {
		return chessGames.containsKey(name);
	}
	
	List<Game> listGames() {
		SortedSet<String> sorted = new TreeSet<String>(chessGames.keySet());
		List<Game> res = new ArrayList<Game>();
		for (String name : sorted) { res.add(chessGames.get(name)); }
		return res;
	}
	
	Game getGame(String name) throws ChessException {
		if (!chessGames.containsKey(name))
			throw new ChessException("No such game '" + name + "'");
		return chessGames.get(name);
	}
	
	void setCurrentGame(Player player, String gameName) throws ChessException {
		Game game = getGame(gameName);
		setCurrentGame(player, game);
	}
	void setCurrentGame(Player player, Game game) {
		currentGame.put(player.getName(), game);
	}
	
	Game getCurrentGame(Player player) {
		return currentGame.get(player.getName());
	}
	
	static String pieceToStr(int piece) {
		switch (piece) {
		case Chess.PAWN: return "pawn";
		case Chess.ROOK: return "rook";
		case Chess.KNIGHT: return "knight";
		case Chess.BISHOP: return "bishop";
		case Chess.KING: return "king";
		case Chess.QUEEN: return "queen";
		default: return "(unknown)";
		}
	}

	static MaterialWithData parseIdAndData(String string) {
		String[] items = string.split(":");
		int mat = Integer.parseInt(items[0]);
		byte data = 0;
		if (items.length >= 2)
			data = Byte.parseByte(items[1]);
		return new MaterialWithData(mat, data);
	}
	
	String getFreeBoard() throws ChessException {
		for (BoardView bv: listBoardViews()) {
			if (bv.getGame() == null)
				return bv.getName();
		}
		throw new ChessException("There are no free boards to create a game on.");
	}
	
}
