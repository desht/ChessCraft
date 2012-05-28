package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardDeletionCommand extends AbstractCommand {

	public BoardDeletionCommand() {
		super("chess b d", 1, 1);
		addAlias("chess d b");
		setPermissionNode("chesscraft.commands.delete.board");
		setUsage("/chess delete board <board-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws ChessException {
		notFromConsole(player);
		BoardView bv = BoardView.getBoardView(args[0]);
		String boardName = bv.getName();
		if (bv.getGame() == null) {
			bv.deletePermanently((Player) player);
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.boardDeleted", boardName)); //$NON-NLS-1$
		} else {
			MiscUtil.errorMessage(player, Messages.getString("ChessCommandExecutor.boardCantBeDeleted", //$NON-NLS-1$
			                                                   boardName, bv.getGame().getName()));
		}

		return true;
	}
}
