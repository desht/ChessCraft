package me.desht.chesscraft.enums;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public enum ChessPermission {

    /**
     * all basic commands
     */
    Basic("chesscraft.basic", true),
    /**
     * admin commands
     */
    Admin("chesscraft.admin"),
    /**
     * claim victory
     */
    COMMAND_WIN("chesscraft.commands.win", true),
    /**
     * "fen" 
     */
    COMMAND_FEN("chesscraft.commands.fen"),
    /**
     * game - get/set active game
     */
    COMMAND_GAME("chesscraft.commands.game", true),
    /**
     * list boards / games
     */
    COMMAND_LIST("chesscraft.commands.list", true),
    /**
     * delete a board or game
     */
    COMMAND_DELETE("chesscraft.commands.delete"),
    /**
     * save all config & data
     */
    COMMAND_SAVE("chesscraft.commands.save"),
    /**
     * reload plugin data
     */
    COMMAND_RELOAD("chesscraft.commands.reload"),
    /**
     * move using commands (not point-click)
     */
    COMMAND_MOVE("chesscraft.commands.move", true),
    /**
     * join a game
     */
    COMMAND_JOIN("chesscraft.commands.join", true),
    /**
     * force a board to be redrawn
     */
    COMMAND_REDRAW("chesscraft.commands.redraw"),
    /**
     * allow players to teleport to/from games
     */
    COMMAND_TELEPORT("chesscraft.commands.tp", true),
    /**
     * set what pawns will be promoted to
     */
    COMMAND_PROMOTE("chesscraft.commands.promote", true),
    /**
     * setup what a game's stake will be
     */
    COMMAND_STAKE("chesscraft.commands.stake", true),
    /**
     * retrieve the current config values
     */
    COMMAND_GETCONFIG("chesscraft.commands.getcfg"),
    /**
     * change the current config values
     */
    COMMAND_SETCONFIG("chesscraft.commands.setcfg"),
    /**
     * offer opposing player to swap sides
     */
    COMMAND_SWAP("chesscraft.commands.offer.swap", true),
    /**
     * offer opposing player to end game in a draw
     */
    COMMAND_DRAW("chesscraft.commands.offer.draw", true),
    /**
     * list currently running games
     */
    COMMAND_LISTGAMES("chesscraft.commands.list.game", true),
    /**
     * list all boards
     */
    COMMAND_LISTBOARDS("chesscraft.commands.list.board", true),
    /**
     * create a new game on a board
     */
    COMMAND_NEWGAME("chesscraft.commands.create.game", true),
    /**
     * delete an existing game
     */
    COMMAND_DELGAME("chesscraft.commands.delete.game"),
    /**
     * create a new board
     */
    COMMAND_NEWBOARD("chesscraft.commands.create.board"),
    /**
     * delete a board
     */
    COMMAND_DELBOARD("chesscraft.commands.delete.board"),
    /**
     * invite a player to join a game
     */
    COMMAND_INVITE("chesscraft.commands.invite", true),
    /**
     * start a game
     */
    COMMAND_START("chesscraft.commands.start", true),
    /**
     * resign from a game
     */
    COMMAND_RESIGN("chesscraft.commands.resign", true),
    /**
     * save a game for later
     * (although only a server op can get the file)
     */
    COMMAND_ARCHIVE("chesscraft.commands.archive", true);
    String permissionNode = null;
    boolean isBasicNode = false;
    private static PermissionHandler permissionHandler;

    ChessPermission(String per) {
        permissionNode = per;
    }

    ChessPermission(String per, boolean isBasic) {
        permissionNode = per;
        isBasicNode = isBasic;
    }

    @Override
    public String toString() {
        return permissionNode;
    }

    /*-----------------------------------------------------------------*/
    public static boolean setupPermissions(Server sv) {        
        if (sv != null && permissionHandler == null) {
            Plugin permissionsPlugin = sv.getPluginManager().getPlugin("Permissions");
            if (permissionsPlugin != null) {
                permissionHandler = ((Permissions) permissionsPlugin).getHandler();
            }
        }
        return permissionHandler != null;
    }
    
    public static boolean isAllowedTo(Player player, ChessPermission node) {
        if (player == null /* || player.isOp() */) {
            return true;
        }
        // if Permissions is in force, then it overrides op status
        if (permissionHandler != null) {
            return permissionHandler.has(player, node.permissionNode);
        } else {
            return node.isBasicNode ? true : player.isOp();
        }
    }

    public static void requirePerms(Player player, ChessPermission node) throws ChessException {
        // TODO? change permission nodes so that these permissions don't override other permissions?
        if (isAllowedTo(player, ChessPermission.Admin)
                || (node.isBasicNode && isAllowedTo(player, ChessPermission.Basic))) {
            return;
        }

        if (!isAllowedTo(player, node)) {
            throw new ChessException("You are not allowed to do that.");
        }
    }
}
