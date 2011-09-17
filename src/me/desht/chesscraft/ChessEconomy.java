package me.desht.chesscraft;

import java.util.logging.Level;
import me.desht.chesscraft.log.ChessCraftLogger;

import me.desht.chesscraft.register.payment.Method;
import me.desht.chesscraft.register.payment.Methods;

import org.bukkit.entity.Player;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.PluginManager;

public class ChessEconomy extends ServerListener {

	protected static Method economyMethod = null;
	protected static Methods _econMethods = new Methods();
	protected final ChessCraft plugin;
	PluginManager pm;

	public ChessEconomy(ChessCraft plugin) {
		this.plugin = plugin;
		pm = plugin.getServer().getPluginManager();
	}

	@Override
	public void onPluginDisable(PluginDisableEvent event) {
		// Check to see if the plugin thats being disabled is the one we are using
		if (_econMethods != null && Methods.hasMethod() && Methods.checkDisabled(event.getPlugin())) {
			economyMethod = null;
			ChessCraftLogger.log(Level.INFO, " Economy Plugin was disabled.");
		}
	}

	@Override
	public void onPluginEnable(PluginEnableEvent event) {
		if (!Methods.hasMethod()) {
			if (Methods.setMethod(pm)) {//event.getPlugin())) {
				economyMethod = Methods.getMethod();
				ChessCraftLogger.log("Using " + economyMethod.getName() + " v" + economyMethod.getVersion() + " for economy");
			}
		}
	}

	public static boolean active() {
		return economyMethod != null;
	}

	public static boolean canAfford(String playerName, double amt) {
		return getBalance(playerName) >= amt;
	}

	public static double getBalance(Player pl) {
		return getBalance(pl.getName());
	}

	public static double getBalance(String playerName) {
		if (economyMethod != null && economyMethod.hasAccount(playerName)) {
			return economyMethod.getAccount(playerName).balance();
		}
		return 0;
	}

	public static void addMoney(Player pl, double amt) {
		addMoney(pl.getName(), amt);
	}

	public static void addMoney(String playerName, double amt) {
		if (economyMethod != null) {
			if (!economyMethod.hasAccount(playerName)) {
				// TODO? add methods for creating an account
				return;
			}
			economyMethod.getAccount(playerName).add(amt);
		}
	}

	public static void subtractMoney(Player pl, double amt) {
		subtractMoney(pl.getName(), amt);
	}

	public static void subtractMoney(String playerName, double amt) {
		if (economyMethod != null) {
			if (!economyMethod.hasAccount(playerName)) {
				// TODO? add methods for creating an account
				return;
			}
			economyMethod.getAccount(playerName).subtract(amt);
		}
	}

	public static String format(double amt) {
		if (economyMethod != null) {
			return economyMethod.format(amt);
		}
		return String.format("%.2f", amt);
	}
}
