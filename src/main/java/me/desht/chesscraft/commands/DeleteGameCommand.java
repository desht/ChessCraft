package me.desht.chesscraft.commands;

import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DeleteGameCommand extends ChessAbstractCommand {

	public DeleteGameCommand() {
		super("chess delete game", 1, 1);
		setUsage("/chess delete game <game-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		String gameName = args[0];
		ChessGame game = ChessGameManager.getManager().getGame(gameName);
		gameName = game.getName();

		// bypass permission check if player is deleting their own game and it's still in setup phase
		if (!game.playerCanDelete(sender)) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.delete.game");
		}

		game.alert(Messages.getString("ChessCommandExecutor.gameDeletedAlert", sender.getName())); //$NON-NLS-1$
		ChessGameManager.getManager().deleteGame(gameName, true);
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.gameDeleted", gameName)); //$NON-NLS-1$

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
