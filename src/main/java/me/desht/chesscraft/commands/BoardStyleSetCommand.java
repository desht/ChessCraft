package me.desht.chesscraft.commands;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class BoardStyleSetCommand extends AbstractCommand {

	public BoardStyleSetCommand() {
		super("chess b se", 2);
		setPermissionNode("chesscraft.commands.board.set");
		setUsage("/chess board set <attribute> <value> [<attribute> <value>...]");
	}
	
	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		notFromConsole(player);
		
		if (args.length % 2 != 0) {
			showUsage(player);
			return false;
		}
		
		BoardView bv = BoardView.partOfChessBoard(player.getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();
		
		for (int i = 0; i < args.length; i += 2) {
			String attr = args[i];
			String val = args[i + 1];
			
			try {
				if (partialMatch(attr, "white")) {
					style.setWhiteSquareMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "black")) {
					style.setBlackSquareMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "frame")) {
					style.setFrameMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "panel")) {
					style.setControlPanelMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "enclosure")) {
					style.setEnclosureMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "struts")) {
					style.setStrutsMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "highlight_default")) {
					style.setHighlightMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "highlight_white")) {
					style.setHighlightWhiteSquareMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "highlight_black")) {
					style.setHighlightBlackSquareMaterial(MaterialWithData.get(val));
				} else if (partialMatch(attr, "highlight_style")) {
					style.setHighlightStyle(HighlightStyle.getStyle(val));
				} else if (partialMatch(attr, "light_level")) {
					style.setLightLevel(Integer.parseInt(val));
				} else if (partialMatch(attr, "piece_style") || partialMatch(attr, "piecestyle")) {
					// update both the default piece style for the current board style...
					style.setPieceStyleName(val);
					// ... and the piece style that the current board is using.
					bv.getChessBoard().setPieceStyle(val);
				} else if (partialMatch(attr, "board_style") || partialMatch(attr, "boardstyle")) {
					bv.getChessBoard().setBoardStyle(val);
				} else if (partialMatch(attr, "default_stake")) {
					bv.setDefaultStake(Double.parseDouble(val));
				} else {
					throw new ChessException("Unknown attribute '" + attr + "'.");
				}
			} catch (NumberFormatException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", val));
			} catch (IllegalArgumentException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.boardStyleBadParam", val));
			}
		}
		
		MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.boardStyleChanged", bv.getName()));
		
		bv.paintAll();
		bv.save();
		
		return true;
	}

}
