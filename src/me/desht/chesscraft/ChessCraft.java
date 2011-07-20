package me.desht.chesscraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import chesspresso.Chess;

import com.iConomy.*;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

@SuppressWarnings("serial")
public class ChessCraft extends JavaPlugin {

	enum Privilege {
		Basic, Admin
	};

	private PluginDescriptionFile description;
	static final String directory = "plugins" + File.separator + "ChessCraft";
	final Logger logger = Logger.getLogger("Minecraft");

	private PermissionHandler permissionHandler;
	private WorldEditPlugin worldEditPlugin;
	iConomy iConomy = null;

//	private final Map<String, Game> chessGames = new HashMap<String, Game>();
//	private final Map<String, Game> currentGame = new HashMap<String, Game>();
	private final Map<String, Location> lastPos = new HashMap<String, Location>();

	private final ChessPlayerListener playerListener = new ChessPlayerListener(this);
	private final ChessBlockListener blockListener = new ChessBlockListener(this);
	private final ChessEntityListener entityListener = new ChessEntityListener(this);
	private final ChessCommandExecutor commandExecutor = new ChessCommandExecutor(this);

	final ChessPieceLibrary library = new ChessPieceLibrary(this);
	final ChessPersistence persistence = new ChessPersistence(this);
	final ExpectResponse expecter = new ExpectResponse();

	private static String prevColour = "";

	private int tickTaskId;

	private Map<String, Long> loggedOutAt = new HashMap<String, Long>();

	private static final Map<String, Object> configItems = new HashMap<String, Object>() {
		{
			put("autosave", true);
			put("tick_interval", 1);
			put("broadcast_results", true);
			put("auto_delete_finished", 30);
			put("no_building", true);
			put("no_creatures", true);
			put("no_explosions", true);
			put("no_burning", true);
			put("no_pvp", true);
			put("wand_item", "air");
			put("auto_teleport_on_join", true);
			put("timeout_forfeit", 60);
			put("timeout_auto_delete", 180);
			put("stake.default", 0.0);
			put("stake.smallIncrement", 1.0);
			put("stake.largeIncrement", 10.0);
		}
	};

	/*-----------------------------------------------------------------*/

	@Override
	public void onDisable() {
		for (Game game : Game.listGames()) {
			game.clockTick();
		}
		getServer().getScheduler().cancelTasks(this);
		persistence.save();
		logger.info(description.getName() + " version " + description.getVersion() + " is disabled!");
	}

	@Override
	public void onEnable() {
		description = this.getDescription();

		setupDefaultStructure();
		addExtraResources();

		configInitialise();

		setupWorldEdit();
		setupPermissions();
		getCommand("chess").setExecutor(commandExecutor);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ANIMATION, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, new ChessServerListener(this), Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, new ChessServerListener(this), Event.Priority.Monitor, this);

		persistence.reload();

		setupRepeatingTask(2);

		// if upgrading from 0.1, control panels may need to be drawn on the
		// boards
		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				checkControlPanelCreation();
			}
		}) == -1) {
			log(Level.WARNING, "Couldn't schedule startup tasks - multiworld support might not work.");
			checkControlPanelCreation();
		}

		logger.info(description.getName() + " version " + description.getVersion() + " is enabled!");
	}

	private void setupWorldEdit() {
		Plugin p = this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (p != null && p instanceof WorldEditPlugin) {
			worldEditPlugin = (WorldEditPlugin) p;
			log(Level.INFO, "WorldEdit plugin detected - chess board terrain saving enabled.");
		} else {
			log(Level.INFO, "WorldEdit plugin not detected - chess board terrain saving disabled.");
		}
	}

	WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	private void checkControlPanelCreation() {
		for (BoardView bv : BoardView.listBoardViews()) {
			bv.checkControlPanel();
		}
	}

	private void setupRepeatingTask(int initialDelay) {
		tickTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				for (BoardView bv : BoardView.listBoardViews()) {
					bv.doLighting();
				}
				for (Game game : Game.listGames()) {
					game.clockTick();
					game.checkForAutoDelete();
				}
			}
		}, 20L * initialDelay, 20L * getConfiguration().getInt("tick_interval", 1));
	}

	private void setupDefaultStructure() {
		try {
			createDir(null);
			createDir("archive");
			createDir("board_styles");
			createDir("piece_styles");
			createDir("schematics");

			extractResource("/datafiles/board_styles/Standard.yml", "board_styles/Standard.yml");
			extractResource("/datafiles/piece_styles/Standard.yml", "piece_styles/Standard.yml");
		} catch (FileNotFoundException e) {
			log(Level.SEVERE, e.getMessage());
		} catch (IOException e) {
			log(Level.SEVERE, e.getMessage());
		}
	}

	private void addExtraResources() {
		try {
			extractResource("/datafiles/piece_styles/twist.yml", "piece_styles/twist.yml");
		} catch (IOException e) {
			log(Level.SEVERE, e.getMessage());
		}
	}

	void createDir(String dir) {
		File f = dir == null ? getDataFolder() : new File(getDataFolder(), dir);
		if (f.isDirectory())
			return;
		if (!f.mkdir())
			log(Level.WARNING, "Can't make directory " + f.getName());
	}

	private void extractResource(String from, String to) throws IOException {
		File of = new File(getDataFolder(), to);
		if (of.exists())
			return;
		OutputStream out = new FileOutputStream(of);

		InputStream in = this.getClass().getResourceAsStream(from);
		if (in == null)
			throw new IOException("can't extract resource " + from + " from plugin JAR");

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	/*-----------------------------------------------------------------*/

	private void configInitialise() {
		Boolean saveNeeded = false;
		Configuration config = getConfiguration();
		for (String k : configItems.keySet()) {
			if (config.getProperty(k) == null) {
				saveNeeded = true;
				config.setProperty(k, configItems.get(k));
			}
		}
		if (saveNeeded)
			config.save();
	}

	// return a sorted list of all config keys
	List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configItems.keySet()) {
			res.add(k + " = '" + getConfiguration().getString(k) + "'");
		}
		Collections.sort(res);
		return res;
	}

	void setConfigItem(Player player, String key, String val) {
		if (configItems.get(key) == null) {
			errorMessage(player, "No such config key: " + key);
			errorMessage(player, "Use '/chess getcfg' to list all valid keys");
			return;
		}
		if (configItems.get(key) instanceof Boolean) {
			Boolean bVal = false;
			if (val.equals("false") || val.equals("no")) {
				bVal = false;
			} else if (val.equals("true") || val.equals("yes")) {
				bVal = true;
			} else {
				errorMessage(player, "Invalid boolean value " + val + " - use true/yes or false/no.");
				return;
			}
			getConfiguration().setProperty(key, bVal);
		} else if (configItems.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				getConfiguration().setProperty(key, nVal);
			} catch (NumberFormatException e) {
				errorMessage(player, "Invalid numeric value: " + val);
			}
		} else if (configItems.get(key) instanceof Double) {
			try {
				double nVal = Double.parseDouble(val);
				getConfiguration().setProperty(key, nVal);
			} catch (NumberFormatException e) {
				errorMessage(player, "Invalid numeric value: " + val);
			}
		} else {
			getConfiguration().setProperty(key, val);
		}

		// special hooks
		if (key.equalsIgnoreCase("tick_interval")) {
			getServer().getScheduler().cancelTask(tickTaskId);
			setupRepeatingTask(0);
		}

		statusMessage(player, key + " is now set to: " + val);
		getConfiguration().save();
	}

	/*-----------------------------------------------------------------*/

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

	boolean isAllowedTo(Player player, String node) {
		return isAllowedTo(player, node, Privilege.Admin);
	}

	boolean isAllowedTo(Player player, String node, Privilege level) {
		if (player == null)
			return true;
		// if Permissions is in force, then it overrides op status
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return level == Privilege.Basic ? true : player.isOp();
		}
	}

	void requirePerms(Player player, String node, Privilege level) throws ChessException {
		if (isAllowedTo(player, "chesscraft.admin"))
			return;
		if (isAllowedTo(player, "chesscraft.basic") && level == Privilege.Basic)
			return;

		if (!isAllowedTo(player, node, level)) {
			throw new ChessException("You are not allowed to do that.");
		}
	}

	/*-----------------------------------------------------------------*/

	void log(Level level, String message) {
		String logMsg = this.getDescription().getName() + ": " + message;
		logger.log(level, logMsg);
	}

	void errorMessage(Player player, String string) {
		prevColour = ChatColor.RED.toString();
		message(player, string, ChatColor.RED, Level.WARNING);
	}

	void statusMessage(Player player, String string) {
		prevColour = ChatColor.AQUA.toString();
		message(player, string, ChatColor.AQUA, Level.INFO);
	}

	void alertMessage(Player player, String string) {
		if (player == null)
			return;
		prevColour = ChatColor.YELLOW.toString();
		message(player, string, ChatColor.YELLOW, Level.INFO);
	}

	void generalMessage(Player player, String string) {
		prevColour = ChatColor.WHITE.toString();
		message(player, string, Level.INFO);
	}

	private void message(Player player, String string, Level level) {
		if (player != null) {
			player.sendMessage(parseColourSpec(string));
		} else {
			log(level, string);
		}
	}

	private void message(Player player, String string, ChatColor colour, Level level) {
		if (player != null) {
			player.sendMessage(colour + parseColourSpec(string));
		} else {
			log(level, string);
		}
	}

	/*-----------------------------------------------------------------*/

	Location getLastPos(Player player) {
		return lastPos.get(player.getName());
	}

	void setLastPos(Player player, Location loc) {
		lastPos.put(player.getName(), loc);
	}

	/*-----------------------------------------------------------------*/

	void maybeSave() {
		if (getConfiguration().getBoolean("autosave", true))
			persistence.save();
	}

	static String pieceToStr(int piece) {
		switch (piece) {
		case Chess.PAWN:
			return "Pawn";
		case Chess.ROOK:
			return "Rook";
		case Chess.KNIGHT:
			return "Knight";
		case Chess.BISHOP:
			return "Bishop";
		case Chess.KING:
			return "King";
		case Chess.QUEEN:
			return "Queen";
		default:
			return "(unknown)";
		}
	}

	static void setBlock(Block b, MaterialWithData mat) {
		if (mat.data >= 0) {
			b.setTypeIdAndData(mat.material, mat.data, false);
		} else {
			b.setTypeId(mat.material);
		}
	}

	static String formatLoc(Location loc) {
		String str = "<" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ","
				+ loc.getWorld().getName() + ">";
		return str;
	}

	static String parseColourSpec(String spec) {
		String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7");
		return res.replace("&-", prevColour).replace("&&", "&");
	}

	ChessCommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	void playerLeft(String who) {
		loggedOutAt.put(who, new Date().getTime());
	}

	void playerRejoined(String who) {
		loggedOutAt.remove(who);
	}

	long getPlayerLeftAt(String who) {
		return loggedOutAt.containsKey(who) ? loggedOutAt.get(who) : 0;
	}
}
