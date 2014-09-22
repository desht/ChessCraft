package me.desht.chesscraft.commands;

import chesspresso.Chess;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CreateGameCommand extends ChessAbstractCommand {

	public CreateGameCommand() {
		super("chess create game", 0, 3);
		setPermissionNode("chesscraft.commands.create.game");
		setUsage("/chess create game [-black] [<game-name>] [<board-name>]");
		setOptions("black");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;

		int colour = getBooleanOption("black") ? Chess.BLACK : Chess.WHITE;
		ChessGame game = ChessGameManager.getManager().createGame((Player) sender, gameName, boardName, colour);
//		if (plugin.getConfig().getBoolean("auto_teleport_on_join")) {
//			game.getPlayer(colour).teleport();
//		} else {
//			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.canTeleport", game.getName())); //$NON-NLS-1$
//		}

		return true;
	}

}
