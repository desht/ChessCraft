package me.desht.chesscraft;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ExpectBoardCreation extends ExpectData {
	String boardName;
	String style;
	String pieceStyle;
	Location loc;
	
	public ExpectBoardCreation(ChessCraft plugin, String boardName, String style, String pieceStyle) {
		super(plugin);
		this.boardName = boardName;
		this.style = style;
		this.pieceStyle = pieceStyle;
	}
	
	void setLocation(Location loc) {
		this.loc = loc;
	}
	
	@Override
	void doResponse(Player player) throws ChessException {
		if (!plugin.checkBoardView(boardName)) {
			BoardView view = new BoardView(boardName, plugin, loc, style, pieceStyle);
			plugin.addBoardView(boardName, view);
			view.paintAll();
			plugin.statusMessage(player, "Board &6" + boardName + "&- has been created at " + ChessCraft.formatLoc(view.getA1Square()) + ".");
		} else {
			plugin.errorMessage(player, "Board '" + boardName + "' already exists.");
		}
	}
}
