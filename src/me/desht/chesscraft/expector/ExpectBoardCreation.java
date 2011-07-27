package me.desht.chesscraft.expector;

import me.desht.chesscraft.BoardView;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessUtils;
import me.desht.chesscraft.TerrainBackup;
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
//        if (!BoardView.checkBoardView(boardName)) {
            BoardView view = new BoardView(boardName, plugin, loc, style, pieceStyle);
            if (plugin.getWorldEdit() != null) {
                TerrainBackup.save(plugin, player, view);
            }
            view.paintAll();
            ChessUtils.statusMessage(player, "Board &6" + boardName + "&- has been created at "
                    + ChessUtils.formatLoc(view.getA1Square()) + ".");
            plugin.getSaveDatabase().autosaveBoards();
//        } else {
//            ChessUtils.errorMessage(player, "Board '" + boardName + "' already exists.");
//        }
    }
}
