package me.desht.chesscraft;

import com.iConomy.iConomy;

public class Economy {
	private static iConomy iConomyPlugin = null;
	
	static iConomy getiConomyPlugin() {
		return iConomyPlugin;
	}

	static void setiConomyPlugin(iConomy ic) {
		iConomyPlugin = ic;
	}

	static boolean active() {
		if (iConomyPlugin != null)
			return true;
		else
			return false;
	}
	
	static boolean hasEnough(String player, double amount) {
		if (iConomyPlugin != null) {
			return iConomy.getAccount(player).getHoldings().hasEnough(amount);
		} else {
			return true;
		}
	}
	
	static double getBalance(String player) {
		if (iConomyPlugin != null) {
			return iConomy.getAccount(player).getHoldings().balance();
		} else {
			return 0.0;
		}
	}
	
	static void add(String player, double amount) {
		if (iConomyPlugin != null) {
			iConomy.getAccount(player).getHoldings().add(amount);
		} else {
			// nothing
		}
	}
	
	static void subtract(String player, double amount) {
		if (iConomyPlugin != null) {
			iConomy.getAccount(player).getHoldings().subtract(amount);
		} else {
			// nothing
		}
	}
	
	static public String format(Double amount) {
		if (iConomyPlugin != null) {
			return iConomy.format(amount);
		} else {
			return amount.toString(); 
		}
	}
}
