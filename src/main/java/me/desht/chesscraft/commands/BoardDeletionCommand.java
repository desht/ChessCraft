package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class BoardDeletionCommand extends AbstractCommand {

	public BoardDeletionCommand() {
		super("chess b d", 1, 1);
		addAlias("chess d b");
		setPermissionNode("chesscraft.commands.delete.board");
		setUsage("/chess delete board <board-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
		String boardName = bv.getName();
		bv.deletePermanently();
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardDeleted", boardName)); //$NON-NLS-1$
		return true;
	}
}
