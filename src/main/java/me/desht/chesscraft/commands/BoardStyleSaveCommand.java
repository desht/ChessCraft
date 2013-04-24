package me.desht.chesscraft.commands;

import java.util.List;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.BoardViewManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardStyleSaveCommand extends ChessAbstractCommand {

	public BoardStyleSaveCommand() {
		super("chess board save", 0, 1);
		setPermissionNode("chesscraft.commands.board.save");
		setUsage("/chess board save [<new-style-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		BoardView bv = BoardViewManager.getManager().partOfChessBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();

		String newStyleName = args.length > 0 ? args[0] : style.getName();

		BoardStyle newStyle = style.saveStyle(newStyleName);
		bv.getChessBoard().setBoardStyle(newStyle, false);
		bv.save();

		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardStyleSaved", bv.getName(), newStyleName));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}
