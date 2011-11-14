package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class DeleteBoardCommand extends AbstractCommand {

	public DeleteBoardCommand() {
		super("chess d b", 1, 1);
		setPermissionNode("chesscraft.commands.delete.board");
		setUsage("/chess delete board <board-name>");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		BoardView view = BoardView.getBoardView(args[0]);
		String boardName = view.getName();
		if (view.getGame() == null) {
			view.restoreTerrain(player);
			BoardView.removeBoardView(boardName);
			plugin.persistence.removeBoardSavefile(view);
			ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardDeleted", boardName)); //$NON-NLS-1$
		} else {
			ChessUtils.errorMessage(player, Messages.getString("ChessCommandExecutor.boardCantBeDeleted", //$NON-NLS-1$
			                                                   boardName, view.getGame().getName()));
		}

		return true;
	}

}
