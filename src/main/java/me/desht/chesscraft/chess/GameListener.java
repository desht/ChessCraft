package me.desht.chesscraft.chess;

import me.desht.chesscraft.chess.player.ChessPlayer;
import me.desht.chesscraft.enums.GameState;

public interface GameListener {
    void gameStateChanged(GameState state);
    boolean tryTimeControlChange(String tcSpec);
    void timeControlChanged(String spec);
    boolean tryStakeChange(double newStake);
    void stakeChanged(double newStake);
    void playerAdded(ChessPlayer cp);
    void gameDeleted();
    void promotionPieceChanged(ChessPlayer chessPlayer, int promotionPiece);
}
