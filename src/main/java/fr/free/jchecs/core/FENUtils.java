/*
 $Id$

 Copyright (C) 2006-2007 by David Cotton

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later
 version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package fr.free.jchecs.core;

import static fr.free.jchecs.core.Constants.FILE_COUNT;
import static fr.free.jchecs.core.Constants.RANK_COUNT;

/**
 * Classes fournissant des fonctions utilitaires pour gérer la notation FEN.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
public final class FENUtils
{
  /** Chaine FEN de la position de départ. */
  public static final String STANDART_STARTING_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private FENUtils()
  {
    // Rien de spécifique...
  }

  /**
   * Evalue le champ de notation du trait d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN du trait.
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENActiveColor(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    if ((pChamp.length() != 1) || ("bw".indexOf(pChamp.charAt(0)) < 0))
    {
      throw new FENException("Invalid FEN active color [" + pChamp + ']', null);
    }

    pEtat.setWhiteActive(pChamp.charAt(0) == 'w');
  }

  /**
   * Evalue le champ de notation des possiblités de roque d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN du roque.
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENCastling(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    final int l = pChamp.length();
    if ((l < 1) || (l > 4))
    {
      throw new FENException("Invalid FEN castling field [" + pChamp + ']', null);
    }

    pEtat.setCastleLong(false, false);
    pEtat.setCastleLong(true, false);
    pEtat.setCastleShort(false, false);
    pEtat.setCastleShort(true, false);

    if ((l == 1) && (pChamp.charAt(0) == '-'))
    {
      return;
    }

    for (int i = l - 1; i >= 0; i--)
    {
      switch (pChamp.charAt(i))
      {
        case 'k' :
          pEtat.setCastleShort(false, true);
          break;
        case 'K' :
          pEtat.setCastleShort(true, true);
          break;
        case 'q' :
          pEtat.setCastleLong(false, true);
          break;
        case 'Q' :
          pEtat.setCastleLong(true, true);
          break;
        default :
          throw new FENException("Invalid FEN castling field [" + pChamp + ']', null);
      }
    }
  }

  /**
   * Evalue le champ "En passant" d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN "En passant".
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENEnPassant(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    final int l = pChamp.length();
    if ((l < 1) || (l > 2))
    {
      throw new FENException("Invalid FEN 'en passant' field [" + pChamp + ']', null);
    }

    if ((l != 1) || (pChamp.charAt(0) != '-'))
    {
      try
      {
        pEtat.setEnPassant(Square.valueOf(pChamp));
      }
      catch (final IllegalArgumentException e)
      {
        throw new FENException("Invalid FEN 'en passant' field [" + pChamp + ']', e);
      }
    }
  }

  /**
   * Evalue le champ "numéro de coup" d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN stockant le numéro du coup.
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENFullmove(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    try
    {
      final int num = Integer.parseInt(pChamp);
      if (num <= 0)
      {
        throw new FENException("Invalid FEN fullmove number field [" + pChamp + ']', null);
      }
      pEtat.setFullmoveNumber(num);
    }
    catch (final NumberFormatException e)
    {
      throw new FENException("Invalid FEN fullmove number field [" + pChamp + ']', e);
    }
  }

  /**
   * Evalue le champ "nombre de demi-coups" d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN stockant le nombre de demi-coups depuis la dernière prise ou
   *          mouvement de pion.
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENHalfmove(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    try
    {
      final int nb = Integer.parseInt(pChamp);
      if ((nb < 0) || (pChamp.length() == 0))
      {
        throw new FENException("Invalid FEN halfmove clock field [" + pChamp + ']', null);
      }
      pEtat.setHalfmoveCount(nb);
    }
    catch (final NumberFormatException e)
    {
      throw new FENException("Invalid FEN halfmove clock field [" + pChamp + ']', e);
    }
  }

  /**
   * Evalue le champ de positionnement des pièces d'une chaine FEN.
   * 
   * @param pChamp Contenu du champ FEN de positionnement des pièces.
   * @param pEtat Etat du jeu à paramètrer en fonction.
   * @throws FENException en cas d'erreur dans le champ.
   */
  private static void parseFENPlacement(final String pChamp, final MutableBoard pEtat)
    throws FENException
  {
    assert pChamp != null;
    assert pEtat != null;

    int rang = RANK_COUNT - 1;
    int col = 0;
    for (int i = 0; i < pChamp.length(); i++)
    {
      final char c = pChamp.charAt(i);
      if (c == '/')
      {
        if ((col != FILE_COUNT) || (rang <= 0))
        {
          throw new FENException("Invalid piece placement field [" + pChamp + ']', null);
        }
        rang--;
        col = 0;
      }
      else if ("12345678".indexOf(c) >= 0)
      {
        final int rep = c - '0';
        for (int j = 0; j < rep; j++)
        {
          try
          {
            pEtat.setPieceAt(null, Square.valueOf(col++, rang));
          }
          catch (final IllegalArgumentException e)
          {
            throw new FENException("Invalid piece placement field [" + pChamp + ']', e);
          }
        }
      }
      else
      {
        final Piece p = Piece.valueOf(c);
        if (p == null)
        {
          throw new FENException("Invalid piece placement field [" + pChamp + ']', null);
        }
        try
        {
          pEtat.setPieceAt(p, Square.valueOf(col, rang));
        }
        catch (final IllegalArgumentException e)
        {
          throw new FENException("Invalid piece placement field [" + pChamp + ']', e);
        }
        col++;
      }
      if (col > FILE_COUNT)
      {
        throw new FENException("Invalid piece placement field [" + pChamp + ']', null);
      }
    }
    if ((col != FILE_COUNT) || (rang != 0))
    {
      throw new FENException("Invalid piece placement field [" + pChamp + ']', null);
    }
  }

  /**
   * Renvoi la description d'état de jeu correspondant à une chaine FEN particulière.
   * 
   * @param pFEN Chaine FEN décrivant un état.
   * @return Instance correspondante de description d'état du jeu.
   * @throws FENException en cas d'erreur dans le format de la chaine FEN.
   */

  public static Board toBoard(final String pFEN) throws FENException
  {
    if (pFEN == null)
    {
      throw new NullPointerException("Missing FEN string");
    }

    final String [] fields = pFEN.split(" ");
    if (fields.length != 6)
    {
      throw new FENException("Invalid FEN string [" + pFEN + ']', null);
    }

    final MutableBoard res = new MutableBoard();

    parseFENPlacement(fields[0], res);
    parseFENActiveColor(fields[1], res);
    parseFENCastling(fields[2], res);
    parseFENEnPassant(fields[3], res);
    parseFENHalfmove(fields[4], res);
    parseFENFullmove(fields[5], res);

    return res;
  }

  /**
   * Renvoi la chaine FEN correspondant à un état du jeu.
   * 
   * @param pEtat Etat du jeu.
   * @return Chaine FEN décrivant l'état.
   */
  public static String toFEN(final Board pEtat)
  {
    if (pEtat == null)
    {
      throw new NullPointerException("Missing game state");
    }

    final StringBuilder res = new StringBuilder(toFENKey(pEtat));
    res.append(' ');

    // Champ du compteur de demi-coups...
    res.append(Integer.toString(pEtat.getHalfmoveCount()));
    res.append(' ');

    // Champ du numéro de coup...
    res.append(Integer.toString(pEtat.getFullmoveNumber()));

    return res.toString();
  }

  /**
   * Renvoi le début de la chaine FEN (4 premiers champs), utilisable comme clé identifiant un etat
   * (sans tenir compte du numéro du coup et de la règle des 50 demi-coups).
   * 
   * @param pEtat Etat du jeu.
   * @return Chaine FEN décrivant l'état.
   */
  public static String toFENKey(final Board pEtat)
  {
    if (pEtat == null)
    {
      throw new NullPointerException("Missing game state");
    }

    final StringBuilder res = new StringBuilder();

    // Champ des positions...
    for (int y = RANK_COUNT - 1; y >= 0; y--)
    {
      int vide = 0;
      for (int x = 0; x < FILE_COUNT; x++)
      {
        final Piece p = pEtat.getPieceAt(Square.valueOf(x, y));
        if (p == null)
        {
          vide++;
        }
        else
        {
          if (vide > 0)
          {
            res.append((char) ('0' + vide));
            vide = 0;
          }
          res.append(p.getFENLetter());
        }
      }
      if (vide > 0)
      {
        res.append((char) ('0' + vide));
      }
      if (y != 0)
      {
        res.append('/');
      }
    }
    res.append(' ');

    // Champ du trait
    if (pEtat.isWhiteActive())
    {
      res.append('w');
    }
    else
    {
      res.append('b');
    }
    res.append(' ');

    // Champ des roques...
    boolean roque = false;
    if (pEtat.canCastleShort(true))
    {
      res.append('K');
      roque = true;
    }
    if (pEtat.canCastleLong(true))
    {
      res.append('Q');
      roque = true;
    }
    if (pEtat.canCastleShort(false))
    {
      res.append('k');
      roque = true;
    }
    if (pEtat.canCastleLong(false))
    {
      res.append('q');
      roque = true;
    }
    if (!roque)
    {
      res.append('-');
    }
    res.append(' ');

    // Champ de la prise ne passant...
    final Square enPassant = pEtat.getEnPassant();
    if (enPassant != null)
    {
      res.append(enPassant.getFENString());
    }
    else
    {
      res.append('-');
    }

    return res.toString();
  }
}
