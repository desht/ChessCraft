package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class BoardDeletionCommand extends ChessAbstractCommand {

	public BoardDeletionCommand() {
		super("chess board delete", 1, 1);
		addAlias("chess delete board");
		setPermissionNode("chesscraft.commands.delete.board");
		setUsage("/chess delete board <board-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
		String boardName = bv.getName();
		BoardViewManager.getManager().deleteBoardView(boardName, true);
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardDeleted", boardName)); //$NON-NLS-1$
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getBoardCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
