package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import chesspresso.Chess;

public class CreateGameCommand extends AbstractCommand {

	public CreateGameCommand() {
		super("chess c g", 0, 3);
		setPermissionNode("chesscraft.commands.create.game");
		setUsage("/chess create [-black] [<game-name>] [<board-name>]");
		setOptions("black");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		
		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;
		
		ChessGameManager.getManager().createGame((Player) sender, gameName, boardName, getBooleanOption("black") ? Chess.BLACK : Chess.WHITE);
		
		return true;
	}

}
