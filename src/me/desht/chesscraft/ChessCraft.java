package me.desht.chesscraft;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import me.desht.chesscraft.enums.ChessPermission;
import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.expector.ExpectResponse;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class ChessCraft extends JavaPlugin {

	private static PluginDescriptionFile description;
	private final Map<String, Location> lastPos = new HashMap<String, Location>();
	protected ChessPlayerListener playerListener;
	protected ChessBlockListener blockListener;
	protected ChessEntityListener entityListener;
	protected ChessCommandExecutor commandExecutor;
	protected ChessPersistence persistence;
	protected ExpectResponse expecter;
	protected ChessEconomy economyPluginListener = new ChessEconomy();
	public ChessConfig config = null;
	public ChessUtils util = null;
	protected static WorldEditPlugin worldEditPlugin = null;
	private static ScrollingMenuSign smsPlugin;

	/*-----------------------------------------------------------------*/
	@Override
	public void onEnable() {
		description = this.getDescription();
		util = new ChessUtils(this);
		ChessConfig.init(this);

		playerListener = new ChessPlayerListener(this);
		blockListener = new ChessBlockListener(this);
		entityListener = new ChessEntityListener(this);
		commandExecutor = new ChessCommandExecutor(this);

		persistence = new ChessPersistence(this);
		expecter = new ExpectResponse();

		// TODO: this is just here so the results DB stuff gets loaded at startup
		// time - easier to test that way.  Remove it for production.
//		Results.getResultsHandler().addTestData();
		
		setupSMS();
		setupWorldEdit();
		if (ChessPermission.setupPermissions(this.getServer())) {
			ChessCraftLogger.log(Level.INFO, "Permissions detected");
		} else {
			ChessCraftLogger.log(Level.INFO, "Permissions not detected, using Bukkit superperms");
		}
		ChessAI.initThreading(this);

		getCommand("chess").setExecutor(commandExecutor);

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ANIMATION, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, economyPluginListener, Event.Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, economyPluginListener, Event.Priority.Monitor, this);

		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

			@Override
			public void run() {
				delayedInitTasks();
			}
		}) == -1) {
			ChessCraftLogger.warning("Couldn't schedule persisted data reloading.  Loading immediately, but multi-world"
					+ "support might not work, and board views may be inconsistent (use /chess redraw to fix).");
			delayedInitTasks();
		}

		ChessCraftLogger.log("Version " + description.getVersion() + " is enabled!");
	}

	@Override
	public void onDisable() {
		ChessAI.clearAI();
		for (Game game : Game.listGames()) {
			game.clockTick();
		}
		getServer().getScheduler().cancelTasks(this);
		persistence.save();
		for (Game game : Game.listGames()) {
			game.deleteTransitory();
		}
		for (BoardView view : BoardView.listBoardViews()) {
			view.delete();
		}
		Results.getResultsHandler().shutdown();
		ChessCraftLogger.log("disabled!");
	}

	private void delayedInitTasks() {
		persistence.reload();
		util.setupRepeatingTask(1);
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.createMenus();
		}
	}
	
	private void setupSMS() {
		Plugin p = this.getServer().getPluginManager().getPlugin("ScrollingMenuSign");
		if (p != null && p instanceof ScrollingMenuSign) {
			smsPlugin = (ScrollingMenuSign) p;
			SMSIntegration.setup(smsPlugin);
			ChessCraftLogger.log("ScrollingMenuSign plugin detected.");
		} else {
			ChessCraftLogger.log("ScrollingMenuSign plugin not detected.");
		}
	}
	
	private void setupWorldEdit() {
		Plugin p = this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (p != null && p instanceof WorldEditPlugin) {
			worldEditPlugin = (WorldEditPlugin) p;
			ChessCraftLogger.log("WorldEdit plugin detected - chess board terrain saving enabled.");
		} else {
			ChessCraftLogger.log("WorldEdit plugin not detected - chess board terrain saving disabled.");
		}
	}

	public static ScrollingMenuSign getSMS() {
		return smsPlugin;
	}
	
	public static WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	/*-----------------------------------------------------------------*/
	Location getLastPos(Player player) {
		return lastPos.get(player.getName());
	}

	void setLastPos(Player player, Location loc) {
		lastPos.put(player.getName(), loc);
	}

	/*-----------------------------------------------------------------*/

    public ChessPersistence getSaveDatabase(){
        return persistence;
    }

	ChessCommandExecutor getCommandExecutor() {
		return commandExecutor;
	}
}
