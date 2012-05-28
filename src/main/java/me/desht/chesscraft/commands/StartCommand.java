package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class StartCommand extends AbstractCommand {

	public StartCommand() {
		super("chess star", 0, 1);
		setPermissionNode("chesscraft.commands.start");
		setUsage("/chess start [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);
		if (args.length >= 2) {
			ChessGame.getGame(args[0]).start(player.getName());
		} else {
			ChessGame.getCurrentGame(player.getName(), true).start(player.getName());
		}
		return true;
	}

}

