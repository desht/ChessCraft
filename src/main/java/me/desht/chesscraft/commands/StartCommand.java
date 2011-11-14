package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

public class StartCommand extends AbstractCommand {

	public StartCommand() {
		super("chess star", 0, 1);
		setPermissionNode("chesscraft.commands.start");
		setUsage("/chess start [<game>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		if (args.length >= 2) {
			ChessGame.getGame(args[0]).start(player.getName());
		} else {
			ChessGame.getCurrentGame(player).start(player.getName());
		}
		return true;
	}

}

