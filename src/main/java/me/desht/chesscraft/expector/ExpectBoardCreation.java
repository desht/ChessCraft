package me.desht.chesscraft.expector;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ExpectBoardCreation extends ExpectChessBase {

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
	public void doResponse(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		if (player == null) {
			LogUtils.warning("Board creation: player " + playerName + " gone offline?");
			return;
		}
		
		BoardView view = BoardView.createBoard(boardName, loc, BoardRotation.getRotation(player), style, pieceStyle);
		
		MiscUtil.statusMessage(player, Messages.getString("ExpectBoardCreation.boardCreated", //$NON-NLS-1$
				view.getName(), ChessUtils.formatLoc(view.getA1Square())));
	}

}
