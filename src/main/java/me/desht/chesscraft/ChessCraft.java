package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.commands.ArchiveCommand;
import me.desht.chesscraft.commands.ClaimVictoryCommand;
import me.desht.chesscraft.commands.CommandManager;
import me.desht.chesscraft.commands.CreateBoardCommand;
import me.desht.chesscraft.commands.CreateGameCommand;
import me.desht.chesscraft.commands.DeleteBoardCommand;
import me.desht.chesscraft.commands.DeleteGameCommand;
import me.desht.chesscraft.commands.FenCommand;
import me.desht.chesscraft.commands.GameCommand;
import me.desht.chesscraft.commands.GetcfgCommand;
import me.desht.chesscraft.commands.InvitePlayerCommand;
import me.desht.chesscraft.commands.JoinCommand;
import me.desht.chesscraft.commands.ListCommand;
import me.desht.chesscraft.commands.MoveCommand;
import me.desht.chesscraft.commands.NoCommand;
import me.desht.chesscraft.commands.OfferDrawCommand;
import me.desht.chesscraft.commands.OfferSwapCommand;
import me.desht.chesscraft.commands.PageCommand;
import me.desht.chesscraft.commands.PromoteCommand;
import me.desht.chesscraft.commands.RedrawCommand;
import me.desht.chesscraft.commands.ReloadCommand;
import me.desht.chesscraft.commands.ResignCommand;
import me.desht.chesscraft.commands.SaveCommand;
import me.desht.chesscraft.commands.SetcfgCommand;
import me.desht.chesscraft.commands.StakeCommand;
import me.desht.chesscraft.commands.StartCommand;
import me.desht.chesscraft.commands.TeleportCommand;
import me.desht.chesscraft.commands.YesCommand;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.desht.chesscraft.log.ChessCraftLogger;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.enums.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectResponse;
import me.desht.chesscraft.expector.ExpectYesNoOffer;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class ChessCraft extends JavaPlugin {

	private static PluginDescriptionFile description;
	private static Map<String, Location> lastPos = new HashMap<String, Location>();
	protected ChessPlayerListener playerListener;
	protected ChessBlockListener blockListener;
	protected ChessEntityListener entityListener;
	public ChessPersistence persistence;
	public static ExpectResponse expecter;
	public ChessConfig config = null;
	public ChessUtils util = null;
	public static Economy economy = null;
	public static Permission permission = null;
	protected static WorldEditPlugin worldEditPlugin = null;
	private static ScrollingMenuSign smsPlugin;
	private static ChessCraft instance;
	private final Map<String, Long> loggedOutAt = new HashMap<String, Long>();
	private final CommandManager cmds = new CommandManager(this);

	/*-----------------------------------------------------------------*/
	@Override
	public void onEnable() {
		instance = this;
		description = this.getDescription();
		util = new ChessUtils();
		ChessConfig.init(this);

		playerListener = new ChessPlayerListener(this);
		blockListener = new ChessBlockListener(this);
		entityListener = new ChessEntityListener(this);

		persistence = new ChessPersistence();
		expecter = new ExpectResponse();
		
		// This is just here so the results DB stuff gets loaded at startup
		// time - easier to test that way.  Remove it for production.
		//		Results.getResultsHandler().addTestData();

		PluginManager pm = getServer().getPluginManager();
		
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			if (setupEconomy()) {
				ChessCraftLogger.info("Detected economy plugin: " + economy.getName());
			}
			if (setupPermission()) {
				ChessCraftLogger.info("Detected permissions plugin: " + permission.getName());
			}
		} else {
			ChessCraftLogger.info("Vault not loaded: no economy support & superperms-only permission support");
		}
		
		setupSMS();
		setupWorldEdit();

		ChessAI.initThreading(this);

		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ANIMATION, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Event.Priority.Normal, this);

		registerCommands();
		
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
		for (ChessGame game : ChessGame.listGames()) {
			game.clockTick();
		}
		getServer().getScheduler().cancelTasks(this);
		persistence.save();
		for (ChessGame game : ChessGame.listGames()) {
			game.deleteTemporary();
		}
		for (BoardView view : BoardView.listBoardViews()) {
			view.deleteTemporary();
		}
		Results.shutdown();
		ChessCraftLogger.log("disabled!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		try {
			return cmds.dispatch(player, command.getName(), args);
		} catch (ChessException e) {
			ChessUtils.errorMessage(player, e.getMessage());
			return true;
		}
	}
	
	private void delayedInitTasks() {
		persistence.reload();
		util.setupRepeatingTask(this, 1);
		if (ChessCraft.getSMS() != null) {
			SMSIntegration.createMenus();
		}
	}

	private void setupSMS() {
		try {
			Plugin p = this.getServer().getPluginManager().getPlugin("ScrollingMenuSign");
			if (p != null && p instanceof ScrollingMenuSign) {
				smsPlugin = (ScrollingMenuSign) p;
				SMSIntegration.setup(smsPlugin);
				ChessCraftLogger.log("ScrollingMenuSign plugin detected.");
			} else {
				ChessCraftLogger.log("ScrollingMenuSign plugin not detected.");
			}
		} catch (NoClassDefFoundError e) {
			// this can happen if ScrollingMenuSign was disabled
			ChessCraftLogger.log("ScrollingMenuSign plugin not detected (NoClassDefFoundError caught).");
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

	private Boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	private Boolean setupPermission() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}

		return (permission != null);
	}

	public static ScrollingMenuSign getSMS() {
		return smsPlugin;
	}

	public static WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	/*-----------------------------------------------------------------*/
	public static void teleportPlayer(Player player, Location loc) {
		setLastPos(player, player.getLocation());
		player.teleport(loc);
	}
	
	public static Location getLastPos(Player player) {
		return lastPos.get(player.getName());
	}

	public static void setLastPos(Player player, Location loc) {
		lastPos.put(player.getName(), loc);
	}

	/*-----------------------------------------------------------------*/

	public ChessPersistence getSaveDatabase(){
		return persistence;
	}

	/*-----------------------------------------------------------------*/
	
	public void playerLeft(String who) {
		loggedOutAt.put(who, System.currentTimeMillis());
	}

	public void playerRejoined(String who) {
		loggedOutAt.remove(who);
	}

	public long getPlayerLeftAt(String who) {
		return loggedOutAt.containsKey(who) ? loggedOutAt.get(who) : 0;
	}
	
	/*-----------------------------------------------------------------*/

	public static ChessCraft getInstance() {
		return instance;
	}
	
	public static void handleExpectedResponse(Player player, boolean isAccepted) throws ChessException {
		ExpectYesNoOffer a = null;
		if (expecter.isExpecting(player, ExpectAction.DrawResponse)) {
			a = (ExpectYesNoOffer) expecter.getAction(player, ExpectAction.DrawResponse);
			a.setReponse(isAccepted);
			expecter.handleAction(player, ExpectAction.DrawResponse);
		} else if (expecter.isExpecting(player, ExpectAction.SwapResponse)) {
			a = (ExpectYesNoOffer) expecter.getAction(player, ExpectAction.SwapResponse);
			a.setReponse(isAccepted);
			expecter.handleAction(player, ExpectAction.SwapResponse);
		}

		if (a != null) {
			a.getGame().getView().getControlPanel().repaintSignButtons();
		}
	}
	
	private void registerCommands() {
		cmds.registerCommand(new ArchiveCommand());
		cmds.registerCommand(new ClaimVictoryCommand());
		cmds.registerCommand(new CreateBoardCommand());
		cmds.registerCommand(new CreateGameCommand());
		cmds.registerCommand(new DeleteBoardCommand());
		cmds.registerCommand(new DeleteGameCommand());
		cmds.registerCommand(new FenCommand());
		cmds.registerCommand(new GameCommand());
		cmds.registerCommand(new GetcfgCommand());
		cmds.registerCommand(new InvitePlayerCommand());
		cmds.registerCommand(new JoinCommand());
		cmds.registerCommand(new ListCommand());
		cmds.registerCommand(new MoveCommand());
		cmds.registerCommand(new NoCommand());
		cmds.registerCommand(new OfferDrawCommand());
		cmds.registerCommand(new OfferSwapCommand());
		cmds.registerCommand(new PageCommand());
		cmds.registerCommand(new PromoteCommand());
		cmds.registerCommand(new RedrawCommand());
		cmds.registerCommand(new ReloadCommand());
		cmds.registerCommand(new ResignCommand());
		cmds.registerCommand(new SaveCommand());
		cmds.registerCommand(new SetcfgCommand());
		cmds.registerCommand(new StakeCommand());
		cmds.registerCommand(new StartCommand());
		cmds.registerCommand(new TeleportCommand());
		cmds.registerCommand(new YesCommand());
	}
}
