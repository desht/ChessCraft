package me.desht.chesscraft.commands;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;

public class ResignCommand extends AbstractCommand {

	public ResignCommand() {
		super("chess resign", 0, 1);
		setPermissionNode("chesscraft.commands.resign");
		setUsage("/chess resign [<game>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		ChessGame game = null;
		if (args.length >= 1) {
			game = ChessGame.getGame(args[0]);
		} else {
			game = ChessGame.getCurrentGame(player, true);
		}
		
		if (game != null) {
			game.resign(player.getName());
		}

		return true;
	}
}

