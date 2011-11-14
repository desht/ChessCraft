package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public class CreateGameCommand extends AbstractCommand {

	public CreateGameCommand() {
		super("chess c g", 0, 2);
		setPermissionNode("chesscraft.commands.create.game");
		setUsage("/chess create [<game-name>] [<board-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;
		
		ChessGame.createGame(player, gameName, boardName);
		
		return true;
	}

}
