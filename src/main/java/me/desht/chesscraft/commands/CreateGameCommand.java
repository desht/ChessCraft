package me.desht.chesscraft.commands;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import chesspresso.Chess;

public class CreateGameCommand extends AbstractCommand {

	public CreateGameCommand() {
		super("chess c g", 0, 2);
		setPermissionNode("chesscraft.commands.create.game");
		setUsage("/chess create [<game-name>] [<board-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		
		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;
		
		ChessGame.createGame((Player) sender, gameName, boardName, Chess.WHITE);
		
		return true;
	}

}
