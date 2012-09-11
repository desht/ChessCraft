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
			return true;
		}
		
		BoardView bv = BoardView.partOfChessBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new ChessException(Messages.getString("Designer.notOnBoard"));
		}
		if (bv.isDesigning()) {
			throw new ChessException(Messages.getString("Game.boardInDesignMode"));
		}
		BoardStyle style = bv.getChessBoard().getBoardStyle();
		
		boolean styleHasChanged = false;
		
		for (int i = 0; i < args.length; i += 2) {
			String attr = args[i].replace("_", "");	// '_' is optional
			String val = args[i + 1];
			
			try {
				if (attr.startsWith("white")) {
					style.setWhiteSquareMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("black")) {
					style.setBlackSquareMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("frame")) {
					style.setFrameMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("panel")) {
					style.setControlPanelMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("enclosure")) {
					style.setEnclosureMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("struts")) {
					style.setStrutsMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("highlightwhite")) {
					style.setHighlightWhiteSquareMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("highlightblack")) {
					style.setHighlightBlackSquareMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("highlightstyle")) {
					style.setHighlightStyle(HighlightStyle.getStyle(val));
					styleHasChanged = true;
				} else if (attr.startsWith("highlight")) {
					style.setHighlightMaterial(MaterialWithData.get(val));
					style.setHighlightWhiteSquareMaterial(MaterialWithData.get(val));
					style.setHighlightBlackSquareMaterial(MaterialWithData.get(val));
					styleHasChanged = true;
				} else if (attr.startsWith("lightlevel")) {
					style.setLightLevel(Integer.parseInt(val));
					styleHasChanged = true;
				} else if (attr.startsWith("piecestyle")) {
					// update the default piece style for the current board style
					style.setPieceStyleName(val);
					styleHasChanged = true;
				} else if (attr.startsWith("overridepiecestyle")) {
					// update the piece style used by this board (but don't modify the style)
					bv.getChessBoard().setPieceStyle(val);
				} else if (attr.startsWith("boardstyle")) {
					bv.getChessBoard().setBoardStyle(val);
				} else if (attr.startsWith("defaultstake")) {
					bv.setDefaultStake(Double.parseDouble(val));
				} else if (attr.startsWith("defaulttc")) {
					bv.setDefaultTcSpec(val);
				} else if (attr.startsWith("locktc")) {
					bv.setLockTcSpec(Boolean.parseBoolean(val));
				} else if (attr.startsWith("lockstake")) {
					bv.setLockStake(Boolean.parseBoolean(val));
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
		if (styleHasChanged) {
			MiscUtil.statusMessage(sender, Messages.getString("ChessCommandExecutor.boardStyleSuggestSave"));
			bv.paintAll();
		} else {
			bv.getControlPanel().repaint();
		}
		
		bv.save();
		
		return true;
	}

}
