package me.desht.chesscraft;

import cosine.boseconomy.BOSEconomy;
import java.util.logging.Level;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Economy {

    protected static com.iConomy.iConomy iConomy = null;
    protected static com.nijiko.coelho.iConomy.iConomy legacyIConomy = null;
    protected static BOSEconomy economy = null;

    public static boolean initEcon(Server sv) {
        Plugin test = null;
        if ((test = sv.getPluginManager().getPlugin("iConomy")) != null) {
            try {
                legacyIConomy = (com.nijiko.coelho.iConomy.iConomy) test;
            } catch (NoClassDefFoundError e) {
                iConomy = (com.iConomy.iConomy) test;
            }
            ChessCraft.log("Using iConomy.");
        } else if ((test = sv.getPluginManager().getPlugin("BOSEconomy")) != null) {
            economy = (BOSEconomy) test;
            ChessCraft.log("Using BOSEconomy");
        } else {
            ChessCraft.log("no supported economy plugin detected");
            return false;
        }
        return true;
    }

    public static void pluginEnable(Plugin p) {
        String main = p.getDescription().getMain();
        try {
            if (main.equals("com.nijiko.coelho.iConomy.iConomy")) {
                legacyIConomy = (com.nijiko.coelho.iConomy.iConomy) p;
            } else if (main.equals("com.iConomy.iConomy")) {
                iConomy = (com.iConomy.iConomy) p;
            } else if (main.equals("cosine.boseconomy.BOSEconomy")) {
                economy = (BOSEconomy) p;
            }
        } catch (NoClassDefFoundError e) {
            ChessCraft.log(Level.SEVERE, "Error loading enabled plugin: " + e);
        }
    }

    public static void pluginDisable(Plugin p) {
        String main = p.getDescription().getMain();
        try {
            if (main.equals("com.nijiko.coelho.iConomy.iConomy")) {
                legacyIConomy = null;
                ChessCraft.log(Level.INFO, "un-hooked from iConomy");
            } else if (main.equals("com.iConomy.iConomy")) {
                iConomy = null;
                ChessCraft.log(Level.INFO, "un-hooked from iConomy");
            } else if (main.equals("cosine.boseconomy.BOSEconomy")) {
                economy = null;
                ChessCraft.log(Level.INFO, "un-hooked from BOSEconomy");
            }
        } catch (NoClassDefFoundError e) {
            ChessCraft.log(Level.SEVERE, "Error loading enabled plugin: " + e);
        }
    }

    static boolean active() {
        return iConomy != null || legacyIConomy != null || economy != null;
    }

    public static boolean canAfford(String playerName, double amt) {
        return getBalance(playerName) >= amt;
    }

    public static double getBalance(Player pl) {
        if (pl != null && hasAccount(pl.getName())) {
            if (legacyIConomy != null) {
                return com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(pl.getName()).getBalance();
            } else if (iConomy != null) {
                //return iConomy.getAccount(pl.getName()).getHoldings().balance();
                return com.iConomy.iConomy.getAccount(pl.getName()).getHoldings().balance();
            } else if (economy != null) {
                return economy.getPlayerMoneyDouble(pl.getName());
            }
        }
        return 0;
    }

    public static double getBalance(String playerName) {
        if (hasAccount(playerName)) {
            if (legacyIConomy != null) {
                return com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(playerName).getBalance();
            } else if (iConomy != null) {
                //return iConomy.getAccount(playerName).getHoldings().balance();
//            if( com.iConomy.iConomy.getAccount(playerName) == null){
//                System.out.println("no account");
//            }else if(com.iConomy.iConomy.getAccount(playerName).getHoldings() == null){
//                System.out.println("no holdongs");
//            }else return
                com.iConomy.iConomy.getAccount(playerName).getHoldings().balance();
            } else if (economy != null) {
                return economy.getPlayerMoneyDouble(playerName);
            }
        }
        return 0;
    }

    public static boolean hasAccount(String playerName) {
        if (legacyIConomy != null) {
            return com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(playerName) != null;
        } else if (iConomy != null) {
            return com.iConomy.iConomy.getAccount(playerName) != null;
        } else if (economy != null) {
            return economy.playerRegistered(playerName, false);
        } else {
            return false;
        }
    }

    public static void addMoney(Player pl, double amt) {
        if(pl!=null){
        if (legacyIConomy != null) {
            if(com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(pl.getName()) == null){
                com.nijiko.coelho.iConomy.iConomy.getBank().addAccount(pl.getName());
            }
            com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(pl.getName()).add(amt);
        } else if (iConomy != null) {
            //iConomy.getAccount(pl.getName()).getHoldings().add(amt);
            if(com.iConomy.iConomy.getAccount(pl.getName()) == null){
                com.iConomy.iConomy.Accounts.create(pl.getName());
            }
            com.iConomy.iConomy.getAccount(pl.getName()).getHoldings().add(amt);
        } else if (economy != null) {
            if(!economy.playerRegistered(pl.getName(), false)){
                economy.registerPlayer(pl.getName());
            }
            economy.addPlayerMoney(pl.getName(), amt, true);
        }}
    }

    public static void addMoney(String playerName, double amt) {
        if(playerName!=null){
        if (legacyIConomy != null) {
            if(com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(playerName) == null){
                com.nijiko.coelho.iConomy.iConomy.getBank().addAccount(playerName);
            }
            com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(playerName).add(amt);
        } else if (iConomy != null) {
            //iConomy.getAccount(playerName).getHoldings().add(amt);
            if(com.iConomy.iConomy.getAccount(playerName) == null){
                com.iConomy.iConomy.Accounts.create(playerName);
            }
            com.iConomy.iConomy.getAccount(playerName).getHoldings().add(amt);
        } else if (economy != null) {
            if(!economy.playerRegistered(playerName, false)){
                economy.registerPlayer(playerName);
            }
            economy.addPlayerMoney(playerName, amt, true);
        }}
    }

    public static void subtractMoney(Player pl, double amt) {
        if (legacyIConomy != null) {
            com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(pl.getName()).subtract(amt);
        } else if (iConomy != null) {
            //iConomy.getAccount(pl.getName()).getHoldings().subtract(amt);
            com.iConomy.iConomy.getAccount(pl.getName()).getHoldings().subtract(amt);
        } else if (economy != null) {
            economy.addPlayerMoney(pl.getName(), -amt, true);
        }
    }

    public static void subtractMoney(String playerName, double amt) {
        if (legacyIConomy != null) {
            com.nijiko.coelho.iConomy.iConomy.getBank().getAccount(playerName).subtract(amt);
        } else if (iConomy != null) {
            //iConomy.getAccount(playerName).getHoldings().subtract(amt);
            com.iConomy.iConomy.getAccount(playerName).getHoldings().subtract(amt);
        } else if (economy != null) {
            economy.addPlayerMoney(playerName, -amt, true);
        }
    }

    public static String format(double amt) {
        if (legacyIConomy != null) {
            com.nijiko.coelho.iConomy.iConomy.getBank().format(amt);
        } else if (iConomy != null) {
            //return iConomy.format(amt);
            return com.iConomy.iConomy.format(amt);
        } else if (economy != null) {
            amt = Math.round(amt);
            if (amt < 1 || amt > 1) {
                return String.valueOf(amt) + " " + economy.getMoneyName();
            } else {
                return String.valueOf(amt) + " " + economy.getMoneyNamePlural();
            }
        }
        return String.format("%.2f", amt);
    }
}
