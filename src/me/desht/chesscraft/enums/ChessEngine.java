/**
 * Programmer: Jacob Scott
 * Program Name: ChessAI
 * Description: enum to define AI engine constants
 * Date: Jul 26, 2011
 */
package me.desht.chesscraft.enums;

public enum ChessEngine {

    ALPHABETA("jChecs.AlphaBeta", 3, 6, 5),
    MINIMAX("jChecs.MiniMax", 1, 4, 3),
    MINIMAX_PLUS("jChecs.MiniMax++", 2, 5, 4),
    NEGASCOUT("jChecs.NegaScout", 3, 6, 5),
    RANDOM("jChecs.Random", 1, 1, 1);
    // engine restrictions
    int minDepth, maxDepth, defaultDepth;
    String engineStr;

    ChessEngine(String str, int min, int max, int def) {
        engineStr = str;
        minDepth = min;
        maxDepth = max;
        defaultDepth = def;
    }

    public int getDefaultDepth() {
        return defaultDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMinDepth() {
        return minDepth;
    }
    
    @Override
    public String toString(){
        return engineStr;
    }
    
    public static ChessEngine getEngine(String match){
        for(ChessEngine e : values()){
            if(e.engineStr.equalsIgnoreCase(match)
                    || e.engineStr.toLowerCase().endsWith(match.toLowerCase())){
                return e;
            }
        }
        return null;
    }
}
