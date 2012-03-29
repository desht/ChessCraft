package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.PermissionUtils;

import org.bukkit.entity.Player;

public class TeleportCommand extends AbstractCommand {

	public TeleportCommand() {
		super("chess tp", 0, 2);
		setPermissionNode("chesscraft.commands.teleport");
		setUsage("/chess tp [<game-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		switch (args.length) {
		case 0:
			BoardView.teleportOut(player);
			break;
		case 1:
			ChessGame.getGame(args[0]).summonPlayer(player);
			break;
		case 2:
			if (args[1].startsWith("-b")) {
				PermissionUtils.requirePerms(player, "chesscraft.commands.teleport.board");
				BoardView.getBoardView(args[0]).summonPlayer(player);
			}
		}
		return true;
	}

}
