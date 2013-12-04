package me.desht.chesscraft.commands;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MoveCommand extends ChessAbstractCommand {

	public MoveCommand() {
		super("chess move", 1, 2);
		setPermissionNode("chesscraft.commands.move");
		setUsage(new String[] {
				"/chess move <from> <to>" + Messages.getString("ChessCommandExecutor.algebraicNotation"),
				"/chess move <move>" + Messages.getString("ChessCommandExecutor.sanNotation"),
		});
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);

		ChessGame game = ChessGameManager.getManager().getCurrentGame(sender.getName(), true);

		int from, to;

		String move = combine(args, 0).replaceFirst(" ", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
		if (isSimpleCoordinates(move)) {
			from = Chess.strToSqi(move.substring(0, 2));
			if (from == Chess.NO_SQUARE) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidFromSquare", move)); //$NON-NLS-1$
			}
			to = Chess.strToSqi(move.substring(2, 4));
			if (to == Chess.NO_SQUARE) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidToSquare", move)); //$NON-NLS-1$
			}
		} else {
			// might be a move in SAN format
			Move m = game.getMoveFromSAN(move);
			if (m == null) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidMoveString", move)); //$NON-NLS-1$
			}
			from = m.getFromSqi();
			to = m.getToSqi();
		}
		game.getView().getChessBoard().setSelectedSquare(from);
		try {
			game.doMove(sender.getName(), from, to);
			MiscUtil.statusMessage(sender, Messages.getString("ChessPlayerListener.youPlayed",
			                                                  game.getPosition().getLastMove().getSAN())); //$NON-NLS-1$
		} catch (IllegalMoveException e) {
			throw new ChessException(e.getMessage());
		}

		return true;
	}

	/**
	 * Check if given move is a simple (from,to) coordinate pair, e.g. "e2e4", "e7e5" etc.
	 * @param move
	 * @return
	 */
	private boolean isSimpleCoordinates(String move) {
		if (move.length() != 4)
			return false;

		if (move.charAt(0) < 'a' || move.charAt(0) > 'h')
			return false;
		if (move.charAt(2) < 'a' || move.charAt(2) > 'h')
			return false;
		if (move.charAt(1) < '1' || move.charAt(1) > '8')
			return false;
		if (move.charAt(3) < '1' || move.charAt(3) > '8')
			return false;

		return true;
	}
}
