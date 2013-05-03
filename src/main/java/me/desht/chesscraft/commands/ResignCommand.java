package me.desht.chesscraft.commands;

import java.util.List;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ResignCommand extends ChessAbstractCommand {

	public ResignCommand() {
		super("chess resign", 0, 1);
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

