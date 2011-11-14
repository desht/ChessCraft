package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public class TeleportCommand extends AbstractCommand {

	public TeleportCommand() {
		super("chess t", 0, 1);
		setPermissionNode("chesscraft.commands.teleport");
		setUsage("/chess tp [<game-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		if (args.length == 0) {
			BoardView.teleportOut(player);
		} else {
			ChessGame game = ChessGame.getGame(args[0]);
			game.summonPlayer(player);
		}
		return true;
	}

}
