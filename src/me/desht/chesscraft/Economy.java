package me.desht.chesscraft;

import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Methods;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Economy {

	protected static Method economyMethod = null;
	protected static Methods _econMethods = new Methods();

	public static void pluginEnable(Plugin p) {
		if (!_econMethods.hasMethod()) {
            if(_econMethods.setMethod(p)) {
                economyMethod = _econMethods.getMethod();
				ChessCraft.log("Using " + economyMethod.getName() + " v" + economyMethod.getVersion() +" for economy");
			}
        }
	}

	public static void pluginDisable(Plugin p) {
        // Check to see if the plugin thats being disabled is the one we are using
        if (_econMethods != null && _econMethods.hasMethod()
				&& _econMethods.checkDisabled(p)) {
                economyMethod = null;
				ChessCraft.log(Level.INFO, " Economy Plugin was disabled.");
        }
	}

	static boolean active() {
		return economyMethod != null;
	}

	public static boolean canAfford(String playerName, double amt) {
		return getBalance(playerName) >= amt;
	}

	public static double getBalance(Player pl) {
		return getBalance(pl.getName());
	}

	public static double getBalance(String playerName) {
		if(economyMethod != null && economyMethod.hasAccount(playerName)){
			return economyMethod.getAccount(playerName).balance();
		}
		return 0;
	}

	public static void addMoney(Player pl, double amt) {
		addMoney(pl.getName(), amt);
	}

	public static void addMoney(String playerName, double amt) {
		if(economyMethod != null){
			if(!economyMethod.hasAccount(playerName)){
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
		if(economyMethod != null){
			if(!economyMethod.hasAccount(playerName)){
				// TODO? add methods for creating an account
				return;
			}
			economyMethod.getAccount(playerName).subtract(amt);
		}
	}

	public static String format(double amt) {
		if(economyMethod != null){
			return economyMethod.format(amt);
		}
		return String.format("%.2f", amt);
	}
}
