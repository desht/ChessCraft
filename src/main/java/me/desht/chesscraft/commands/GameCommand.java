package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class GameCommand extends AbstractCommand {

	public GameCommand() {
		super("/chess ga", 0, 1);
		setPermissionNode("chesscraft.commands.game");
		setUsage("/chess game [<game-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);

		if (args.length >= 1) {
			ChessGame.setCurrentGame(player.getName(), args[0]);
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameChanged", args[0])); //$NON-NLS-1$
		} else {
			ChessGame game = ChessGame.getCurrentGame(player, false);
			if (game == null) {
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.noActiveGame")); //$NON-NLS-1$
			} else {
				MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.activeGameIs", game.getName())); //$NON-NLS-1$
			}
		}
		return true;
	}

}
