/*
 * Copyright (C) Bernhard Seybold. All rights reserved.
 *
 * This software is published under the terms of the LGPL Software License,
 * a copy of which has been included with this distribution in the LICENSE.txt
 * file.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *
 * $Id: PGNWriter.java,v 1.2 2003/01/04 16:15:02 BerniMan Exp $
 */

package chesspresso.pgn;


import chesspresso.*;
import chesspresso.game.*;
import chesspresso.move.Move;
import chesspresso.position.*;

import java.io.*;
//import java.util.*;


/**
 * A PGN writer is able to write a game (collection) in PGN syntax.
 *
 * @author  Bernhard Seybold
 * @version $Revision: 1.2 $
 */
public class PGNWriter extends PGN
{

    private PrintWriter m_out;
    private int m_charactersPerLine;
    private int m_curCol;
    
    /*================================================================================*/
    
    public PGNWriter(Writer out)
    {
        this(new PrintWriter(out));
    }

    public PGNWriter(PrintWriter out)
    {
        m_out = out;
        setCharactersPerLine(80);
    }

    /*================================================================================*/
    
    public void setCharactersPerLine(int chars) {m_charactersPerLine = chars;}

    public void write(GameModelIterator iterator)
    {
        while (iterator.hasNext()) {
            write(iterator.nextGameModel());
            m_out.println();
        }
    }
    
    public void write(GameModel gameModel)
    {
        Game game = new Game(gameModel);
        writeHeader(game);
        m_out.println();
        m_curCol = 0;
        writeMoves(game);
        if (m_curCol > 0) m_out.println();
    }
    
    /*================================================================================*/
    
    private void writeHeader(Game game)
    {
        m_out.println(TOK_TAG_BEGIN + TAG_EVENT  + " " + TOK_QUOTE + game.getEvent()     + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_SITE   + " " + TOK_QUOTE + game.getSite()      + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_DATE   + " " + TOK_QUOTE + game.getDate()      + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_ROUND  + " " + TOK_QUOTE + game.getRound()     + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_WHITE  + " " + TOK_QUOTE + game.getWhite()     + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_BLACK  + " " + TOK_QUOTE + game.getBlack()     + TOK_QUOTE + TOK_TAG_END);
        m_out.println(TOK_TAG_BEGIN + TAG_RESULT + " " + TOK_QUOTE + game.getResultStr() + TOK_QUOTE + TOK_TAG_END);
        
        if (game.getWhiteEloStr() != null) 
            m_out.println(TOK_TAG_BEGIN + TAG_WHITE_ELO  + " " + TOK_QUOTE + game.getWhiteElo()  + TOK_QUOTE + TOK_TAG_END);
        if (game.getBlackEloStr() != null) 
            m_out.println(TOK_TAG_BEGIN + TAG_BLACK_ELO  + " " + TOK_QUOTE + game.getBlackElo()  + TOK_QUOTE + TOK_TAG_END);
        if (game.getEventDate() != null) 
            m_out.println(TOK_TAG_BEGIN + TAG_EVENT_DATE + " " + TOK_QUOTE + game.getEventDate() + TOK_QUOTE + TOK_TAG_END);
        if (game.getECO() != null) 
            m_out.println(TOK_TAG_BEGIN + TAG_ECO        + " " + TOK_QUOTE + game.getECO()       + TOK_QUOTE + TOK_TAG_END);
        
        if (!game.getPosition().isStartPosition())
            m_out.println(TOK_TAG_BEGIN + TAG_FEN        + " " + TOK_QUOTE + FEN.getFEN(game.getPosition()) + TOK_QUOTE + TOK_TAG_END);
    }

    private void writeMoves(Game game)
    {
        // print leading comments before move 1
        String comment = game.getComment();
        if (comment != null) {
            print(TOK_COMMENT_BEGIN + comment + TOK_COMMENT_END, true);
        }
        
        game.traverse(new GameListener() {
            private boolean needsMoveNumber = true;
            public void notifyMove(Move move, short[] nags, String comment, int plyNumber, int level)
            {
                if (needsMoveNumber) {
                    if (move.isWhiteMove()) {
                        print(Chess.plyToMoveNumber(plyNumber) + ".", true);
                    } else {
                        print(Chess.plyToMoveNumber(plyNumber) + "...", true);
                    }
                }
                print(move.toString(), true);
                
                if (nags != null) {
                    for (int i=0; i < nags.length; i++) {
                        print(String.valueOf(TOK_NAG_BEGIN) + String.valueOf(nags[i]), true);
                    }
                }
                if (comment != null) print(TOK_COMMENT_BEGIN + comment + TOK_COMMENT_END, true);
                needsMoveNumber = !move.isWhiteMove() || (comment != null);
            }
            public void notifyLineStart(int level)
            {
                print(String.valueOf(TOK_LINE_BEGIN), false);
                needsMoveNumber = true;
            }
            public void notifyLineEnd(int level)
            {
                print(String.valueOf(TOK_LINE_END), true);
                needsMoveNumber = true;
            }
        }, true);
        
        print(game.getResultStr(), false);
    }

    private void print(String s, boolean addSpace)
    {
        if (m_curCol + s.length() > m_charactersPerLine) {
            m_out.println();
            m_curCol = 0;
        }
        m_out.print(s);
        m_curCol += s.length();
        if (m_curCol > 0 && addSpace) {
            m_out.print(" ");
            m_curCol += 1;
        }
    }
}