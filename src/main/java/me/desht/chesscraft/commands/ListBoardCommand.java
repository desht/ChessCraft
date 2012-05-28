package me.desht.chesscraft.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

public class ListBoardCommand extends AbstractCommand {

	public ListBoardCommand() {
		super("chess l b", 0, 1);
		setPermissionNode("chesscraft.commands.list.board");
		setUsage("/chess list board");
	}
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (BoardView.listBoardViews().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.noBoards")); //$NON-NLS-1$
			return true;
		}

		if (args.length >= 1) {
			BoardView.getBoardView(args[0]).showBoardDetail(sender);
		} else {
			MessagePager pager = MessagePager.getPager(sender).clear();
			for (BoardView bv : BoardView.listBoardViews(true)) {
				String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$
				pager.add(Messages.getString("ChessCommandExecutor.boardList", bv.getName(), ChessUtils.formatLoc(bv.getA1Square()), //$NON-NLS-1$
				                             bv.getBoardStyleName(), gameName));
			}
			pager.showPage();
		}
		return true;
	}

}
