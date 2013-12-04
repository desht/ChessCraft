package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class GameCommand extends ChessAbstractCommand {

	public GameCommand() {
		super("chess game", 0, 1);
		setPermissionNode("chesscraft.commands.game");
		setUsage("/chess game [<game-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);

		if (args.length >= 1) {
			ChessGameManager.getManager().setCurrentGame(player.getName(), args[0]);
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameChanged", args[0])); //$NON-NLS-1$
		} else {
			ChessGame game = ChessGameManager.getManager().getCurrentGame(player.getName(), false);
			if (game == null) {
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.noActiveGame")); //$NON-NLS-1$
			} else {
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameIs", game.getName())); //$NON-NLS-1$
			}
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
