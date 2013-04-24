package me.desht.chesscraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.commands.ArchiveCommand;
import me.desht.chesscraft.commands.BoardCreationCommand;
import me.desht.chesscraft.commands.BoardDeletionCommand;
import me.desht.chesscraft.commands.BoardStyleSaveCommand;
import me.desht.chesscraft.commands.BoardStyleSetCommand;
import me.desht.chesscraft.commands.ClaimVictoryCommand;
import me.desht.chesscraft.commands.CreateGameCommand;
import me.desht.chesscraft.commands.DeleteGameCommand;
import me.desht.chesscraft.commands.DesignCommand;
import me.desht.chesscraft.commands.FenCommand;
import me.desht.chesscraft.commands.GameCommand;
import me.desht.chesscraft.commands.GetcfgCommand;
import me.desht.chesscraft.commands.InvitePlayerCommand;
import me.desht.chesscraft.commands.JoinCommand;
import me.desht.chesscraft.commands.ListAICommand;
import me.desht.chesscraft.commands.ListBoardCommand;
import me.desht.chesscraft.commands.ListGameCommand;
import me.desht.chesscraft.commands.ListStylesCommand;
import me.desht.chesscraft.commands.ListTopCommand;
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
import me.desht.chesscraft.commands.TimeControlCommand;
import me.desht.chesscraft.commands.UndoCommand;
import me.desht.chesscraft.commands.YesCommand;
import me.desht.chesscraft.listeners.ChessBlockListener;
import me.desht.chesscraft.listeners.ChessEntityListener;
import me.desht.chesscraft.listeners.ChessFlightListener;
import me.desht.chesscraft.listeners.ChessPlayerListener;
import me.desht.chesscraft.listeners.ChessWorldListener;
import me.desht.chesscraft.results.Results;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.PluginVersionChecker;
import me.desht.dhutils.PluginVersionListener;
import me.desht.dhutils.SpecialFX;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.nms.NMSHelper;
import me.desht.dhutils.responsehandler.ResponseHandler;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Plotter;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class ChessCraft extends JavaPlugin implements ConfigurationListener, PluginVersionListener {

	private static ChessCraft instance;
	private static WorldEditPlugin worldEditPlugin;
	private static ChessPersistence persistence;

	public final ResponseHandler responseHandler = new ResponseHandler();

	public static Economy economy = null;

	private final CommandManager cmds = new CommandManager(this);

	private final PlayerTracker tracker = new PlayerTracker();

	private ConfigurationManager configManager;
	private ChessFlightListener flightListener;
	private SMSIntegration sms;
	private ChessTickTask tickTask;
	private SpecialFX fx;

	private boolean startupFailed = false;

	@Override
	public void onLoad() {
		ConfigurationSerialization.registerClass(BoardView.class);
		ConfigurationSerialization.registerClass(ChessGame.class);
		ConfigurationSerialization.registerClass(TimeControl.class);
		ConfigurationSerialization.registerClass(PersistableLocation.class);
	}

	@Override
	public void onEnable() {
		setInstance(this);

		LogUtils.init(this);

		try {
			NMSHelper.init(this);
		} catch (Exception e) {
			e.printStackTrace();
			String url = getDescription().getWebsite();
			LogUtils.severe("ChessCraft version " + getDescription().getVersion() + " is not compatible with this CraftBukkit version.");
			LogUtils.severe("Check " + url + " for information on updated builds.");
			LogUtils.severe("Plugin disabled.");
			startupFailed = true;
			setEnabled(false);
			return;
		}

		configManager = new ConfigurationManager(this, this);

		MiscUtil.init(this);
		MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

		LogUtils.setLogLevel(getConfig().getString("log_level", "INFO"));

		new PluginVersionChecker(this, this);

		DirectoryStructure.setup();

		AIFactory.init();

		Messages.init(getConfig().getString("locale", "default"));

		tickTask = new ChessTickTask();
		persistence = new ChessPersistence();

		// This is just here so the results DB stuff gets loaded at startup
		// time - easier to test that way.  Remove it for production.
		//		Results.getResultsHandler().addTestData();

		PluginManager pm = getServer().getPluginManager();
		setupVault(pm);
		setupSMS(pm);
		setupWorldEdit(pm);

		new ChessPlayerListener(this);
		new ChessBlockListener(this);
		new ChessEntityListener(this);
		new ChessWorldListener(this);
		flightListener = new ChessFlightListener(this);
		flightListener.setEnabled(getConfig().getBoolean("flying.allowed"));

		registerCommands();

		MessagePager.setPageCmd("/chess page [#|n|p]");
		MessagePager.setDefaultPageSize(getConfig().getInt("pager.lines", 0));

		fx = new SpecialFX(getConfig().getConfigurationSection("effects"));

		persistence.reload();

		if (sms != null)
			sms.setAutosave(true);

		tickTask.start(20L);

		setupMetrics();

		LogUtils.fine("Version " + getDescription().getVersion() + " is enabled!");
	}

	@Override
	public void onDisable() {
		// nothing to shut down if we couldn't even start up
		if (startupFailed) return;

		tickTask.cancel();

		flightListener.restoreSpeeds();

		AIFactory.instance.clearDown();
		for (ChessGame game : ChessGameManager.getManager().listGames()) {
			game.clockTick();
		}
		getServer().getScheduler().cancelTasks(this);
		persistence.save();
		List<ChessGame> games = new ArrayList<ChessGame>(ChessGameManager.getManager().listGames());
		for (ChessGame game : games) {
			game.deleteTemporary();
		}
		List<BoardView> views = new ArrayList<BoardView>(BoardViewManager.getManager().listBoardViews());
		for (BoardView view : views) {
			view.deleteTemporary();
		} 
		Results.shutdown();

		instance = null;
		economy = null;
		worldEditPlugin = null;
		persistence = null;

		LogUtils.fine("disabled!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return cmds.dispatch(sender, command, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		return cmds.onTabComplete(sender, command, label, args);
	}

	private void setupMetrics() {
		if (!getConfig().getBoolean("mcstats")) {
			return;
		}
		try {
			Metrics metrics = new Metrics(this);

			metrics.createGraph("Boards Created").addPlotter(new Plotter() {
				@Override
				public int getValue() { return BoardViewManager.getManager().listBoardViews().size();	}
			});
			metrics.createGraph("Games in Progress").addPlotter(new Plotter() {
				@Override
				public int getValue() { return ChessGameManager.getManager().listGames().size(); }
			});
			metrics.start();
		} catch (IOException e) {
			LogUtils.warning("Can't submit metrics data: " + e.getMessage());
		}
	}

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			LogUtils.fine("Loaded Vault v" + vault.getDescription().getVersion());
			if (!setupEconomy()) {
				LogUtils.warning("No economy plugin detected - game stakes not available");
			}
		} else {
			LogUtils.warning("Vault not loaded: game stakes not available");
		}
	}

	private void setupSMS(PluginManager pm) {
		try {
			Plugin p = pm.getPlugin("ScrollingMenuSign");
			if (p != null && p instanceof ScrollingMenuSign) {
				sms = new SMSIntegration((ScrollingMenuSign) p);
				LogUtils.fine("ScrollingMenuSign plugin detected: ChessCraft menus created.");
			} else {
				LogUtils.fine("ScrollingMenuSign plugin not detected.");
			}
		} catch (NoClassDefFoundError e) {
			// this can happen if ScrollingMenuSign was disabled
			LogUtils.fine("ScrollingMenuSign plugin not detected (NoClassDefFoundError caught).");
		}
	}

	private void setupWorldEdit(PluginManager pm) {
		Plugin p = pm.getPlugin("WorldEdit");
		if (p != null && p instanceof WorldEditPlugin) {
			worldEditPlugin = (WorldEditPlugin) p;
			LogUtils.fine("WorldEdit plugin detected: chess board terrain saving enabled.");
		} else {
			LogUtils.warning("WorldEdit plugin not detected: chess board terrain saving disabled.");
		}
	}

	private Boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	public static ChessPersistence getPersistenceHandler() {
		return persistence;
	}

	public static WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	private void setInstance(ChessCraft chessCraft) {
		instance = chessCraft;
	}

	public static ChessCraft getInstance() {
		return instance;
	}

	public PlayerTracker getPlayerTracker() {
		return tracker;
	}

	public SpecialFX getFX() {
		return fx;
	}

	private void registerCommands() {
		cmds.registerCommand(new ArchiveCommand());
		cmds.registerCommand(new BoardCreationCommand());
		cmds.registerCommand(new BoardDeletionCommand());
		cmds.registerCommand(new BoardStyleSaveCommand());
		cmds.registerCommand(new BoardStyleSetCommand());
		cmds.registerCommand(new ClaimVictoryCommand());
		cmds.registerCommand(new CreateGameCommand());
		cmds.registerCommand(new DeleteGameCommand());
		cmds.registerCommand(new DesignCommand());
		cmds.registerCommand(new FenCommand());
		cmds.registerCommand(new GameCommand());
		cmds.registerCommand(new GetcfgCommand());
		cmds.registerCommand(new InvitePlayerCommand());
		cmds.registerCommand(new JoinCommand());
		cmds.registerCommand(new ListAICommand());
		cmds.registerCommand(new ListGameCommand());
		cmds.registerCommand(new ListStylesCommand());
		cmds.registerCommand(new ListBoardCommand());
		cmds.registerCommand(new ListTopCommand());
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
		cmds.registerCommand(new TimeControlCommand());
		cmds.registerCommand(new UndoCommand());
		cmds.registerCommand(new YesCommand());
	}

	public ConfigurationManager getConfigManager() {
		return configManager;
	}

	/* ConfigurationListener */

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.startsWith("auto_delete.") || key.startsWith("timeout")) {
			String dur = newVal.toString();
			try {
				new Duration(dur);
			} catch (NumberFormatException e) {
				throw new DHUtilsException("Invalid duration: " + dur);
			}
		} else if (key.startsWith("effects.") && getConfig().get(key) instanceof String) {
			// this will throw an IllegalArgumentException if the value is no good
			SpecialFX.SpecialEffect e = fx.new SpecialEffect(newVal.toString(), 1.0f);
			e.play(null);
		} else if (key.equals("version")) {
			throw new DHUtilsException("'version' config item may not be changed");
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equalsIgnoreCase("tick_interval")) { //$NON-NLS-1$
			tickTask.start(0L);
		} else if (key.equalsIgnoreCase("locale")) { //$NON-NLS-1$
			Messages.setMessageLocale(newVal.toString());
			// redraw control panel signs in the right language
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("log_level")) { //$NON-NLS-1$
			LogUtils.setLogLevel(newVal.toString());
		} else if (key.equalsIgnoreCase("teleporting")) { //$NON-NLS-1$
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("flying.allowed")) {
			flightListener.setEnabled((Boolean) newVal);
		} else if (key.equalsIgnoreCase("flying.captive")) {
			flightListener.setCaptive((Boolean) newVal);
		} else if (key.equalsIgnoreCase("flying.upper_limit") || key.equalsIgnoreCase("flying.outer_limit")) {
			flightListener.recalculateFlightRegions();
		} else if (key.equalsIgnoreCase("flying.fly_speed") || key.equalsIgnoreCase("flying.walk_speed")) {
			flightListener.updateSpeeds();
		} else if (key.equalsIgnoreCase("pager.enabled")) {
			if ((Boolean) newVal) {
				MessagePager.setDefaultPageSize();
			} else {
				MessagePager.setDefaultPageSize(Integer.MAX_VALUE);
			}
		} else if (key.startsWith("effects.")) {
			fx = new SpecialFX(getConfig().getConfigurationSection("effects"));
		} else if (key.startsWith("database.")) {
			Results.shutdown();
			if (Results.getResultsHandler() == null) {
				LogUtils.warning("DB connection cannot be re-established.  Check your settings.");
			}
		} else if (key.equals("coloured_console")) {
			MiscUtil.setColouredConsole((Boolean)newVal);
		}
	}

	private void updateAllControlPanels() {
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			bv.getControlPanel().repaintControls();
			bv.getControlPanel().repaintClocks();
		}
	}

	/* PluginVersionListener */

	@Override
	public String getPreviousVersion() {
		return getConfig().getString("version");
	}

	@Override
	public void setPreviousVersion(String currentVersion) {
		getConfig().set("version", getDescription().getVersion());
		saveConfig();
	}

	@Override
	public void onVersionChanged(int oldVersion, int newVersion) {
		boolean changed = false;
		for (String k : getConfig().getConfigurationSection("effects").getKeys(false)) {
			if (getConfig().getString("effects." + k).contains("rawname=")) {
				String newEffect = getConfig().getDefaults().getString("effects." + k);
				LogUtils.info("migrating config setting 'effects." + k + "' => " + newEffect);
				getConfig().set("effects." + k, newEffect);
				changed = true;
			}
		}
		if (changed) {
			saveConfig();
		}
	}
}
