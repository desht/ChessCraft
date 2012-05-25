package me.desht.chesscraft.expector;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.TerrainBackup;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ExpectBoardCreation extends ExpectBase {

	private final String boardName;
	private final String style;
	private final String pieceStyle;
	
	private Location loc;

	public ExpectBoardCreation(String boardName, String style, String pieceStyle) {
		this.boardName = boardName;
		this.style = style;
		this.pieceStyle = pieceStyle;
	}

	public void setLocation(Location loc) {
		this.loc = loc;
	}

	@Override
	public void doResponse(Player player) {
		BoardView view = new BoardView(boardName, loc, BoardRotation.getPlayerDirection(player), style, pieceStyle);
		BoardView.addBoardView(view);
		if (ChessCraft.getWorldEdit() != null) {
			TerrainBackup.save(player, view);
		}
		view.save();
		view.paintAll();
		ChessUtils.statusMessage(player, Messages.getString("ExpectBoardCreation.boardCreated", //$NON-NLS-1$
				boardName, ChessUtils.formatLoc(view.getA1Square())));
	}

}
