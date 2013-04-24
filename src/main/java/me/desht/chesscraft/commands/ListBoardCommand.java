package me.desht.chesscraft.commands;

import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListBoardCommand extends ChessAbstractCommand {

	public ListBoardCommand() {
		super("chess list board", 0, 1);
		setPermissionNode("chesscraft.commands.list.board");
		setUsage("/chess list board");
	}
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (BoardViewManager.getManager().listBoardViews().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.noBoards")); //$NON-NLS-1$
			return true;
		}

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
		if (args.length >= 1) {
			List<String> l = BoardViewManager.getManager().getBoardView(args[0]).getBoardDetail();
			pager.add(l);
		} else {
			for (BoardView bv : BoardViewManager.getManager().listBoardViewsSorted()) {
				String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$
				pager.add(MessagePager.BULLET + Messages.getString("ChessCommandExecutor.boardList",
				                                                   bv.getName(), MiscUtil.formatLocation(bv.getA1Square()), //$NON-NLS-1$
				                                                   bv.getBoardStyleName(), gameName));
			}
		}
		pager.showPage();
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
