package me.desht.chesscraft.chess.ai;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author desht
 *
 * This class is responsible for creating and managing the AI definitions and instances.
 * 
 */
public class AIFactory {
	/*
	 * Special character ensures AI name cannot (easily) be faked/hacked, also
	 * adds another level of AI name visibility. Users/admins should NOT be given
	 * control of this prefix - use something else to enable changing AI name
	 * colors, if wanted.
	 */
	public static final String AI_PREFIX = ChatColor.WHITE.toString();

	private static final String DEFS_FILE = "AI_settings.yml";

	private final HashMap<String, AbstractAI> runningAIs = new HashMap<String, AbstractAI>();
	private final Map<String, AIDefinition> allAIs = new HashMap<String, AIDefinition>();

	public static final AIFactory instance = new AIFactory();

	public AIFactory() {
		loadAIDefinitions();
	}

	public AbstractAI getNewAI(ChessGame game, String aiName, boolean isWhiteAI) {
		return getNewAI(game, aiName, false, isWhiteAI);
	}

	public AbstractAI getNewAI(ChessGame game, String aiName, boolean forceNew, boolean isWhiteAI) {
		if (!forceNew) {
			int max = ChessCraft.getInstance().getConfig().getInt("ai.max_ai_games"); //$NON-NLS-1$
			if (max == 0) {
				throw new ChessException(Messages.getString("ChessAI.AIdisabled")); //$NON-NLS-1$
			} else if (runningAIs.size() >= max) {
				throw new ChessException(Messages.getString("ChessAI.noAvailableAIs", max)); //$NON-NLS-1$
			}
		}

		AIDefinition aiDef = getAIDefinition(aiName);
		if (aiDef == null) {
			throw new ChessException(Messages.getString("ChessAI.AInotFound")); //$NON-NLS-1$
		} else if (runningAIs.containsKey(aiDef.getName())) {
			throw new ChessException(Messages.getString("ChessAI.AIbusy")); //$NON-NLS-1$
		}
		AbstractAI ai = aiDef.createInstance(game, isWhiteAI);
		runningAIs.put(aiName, ai);

		return ai;
	}

	void deleteAI(AbstractAI ai) {
		runningAIs.remove(ai.getName());
	}

	/**
	 * Check if the given AI name is available (i.e. not in a game).
	 * 
	 * @param aiName
	 * @return
	 */
	public boolean isAvailable(String aiName) {
		return !runningAIs.containsKey(aiName);
	}

	/**
	 * Clear down all running AIs. Called on disable.
	 */
	public void clearDown() {
		for (Entry<String, AbstractAI> e : runningAIs.entrySet()) {
			e.getValue().delete();
		}
	}

	public List<AIDefinition> listAIDefinitions() {
		return listAIDefinitions(true);
	}
	public List<AIDefinition> listAIDefinitions(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(allAIs.keySet());
			List<AIDefinition> res = new ArrayList<AIDefinition>();
			for (String name : sorted) {
				res.add(allAIs.get(name));
			}
			return res;
		} else {
			return new ArrayList<AIDefinition>(allAIs.values());
		}
	}

	/**
	 * Return the AI definition for the given AI name.  If a null name is passed, return a 
	 * random available AI.
	 * 
	 * @param aiName
	 * @return
	 */
	public AIDefinition getAIDefinition(String aiName) {
		if (aiName.startsWith(AI_PREFIX)) {
			aiName = aiName.substring(AI_PREFIX.length());
		}
		return allAIs.get(aiName);
	}
	public AIDefinition getAIDefinition(String aiName, boolean force) {
		AIDefinition def = getAIDefinition(aiName);
		if (def == null && force) {
			throw new ChessException(Messages.getString("ChessAI.AInotFound"));
		}
		return def;
	}

	/**
	 * Get the name of a random free AI.
	 * 
	 * @return
	 * @throws ChessException if there are no free AIs
	 */
	public String getFreeAIName() {
		List<String> free = new ArrayList<String>();
		for (String k : allAIs.keySet()) {
			if (isAvailable(k)) {
				free.add(k);
			}
		}
		if (free.size() == 0)
			throw new ChessException(Messages.getString("ChessAI.noAvailableAIs", allAIs.size()));
		
		return free.get(new Random().nextInt(free.size()));
	}

	public void loadAIDefinitions() {
		File aiFile = new File(DirectoryStructure.getPluginDirectory(), DEFS_FILE); //$NON-NLS-1$

		if (!aiFile.exists()) {
			LogUtils.severe("AI Loading Error: file not found: " + aiFile); //$NON-NLS-1$
			return;
		}

		YamlConfiguration config = YamlConfiguration.loadConfiguration(aiFile);

		ConfigurationSection n = config.getConfigurationSection("AI"); //$NON-NLS-1$
		if (n == null) {
			LogUtils.severe("AI Loading Error: AI section missing from " + aiFile); //$NON-NLS-1$
			return;
		}

		allAIs.clear();
		for (String a : n.getKeys(false)) {
			ConfigurationSection d = n.getConfigurationSection(a);
			if (n.getBoolean("enabled", true)) { //$NON-NLS-1$
				for (String name : d.getString("funName", a).split(",")) { //$NON-NLS-1$ //$NON-NLS-2$
					if ((name = name.trim()).length() > 0) {
						AIDefinition def;
						try {
							def = new AIDefinition(name, d);
							allAIs.put(name.toLowerCase(), def);
						} catch (ClassNotFoundException e) {
							LogUtils.warning("unknown class '" + d.getString("class") + "' for AI [" + name + "]: skipped");
						} catch (ClassCastException e) {
							LogUtils.warning("class '" + d.getString("class") + "'for AI [" + name + "] is not a AbstractAI subclass: skipped");
						}
					}
				}
			}
		}

		LogUtils.fine("Loaded " + allAIs.size() + " AI definitions from " + DEFS_FILE);
	}

	public static boolean isAIPlayer(String playerName) {
		return playerName.startsWith(AI_PREFIX);
	}

	public static void init() {
	}

	public class AIDefinition {
		private final ConfigurationSection params;
		private final Class<? extends AbstractAI> aiImplClass;
		private final String name;

		public AIDefinition(String name, ConfigurationSection d) throws ClassNotFoundException {
			this.name = name;
			this.params = new MemoryConfiguration();

			String className = d.getString("class", "me.desht.chesscraft.chess.ai.JChecsAI");
			aiImplClass = Class.forName(className).asSubclass(AbstractAI.class);

			for (String k : d.getKeys(false)) {
				params.set(k, d.get(k));
			}

			LogUtils.finer("loaded " + aiImplClass.getName() + " for AI " + name);
		}

		public AbstractAI createInstance(ChessGame game, boolean isWhiteAI) {
			try {
				Constructor<? extends AbstractAI> ctor = aiImplClass.getDeclaredConstructor(String.class, ChessGame.class, Boolean.class, ConfigurationSection.class);
				return ctor.newInstance(name, game, isWhiteAI, params);
			} catch (Exception e) {
				LogUtils.warning("Caught " + e.getClass().getName() + " while loading AI " + name);
				LogUtils.warning("  Exception message: " + e.getMessage());
				e.printStackTrace();
				throw new ChessException("internal error while creating AI " + name);
			}
		}

		public String getImplClassName() {
			return aiImplClass.getSimpleName();
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return AI_PREFIX + name;
		}

		public List<String> getDetails() {
			List<String> res = new ArrayList<String>();
			res.add("AI " + getDisplayName() + " (" + getImplClassName() + ") :");
			for (String k : MiscUtil.asSortedList(params.getKeys(false))) {
				res.add(ChatColor.DARK_RED + "* " + ChatColor.WHITE + k + ": " + ChatColor.YELLOW + params.get(k));
			}
			return res;
		}

		public String getEngine() {
			return getParams().getString("engine");
		}

		public double getPayoutMultiplier() {
			return getParams().getDouble("payout_multiplier", 1.0);
		}

		public String getComment() {
			return getParams().getString("comment");
		}

		public ConfigurationSection getParams() {
			return params;
		}
	}
}
