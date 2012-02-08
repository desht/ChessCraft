package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.pieces.PieceDesigner;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.util.ChessUtils;

import org.bukkit.entity.Player;

public class DesignCommand extends AbstractCommand {

	public DesignCommand() {
		super("chess des");
		setPermissionNode("chesscraft.designer");
		setUsage(new String[] {
				"/chess design",
				"/chess design save <style-name>"
		});
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException("You are not standing on a chess board.");
		}
		if (bv.getGame() != null) {
			throw new ChessException("You cannot design on a board with a game in progress.");
		}
		
		if (args.length == 0) {
			// toggle in and out of design mode
			if (bv.isDesigning()) {
				bv.getChessBoard().setDesigner(null);
				ChessUtils.statusMessage(player, Messages.getString("Designer.outOfDesignMode", bv.getName()));
			} else {
				bv.getChessBoard().setDesigner(new PieceDesigner(bv, bv.getPieceStyleName()));
				ChessUtils.statusMessage(player, Messages.getString("Designer.inDesignMode", bv.getName()));
			}
			bv.paintAll();
		} else if (args[0].equalsIgnoreCase("save")) {
			PieceDesigner designer = bv.getChessBoard().getDesigner();
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.scan();
			designer.save();
			ChessUtils.statusMessage(player, Messages.getString("Designer.styleSaved", designer.getSetName()));
		} else if (args[0].equalsIgnoreCase("load")) {
			PieceDesigner designer = bv.getChessBoard().getDesigner();
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.load();
			ChessUtils.statusMessage(player, Messages.getString("Designer.styleLoaded", designer.getSetName()));
		}
		
		return true;
	}

}
