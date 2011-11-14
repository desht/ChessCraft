package me.desht.chesscraft.util;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public class PermissionUtils {

	/**
	 * Check if the player has the specified permission node.
	 * 
	 * @param player	Player to check
	 * @param node		Node to check for
	 * @return	true if the player has the permission node, false otherwise
	 */
	public static boolean isAllowedTo(Player player, String node) {
		if (player == null || node == null) {
			return true;
		} else {
			if (ChessCraft.permission != null) { 
				return ChessCraft.permission.has(player, node);
			} else { 
				return player.hasPermission(node);
			}
		}
	}

	/**
	 * Throw an exception if the player does not have the specified permission.
	 * 
	 * @param player	Player to check
	 * @param node		Require permission node
	 * @throws SMSException	if the player does not have the node
	 */
	public static void requirePerms(Player player, String node) throws ChessException {
		if (!isAllowedTo(player, node)) {
			throw new ChessException("You are not allowed to do that (need node " + node + ").");
		}
	}
}