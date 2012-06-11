package me.desht.chesscraft.commands;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardStyle;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardStyleSetCommand extends AbstractCommand {

	public BoardStyleSetCommand() {
		super("chess b se", 2);
		setPermissionNode("chesscraft.commands.board.set");
		setUsage("/chess board set <attribute> <value> [<attribute> <value>...]");
	}
	
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws ChessException {
		notFromConsole(sender);
		
		if (args.length % 2 != 0) {
			showUsage(sender);
			return false;
		}
		
		BoardView bv = BoardView.partOfChessBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		if (bv.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode"));
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();
		
		for (int i = 0; i < args.length; i += 2) {
			String attr = args[i];
			String val = args[i + 1];
			
			try {
				if (attr.startsWith("white")) {
					style.setWhiteSquareMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("black")) {
					style.setBlackSquareMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("frame")) {
					style.setFrameMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("panel")) {
					style.setControlPanelMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("enclosure")) {
					style.setEnclosureMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("struts")) {
					style.setStrutsMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("highlight_default")) {
					style.setHighlightMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("highlight_white")) {
					style.setHighlightWhiteSquareMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("highlight_black")) {
					style.setHighlightBlackSquareMaterial(MaterialWithData.get(val));
				} else if (attr.startsWith("highlight_style")) {
					style.setHighlightStyle(HighlightStyle.getStyle(val));
				} else if (attr.startsWith("light_level")) {
					style.setLightLevel(Integer.parseInt(val));
				} else if (attr.startsWith("piece_style") || attr.startsWith("piecestyle")) {
					// update both the default piece style for the current board style...
					style.setPieceStyleName(val);
					// ... and the piece style that the current board is using.
					bv.getChessBoard().setPieceStyle(val);
				} else if (attr.startsWith("board_style") || attr.startsWith("boardstyle")) {
					bv.getChessBoard().setBoardStyle(val);
				} else if (attr.startsWith("default_stake")) {
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
		
		MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardStyleChanged", bv.getName()));
		
		bv.paintAll();
		bv.save();
		
		return true;
	}

}
