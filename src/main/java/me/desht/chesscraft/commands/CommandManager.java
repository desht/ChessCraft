package me.desht.chesscraft.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.ChessCraft;
import me.desht.dhutils.PermissionUtils;

public class CommandManager {
	private ChessCraft plugin;
	private final List<AbstractCommand> cmdList = new ArrayList<AbstractCommand>();

	public CommandManager(ChessCraft plugin) {
		this.plugin = plugin;
	}
	
	public void registerCommand(AbstractCommand cmd) {
		cmdList.add(cmd);
	}

	public boolean dispatch(Player player, String label, String[] args) throws ChessException {
		boolean res = false;
		for (AbstractCommand cmd : cmdList) {
			if (cmd.matchesSubCommand(label, args)) {
				if (cmd.matchesArgCount(label, args)) {
					PermissionUtils.requirePerms(player, cmd.getPermissionNode());
					String[] actualArgs = cmd.getArgs();
					res = cmd.execute(plugin, player, actualArgs);
				} else {
					cmd.showUsage(player);
					res = true;
				}
				break;
			}
		}
		return res;
	}
}
