package me.desht.chesscraft.commands;

import java.util.Map;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class CreateBoardCommand extends AbstractCommand {

	public CreateBoardCommand() {
		super("chess c b", 1, 3);
		setPermissionNode("chesscraft.commands.create.board");
		setUsage("/chess create board <board-name> [-style <style-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {

		String name = args[0];
		Map<String, String> options = parseCommandOptions(args, 1);

		String style = options.get("style"); //$NON-NLS-1$
		String pieceStyle = options.get("pstyle"); //$NON-NLS-1$
		
		@SuppressWarnings("unused")
		// we create this temporary board only to check that the style & piece styles are valid & compatible
		BoardView testBoard = new BoardView(name, null, style, pieceStyle);

		ChessUtils.statusMessage(player, Messages.getString("ChessCommandExecutor.boardCreationPrompt", name)); //$NON-NLS-1$
		ChessCraft.expecter.expectingResponse(player, ExpectAction.BoardCreation,
		                                  new ExpectBoardCreation(name,style, pieceStyle));
		
		return true;
	}

}
