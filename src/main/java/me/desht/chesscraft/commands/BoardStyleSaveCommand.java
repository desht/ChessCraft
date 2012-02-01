package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class BoardStyleSaveCommand extends AbstractCommand {

	public BoardStyleSaveCommand() {
		super("chess b sa", 0, 1);
		setPermissionNode("chesscraft.commands.board.save");
		setUsage("/chess board save [<new-style-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException("You are not standing on a chess board.");
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();

		String newStyleName = args.length > 0 ? args[0] : style.getName();
		
		style.saveStyle(newStyleName);
		bv.getChessBoard().setBoardStyle(newStyleName);
		bv.save();
		
		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardStyleSaved", bv.getName(), newStyleName));
		
		return true;
	}

}
