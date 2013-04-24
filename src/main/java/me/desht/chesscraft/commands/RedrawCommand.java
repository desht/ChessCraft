package me.desht.chesscraft.commands;

import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RedrawCommand extends ChessAbstractCommand {

	public RedrawCommand() {
		super("chess redraw", 0, 1);
		setPermissionNode("chesscraft.commands.redraw");
		setUsage("/chess redraw [<board-name>]");
		setOptions("all");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		if (args.length >= 1) {
			// redraw named board
			BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
			repaintBoard(bv);
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardRedrawn", bv.getName())); //$NON-NLS-1$
		} else if (getBooleanOption("all")) {
			// redraw ALL boards
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				repaintBoard(bv);
			}
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.allBoardsRedrawn")); //$NON-NLS-1$
		} else {
			// redraw board caller is standing on, if any
			notFromConsole(sender);
			Player player = (Player) sender;
			BoardView bv = BoardViewManager.getManager().partOfChessBoard(player.getLocation());
			if (bv == null) {
				throw new ChessException(Messages.getString("Designer.notOnBoard"));
			}
			repaintBoard(bv);
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardRedrawn", bv.getName())); //$NON-NLS-1$
		}
		return true;
	}

	private void repaintBoard(BoardView bv) {
		bv.reloadStyle();
		bv.paintAll();
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
