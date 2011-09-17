package me.desht.chesscraft.expector;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.ChessCraft;
import me.desht.util.ChessUtils;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.TerrainBackup;
import me.desht.chesscraft.enums.BoardOrientation;
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

	public void setLocation(Location loc) {
		this.loc = loc;
	}

	@Override
	public void doResponse(Player player) throws ChessException {
		BoardView view = new BoardView(boardName, plugin, loc, BoardOrientation.getPlayerDirection(player), style, pieceStyle);
		BoardView.addBoardView(view);
		if (ChessCraft.getWorldEdit() != null) {
			TerrainBackup.save(player, view);
		}
		view.paintAll();
		ChessUtils.statusMessage(player, Messages.getString("ExpectBoardCreation.boardCreated", //$NON-NLS-1$
				boardName, ChessUtils.formatLoc(view.getA1Square())));
		view.save();
	}

}
