package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ResignCommand extends AbstractCommand {

	public ResignCommand() {
		super("chess res", 0, 1);
		setPermissionNode("chesscraft.commands.resign");
		setUsage("/chess resign [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);
		
		ChessGame game = null;
		if (args.length >= 1) {
			game = ChessGameManager.getManager().getGame(args[0]);
		} else {
			game = ChessGameManager.getManager().getCurrentGame(player.getName(), true);
		}
		
		if (game != null) {
			game.resign(player.getName());
		}

		return true;
	}
}

