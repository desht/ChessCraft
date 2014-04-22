package me.desht.chesscraft;

import com.comphenix.protocol.ProtocolLibrary;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.desht.chesscraft.chess.*;
import me.desht.chesscraft.chess.ai.AIFactory;
import me.desht.chesscraft.citizens.CitizensUtil;
import me.desht.chesscraft.commands.*;
import me.desht.chesscraft.listeners.*;
import me.desht.chesscraft.results.Results;
import me.desht.dhutils.*;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.nms.NMSHelper;
import me.desht.dhutils.responsehandler.ResponseHandler;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Plotter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChessCraft extends JavaPlugin implements ConfigurationListener, PluginVersionListener {

	private static ChessCraft instance;

	private WorldEditPlugin worldEditPlugin;
	private ChessPersistence persistence;

	public final ResponseHandler responseHandler = new ResponseHandler(this);

	public static Economy economy = null;

	private final CommandManager cmds = new CommandManager(this);

	private final PlayerTracker tracker = new PlayerTracker();

	private ConfigurationManager configManager;
	private ChessFlightListener flightListener;
	private SMSIntegration sms;
	private ChessTickTask tickTask;
	private SpecialFX fx;

	private boolean startupFailed = false;
	private DynmapIntegration dynmapIntegration;
	private boolean protocolLibEnabled;
	private boolean citizensEnabled;

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

		configManager = new ConfigurationManager(this, this);

		Debugger.getInstance().setPrefix("[ChessCraft] ");
		Debugger.getInstance().setLevel(getConfig().getInt("debug_level"));
		Debugger.getInstance().setTarget(getServer().getConsoleSender());

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

		MiscUtil.init(this);
		MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

		new PluginVersionChecker(this, this);

		DirectoryStructure.setup(this);

		Messages.init(getConfig().getString("locale", "default"));

		tickTask = new ChessTickTask();
		persistence = new ChessPersistence();

		// This is just here so the results DB stuff gets loaded at startup
		// time - easier to test that way.  Remove it for production.
		//		Results.getResultsHandler().addTestData();

		// this will cause saved results data to start being pulled in (async)
		Results.getResultsHandler();

		PluginManager pm = getServer().getPluginManager();
		setupVault(pm);
		setupSMS(pm);
		setupWorldEdit(pm);
		setupDynmap(pm);
		setupCitizens2(pm);
		setupProtocolLib(pm);

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

		if (sms != null) {
			sms.setAutosave(true);
		}
		if (dynmapIntegration != null && dynmapIntegration.isEnabled()) {
			dynmapIntegration.setActive(true);
		}
		if (isProtocolLibEnabled()) {
			ProtocolLibIntegration.registerPacketHandler(this);
			ProtocolLibIntegration.setEntityVolume(getConfig().getDouble("entity_volume"));
		}
		tickTask.start(20L);

		setupMetrics();

		Debugger.getInstance().debug("Version " + getDescription().getVersion() + " enable complete");
	}

	@Override
	public void onDisable() {
		// nothing to shut down if we couldn't even start up
		if (startupFailed) return;

		tickTask.cancel();

		flightListener.restoreSpeeds();

		ChessGameManager gm = ChessGameManager.getManager();

		AIFactory.getInstance().clearDown();
		for (ChessGame game : gm.listGames()) {
			game.clockTick();
		}
		getServer().getScheduler().cancelTasks(this);
		persistence.save();
		List<BoardView> views = new ArrayList<BoardView>(BoardViewManager.getManager().listBoardViews());
		for (BoardView view : views) {
			// this will also do a temporary delete on the board's game, if any
			BoardViewManager.getManager().deleteBoardView(view.getName(), false);
		}
		Results.shutdown();

		instance = null;
		economy = null;

		Debugger.getInstance().debug("disable complete");
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
			Debugger.getInstance().debug("Loaded Vault v" + vault.getDescription().getVersion());
			if (!setupEconomy()) {
				LogUtils.warning("No economy plugin detected - game stakes not available");
			}
		} else {
			LogUtils.warning("Vault not loaded: game stakes not available");
		}
	}

	private void setupSMS(PluginManager pm) {
		Plugin p = pm.getPlugin("ScrollingMenuSign");
		if (p != null && p instanceof ScrollingMenuSign && p.isEnabled()) {
			sms = new SMSIntegration((ScrollingMenuSign) p);
			Debugger.getInstance().debug("ScrollingMenuSign plugin detected: ChessCraft menus created.");
		} else {
			Debugger.getInstance().debug("ScrollingMenuSign plugin not detected.");
		}
	}

	private void setupWorldEdit(PluginManager pm) {
		Plugin p = pm.getPlugin("WorldEdit");
		if (p != null && p instanceof WorldEditPlugin && p.isEnabled()) {
			worldEditPlugin = (WorldEditPlugin) p;
			Debugger.getInstance().debug("WorldEdit plugin detected: chess board terrain saving enabled.");
		} else {
			LogUtils.warning("WorldEdit plugin not detected: chess board terrain saving disabled.");
		}
	}

	private void setupDynmap(PluginManager pm) {
		Plugin p = pm.getPlugin("dynmap");
		if (p != null && p.isEnabled()) {
			dynmapIntegration = new DynmapIntegration(this, (DynmapAPI) p);
			Debugger.getInstance().debug("dynmap plugin detected.  Boards and games will be labelled.");
		} else {
			Debugger.getInstance().debug("dynmap plugin not detected.");
		}
	}

	private void setupProtocolLib(PluginManager pm) {
		Plugin pLib = pm.getPlugin("ProtocolLib");
		if (pLib != null && pLib instanceof ProtocolLibrary && pLib.isEnabled()) {
			protocolLibEnabled = true;
			Debugger.getInstance().debug("Hooked ProtocolLib v" + pLib.getDescription().getVersion());
		}
	}

	private void setupCitizens2(PluginManager pm) {
		Plugin citizens = pm.getPlugin("Citizens");
		if (citizens != null && citizens.isEnabled()) {
			citizensEnabled = true;
			Debugger.getInstance().debug("Hooked Citizens2 v" + citizens.getDescription().getVersion());
			CitizensUtil.initCitizens();
		} else {
			LogUtils.warning("Citizens plugin not detected: entity-based chess sets will not be available");
		}
	}

	private Boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	public ChessPersistence getPersistenceHandler() {
		return persistence;
	}

	public WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	private void setInstance(ChessCraft chessCraft) {
		instance = chessCraft;
	}

	public static ChessCraft getInstance() {
		return instance;
	}

	public DynmapIntegration getDynmapIntegration() {
		return dynmapIntegration;
	}

	/**
	 * @return the protocolLibEnabled
	 */
	public boolean isProtocolLibEnabled() {
		return protocolLibEnabled;
	}

	/**
	 * @return the citizensEnabled
	 */
	public boolean isCitizensEnabled() {
		return citizensEnabled;
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
		} else if (key.equals("database.table_prefix") && newVal.toString().isEmpty()) {
			throw new DHUtilsException("'database.table_prefix' may not be empty");
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equalsIgnoreCase("tick_interval")) {
			tickTask.start(0L);
		} else if (key.equalsIgnoreCase("locale")) {
			Messages.setMessageLocale(newVal.toString());
			// redraw control panel signs in the right language
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("debug_level")) {
			Debugger.getInstance().setLevel((Integer) newVal);
		} else if (key.equalsIgnoreCase("teleporting")) {
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("flying.allowed")) {
			flightListener.setEnabled((Boolean) newVal);
		} else if (key.equalsIgnoreCase("flying.captive")) {
			flightListener.setCaptive((Boolean) newVal);
		} else if (key.equalsIgnoreCase("flying.upper_limit") || key.equalsIgnoreCase("flying.outer_limit")) {
			BoardViewManager.getManager().recalculateFlightRegions();
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
		} else if (key.startsWith("dynmap.") && dynmapIntegration != null) {
			dynmapIntegration.processConfig();
			dynmapIntegration.setActive(dynmapIntegration.isEnabled());
		} else if (key.equals("time_control.default")) {
			// force any board which doesn't have a specific time control to update
			// its time control button to the new global default
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				bv.defaultTimeControlChanged();
			}
		} else if (key.equals("entity_volume") && isProtocolLibEnabled()) {
			ProtocolLibIntegration.setEntityVolume((Double) newVal);
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

	public boolean isChessNPC(Entity entity) {
		return entity != null && entity.hasMetadata("NPC");
	}
}
