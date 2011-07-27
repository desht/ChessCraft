/**
 * Programmer: Jacob Scott
 * Program Name: ChessUtils
 * Description: misc. functions
 * Date: Jul 23, 2011
 */
package me.desht.chesscraft;

import chesspresso.Chess;
import com.sk89q.util.StringUtil;
import java.util.ArrayList;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author jacob
 */
public class ChessUtils {

    private int tickTaskId = -1;
    static ChessCraft plugin = null;
    private static String prevColour = "";

    public ChessUtils(ChessCraft plugin) {
        ChessUtils.plugin = plugin;
    }

    public void setupRepeatingTask(int initialDelay) {
        if (plugin == null) {
            return;
        }
        BukkitScheduler s = plugin.getServer().getScheduler();
        if (tickTaskId != -1) {
            s.cancelTask(tickTaskId);
        }
        tickTaskId = s.scheduleSyncRepeatingTask(plugin, new Runnable() {

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
        }, 20L * initialDelay, 20L * plugin.config.config.getInt("tick_interval", 1));
    }

    public static void errorMessage(Player player, String string) {
        prevColour = ChatColor.RED.toString();
        message(player, string, ChatColor.RED, Level.WARNING);
    }

    public static void statusMessage(Player player, String string) {
        prevColour = ChatColor.AQUA.toString();
        message(player, string, ChatColor.AQUA, Level.INFO);
    }

    public static void alertMessage(Player player, String string) {
        if (player == null) {
            return;
        }
        prevColour = ChatColor.YELLOW.toString();
        message(player, string, ChatColor.YELLOW, Level.INFO);
    }

    public static void generalMessage(Player player, String string) {
        prevColour = ChatColor.WHITE.toString();
        message(player, string, Level.INFO);
    }

    private static void message(Player player, String string, Level level) {
        if (player != null) {
            player.sendMessage(parseColourSpec(string));
        } else {
            ChessCraft.log(level, string);
        }
    }

    private static void message(Player player, String string, ChatColor colour, Level level) {
        if (player != null) {
            player.sendMessage(colour + parseColourSpec(string));
        } else {
            ChessCraft.log(level, string);
        }
    }

    public static String parseColourSpec(String spec) {
        String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7");
        return res.replace("&-", prevColour).replace("&&", "&");
    }

    public static String formatLoc(Location loc) {
        String str = "<" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ","
                + loc.getWorld().getName() + ">";
        return str;
    }

    public static String pieceToStr(int piece) {
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

    public static String[] fuzzyMatch(String search, String set[], int minDist) {
        ArrayList<String> matches = new ArrayList<String>();
        int dist = minDist;
        if (search != null) {
            for (String s : set) {
                if (s != null) {
                    int d = StringUtil.getLevenshteinDistance(s, search);
                    if (d < dist) {
                        dist = d;
                        matches.clear();
                        matches.add(s);
                    } else if (d == dist) {
                        matches.add(s);
                    }
                }
            }
        }
        return matches.toArray(new String[0]);
    }
} // end class ChessUtils

