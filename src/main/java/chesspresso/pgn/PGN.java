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
 * $Id: PGN.java,v 1.1 2002/12/08 13:27:34 BerniMan Exp $
 */

package chesspresso.pgn;


import java.util.Date;
import java.util.Calendar;


/**
 * General definitions for the PGN standard.
 *
 * The pgn standard is available at <a href="ftp://chess.onenet.net">ftp://chess.onenet.net</a>.
 *
 * @author  Bernhard Seybold
 * @version $Revision: 1.1 $
 */
public abstract class PGN
{

    //======================================================================
    // pgn date
    // examples: 
    // [Date "1992.08.31"]
    // [Date "1993.??.??"]
    // [Date "2001.01.01"]
    
    public static int getYearOfPGNDate(String pgnDate) throws IllegalArgumentException
    {
        if (pgnDate == null) throw new IllegalArgumentException("date string is null");
        try {
            int index = pgnDate.indexOf('.');
            if (index == -1) throw new IllegalArgumentException("string does not contain a dot");
            return Integer.parseInt(pgnDate.substring(0, index));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }
    
    private static String getRights(String s, int num)
    {
        return s.substring(s.length() - num);
    }
    
    public static String dateToPGNDate(Date date)
    {
        Calendar cal =Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "."
             + getRights("00" + cal.get(Calendar.MONTH), 2) + "."
             + getRights("00" + cal.get(Calendar.DAY_OF_MONTH), 2);
        
//deprecated
//        return date.getYear() + "."
//             + getRights("00" + date.getMonth(), 2) + "."  Calendar.get
//             + getRights("00" + date.getDay(), 2);
    }
    
    //======================================================================
    // constants for parsing
    
    public final static char
        TOK_QUOTE          = '"',
        TOK_PERIOD         = '.',
        TOK_ASTERISK       = '*',
        TOK_TAG_BEGIN      = '[',
        TOK_TAG_END        = ']',
        TOK_LINE_BEGIN     = '(',
        TOK_LINE_END       = ')',
        TOK_LBRACKET       = '<',
        TOK_RBRACKET       = '>',
        TOK_NAG_BEGIN      = '$',
        TOK_LINE_COMMENT   = ';',
        TOK_COMMENT_BEGIN  = '{',
        TOK_COMMENT_END    = '}',
        TOK_PGN_ESCAPE     = '%';
    
    //======================================================================
    // TAG constants
    
    // Seven tag roaster
    public final static String
        TAG_EVENT      = "Event",
        TAG_SITE       = "Site",
        TAG_DATE       = "Date",
        TAG_ROUND      = "Round",
        TAG_WHITE      = "White",
        TAG_BLACK      = "Black",
        TAG_RESULT     = "Result",
    
    // Standard extensions
        TAG_EVENT_DATE = "EventDate",
        TAG_WHITE_ELO  = "WhiteElo",
        TAG_BLACK_ELO  = "BlackElo",
        TAG_ECO        = "ECO",
        TAG_FEN        = "FEN";
    
}