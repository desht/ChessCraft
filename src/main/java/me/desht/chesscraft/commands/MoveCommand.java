package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;

public class MoveCommand extends AbstractCommand {

	public MoveCommand() {
		super("chess m", 1, 2);
		setPermissionNode("chesscraft.commands.move");
		setUsage("/chess move <from> <to>" + Messages.getString("ChessCommandExecutor.algebraicNotation"));
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		ChessGame game = ChessGame.getCurrentGame(sender.getName(), true);

		String move = combine(args, 0).replaceFirst(" ", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (move.length() != 4) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidMoveString", move)); //$NON-NLS-1$ 
		}
		int from = Chess.strToSqi(move.substring(0, 2));
		if (from == Chess.NO_SQUARE) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidFromSquare", move)); //$NON-NLS-1$
		}
		int to = Chess.strToSqi(move.substring(2, 4));
		if (to == Chess.NO_SQUARE) {
			throw new ChessException(Messages.getString("ChessCommandExecutor.invalidToSquare", move)); //$NON-NLS-1$
		}
		game.setFromSquare(from);
		try {
			game.doMove(sender.getName(), to);
			MiscUtil.statusMessage(sender, Messages.getString("ChessPlayerListener.youPlayed",
			                                                  game.getPosition().getLastMove().getLAN())); //$NON-NLS-1$
		} catch (IllegalMoveException e) {
			throw new ChessException(e.getMessage());
		}

		return true;
	}

}
