package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class StartCommand extends ChessAbstractCommand {

	public StartCommand() {
		super("chess start", 0, 1);
		setPermissionNode("chesscraft.commands.start");
		setUsage("/chess start [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);
		if (args.length >= 1) {
			ChessGameManager.getManager().getGame(args[0]).start(player.getName());
		} else {
			ChessGameManager.getManager().getCurrentGame(player.getName(), true).start(player.getName());
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getPlayerInGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}

