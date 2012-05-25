package me.desht.chesscraft.commands;

import java.util.Map;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectBoardCreation;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class BoardCreationCommand extends AbstractCommand {

	public BoardCreationCommand() {
		super("chess b c", 1, 3);
		addAlias("chess c b");	// backwards compat
		setPermissionNode("chesscraft.commands.create.board");
		setUsage("/chess board create <board-name> [-style <style-name>]");
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {

		String name = args[0];
		if (name.startsWith("-")) {
			showUsage(player);
			return true;
		}
		
		Map<String, String> options = parseCommandOptions(args, 1);

		String boardStyleName = options.get("style"); //$NON-NLS-1$
		String pieceStyleName = options.get("pstyle"); //$NON-NLS-1$
		
		// this will throw an exception if the styles are in any way invalid or incompatible
		BoardStyle.verifyCompatibility(boardStyleName, pieceStyleName);

		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.boardCreationPrompt", name)); //$NON-NLS-1$
		ChessCraft.getResponseHandler().expect(player, new ExpectBoardCreation(name, boardStyleName, pieceStyleName));
		
		return true;
	}

}
