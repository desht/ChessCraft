package me.desht.chesscraft.commands;

import java.io.File;

import org.bukkit.entity.Player;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

public class ArchiveCommand extends AbstractCommand {

	public ArchiveCommand() {
		super("chess archive", 0, 1);
		setPermissionNode("chesscraft.commands.archive");
		setUsage("/chess archive");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		ChessGame game = null;
		if (args.length >= 1) {
			game = ChessGame.getGame(args[0]);
		} else {
			notFromConsole(player);
			game = ChessGame.getCurrentGame(player);
		}
		File written = game.writePGN(false);
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.PGNarchiveWritten", written.getName()));
		return true;
	}

}

