package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessConfig;
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
				"/chess design clear",
				"/chess design exit",
				"/chess design save [<style-name>]",
				"/chess design load [<style-name>]",
		});
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		if (bv.getGame() != null) {
			throw new ChessException(Messages.getString("Designer.gameRunning"));
		}

		PieceDesigner designer = bv.getChessBoard().getDesigner();
		if (designer == null && args.length > 0) {
			throw new ChessException(Messages.getString("Designer.mustBeInDesignMode"));
		}
		
		if (args.length == 0) {
			if (bv.isDesigning()) {
				showUsage(player);
				return true;
			} else {
				// toggle into design mode
				designer = new PieceDesigner(bv, bv.getPieceStyleName());
				bv.getChessBoard().setDesigner(designer);
				ChessUtils.statusMessage(player, Messages.getString("Designer.inDesignMode", bv.getName()));
				if (ChessConfig.getConfig().getBoolean("designer.auto_load")) {
					designer.load();
					ChessUtils.statusMessage(player, Messages.getString("Designer.styleLoaded", designer.getSetName()));
				}
			}
			bv.paintAll();
		} else if (args[0].startsWith("e")) {	// exit
			if (bv.isDesigning()) {
				bv.getChessBoard().setDesigner(null);
				ChessUtils.statusMessage(player, Messages.getString("Designer.outOfDesignMode", bv.getName()));
				bv.paintAll();
			}
		} else if (args[0].startsWith("c")) {	// clear
			designer.clear();
			ChessUtils.statusMessage(player, Messages.getString("Designer.cleared", designer.getSetName()));
		} else if (args[0].startsWith("s")) {	// save
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.scan();
			designer.save();
			ChessUtils.statusMessage(player, Messages.getString("Designer.styleSaved", designer.getSetName()));
		} else if (args[0].startsWith("l")) {	// load
			if (args.length >= 2) {
				designer.setSetName(args[1]);
			}
			designer.load();
			ChessUtils.statusMessage(player, Messages.getString("Designer.styleLoaded", designer.getSetName()));
		}
		
		return true;
	}

}
