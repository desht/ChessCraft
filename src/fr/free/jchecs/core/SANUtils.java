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

import static fr.free.jchecs.core.Constants.RANK_COUNT;
import static fr.free.jchecs.core.Piece.BLACK_KING;
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_KING;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;
import static fr.free.jchecs.core.PieceType.KING;
import static fr.free.jchecs.core.PieceType.PAWN;
import static fr.free.jchecs.core.PieceType.QUEEN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Classes fournissant des fonctions utilitaires pour gérer la notation PGN.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
public final class SANUtils
{
  /** Expression régulière permettant de valider une chaîne SAN. */
  // Découpage du pattern :
  // Mat/Pat/Nullité : (\\+{1,2}|#|\\(=\\))?
  // Petit roque : 0-0<Mat/Pat/Nullité>
  // Grand roque : 0-0-0<Mat/Pat/Nullité>
  // Pion sans prise : [a-h]([1-8]|[18][BKNQR])<Mat/Pat/Nullité>
  // Pion avec prise :
  // [a-h]x[a-h]((([1-8]|[18][BKNQR])<Mat/Pat/Nullité>)|([36]<Mat/Pat/Nullité> e\\.p\\.))
  // Pièces (sauf pion) : [BKNQR][a-h]?[1-8]?x?[a-h][1-8]<Mat/Pat/Nullité>
  public static final Pattern SAN_VALIDATOR =
      Pattern.compile("^(0-0(\\+{1,2}|#|\\(=\\))?)|(0-0-0(\\+{1,2}|#|\\(=\\))?)|"
          + "([a-h]([1-8]|[18][BKNQR])(\\+{1,2}|#|\\(=\\))?)|"
          + "([a-h]x[a-h]((([1-8]|[18][BKNQR])(\\+{1,2}|#|\\(=\\))?)|"
          + "([36](\\+{1,2}|#|\\(=\\))? e\\.p\\.)))|"
          + "([BKNQR][a-h]?[1-8]?x?[a-h][1-8](\\+{1,2}|#|\\(=\\))?)$");

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private SANUtils()
  {
    // Rien de spécifique...
  }

  /**
   * Renvoi le mouvement correspondant à une chaine SAN appliquée à un état d'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pSAN Chaine SAN.
   * @return Mouvement correspondant.
   * @throws SANException en cas d'erreur dans le format de la chaine SAN.
   */
  public static Move toMove(final MoveGenerator pEtat, final String pSAN) throws SANException
  {
    if (pEtat == null)
    {
      throw new NullPointerException("Missing game state");
    }
    if (pSAN == null)
    {
      throw new NullPointerException("Missing SAN string");
    }

    if (!SAN_VALIDATOR.matcher(pSAN).matches())
    {
      throw new SANException("Invalid SAN string [" + pSAN + ']', null);
    }

    final boolean trait = pEtat.isWhiteActive();
    if ("0-0".equals(pSAN))
    {
      // Gère les petits roques...
      if (trait)
      {
        return new Move(WHITE_KING, Square.valueOf(4), Square.valueOf(6));
      }
      return new Move(BLACK_KING, Square.valueOf(60), Square.valueOf(62));
    }
    else if ("0-0-0".equals(pSAN))
    {
      // ... les grands roques...
      if (trait)
      {
        return new Move(WHITE_KING, Square.valueOf(4), Square.valueOf(2));
      }
      return new Move(BLACK_KING, Square.valueOf(60), Square.valueOf(58));
    }

    // Gère les coups normaux...
    final Piece piece;
    int posSrc = 0;
    char c = pSAN.charAt(posSrc);
    if (Character.isLowerCase(c))
    {
      if (trait)
      {
        piece = WHITE_PAWN;
      }
      else
      {
        piece = BLACK_PAWN;
      }
    }
    else
    {
      if (trait)
      {
        piece = Piece.valueOf(c);
      }
      else
      {
        piece = Piece.valueOf(Character.toLowerCase(c));
      }
      posSrc++;
    }

    final boolean prise = pSAN.indexOf('x') >= 0;
    final List<Move> mvts = new ArrayList<Move>(Arrays.asList(pEtat.getValidMoves(trait)));
    for (int i = mvts.size() - 1; i >= 0; i--)
    {
      final Move m = mvts.get(i);
      final boolean capture = m.getCaptured() != null;
      if ((piece != m.getPiece()) || (prise != capture))
      {
        mvts.remove(i);
      }
    }

    int posDst = pSAN.length() - 1;
    while ((posDst > 0) && (!Character.isDigit(pSAN.charAt(posDst))))
    {
      posDst--;
    }
    final Square dst = Square.valueOf(pSAN.substring(posDst - 1, posDst + 1));
    for (int i = mvts.size() - 1; i >= 0; i--)
    {
      final Move m = mvts.get(i);
      if (dst != m.getTo())
      {
        mvts.remove(i);
      }
    }
    if (mvts.size() == 1)
    {
      return mvts.get(0);
    }

    // Supprime les ambiguités...
    c = pSAN.charAt(posSrc);
    if (Character.isLowerCase(c))
    {
      final int col = c - 'a';
      for (int i = mvts.size() - 1; i >= 0; i--)
      {
        final Move m = mvts.get(i);
        if (col != m.getFrom().getFile())
        {
          mvts.remove(i);
        }
      }
      posSrc++;
    }
    c = pSAN.charAt(posSrc);
    if (Character.isDigit(c))
    {
      final int lig = c - '1';
      for (int i = mvts.size() - 1; i >= 0; i--)
      {
        final Move m = mvts.get(i);
        if (lig != m.getFrom().getRank())
        {
          mvts.remove(i);
        }
      }
      posSrc++;
    }

    final int l = mvts.size();
    if (l > 1)
    {
      throw new SANException("Ambiguous SAN string [" + pSAN + ']', null);
    }
    else if (l < 1)
    {
      throw new SANException("Illegal SAN string context [" + pSAN + ']', null);
    }

    return mvts.get(0);
  }

  /**
   * Renvoi la chaine SAN correspondant à un mouvement pour un état d'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pMouvement Mouvement à traduire.
   * @return Chaine SAN correspondante.
   */
  public static String toSAN(final MoveGenerator pEtat, final Move pMouvement)
  {
    if (pEtat == null)
    {
      throw new NullPointerException("Missing game state");
    }
    if (pMouvement == null)
    {
      throw new NullPointerException("Missing move");
    }

    final boolean trait = pEtat.isWhiteActive();
    final Piece piece = pMouvement.getPiece();
    final PieceType t = piece.getType();
    final StringBuilder sb = new StringBuilder();
    final Square src = pMouvement.getFrom();
    final Square dst = pMouvement.getTo();
    final MoveGenerator apres = pEtat.derive(pMouvement, false);
    final int nbMvts = apres.getValidMoves(!trait).length;

    final int xSrc = src.getFile();
    final int xDst = dst.getFile();
    if ((t == KING) && (Math.abs(xSrc - xDst) > 1))
    {
      // Roques...
      sb.append("0-0");
      if (xSrc > xDst)
      {
        sb.append("-0");
      }
    }
    else
    {
      // Normal...
      sb.append(t.getSANLetter());

      // Recherche et levée des éventuelles ambiguités...
      if (t != PAWN)
      {
        final List<Move> mvts = new ArrayList<Move>(Arrays.asList(pEtat.getValidMoves(trait)));
        for (int i = mvts.size() - 1; i >= 0; i--)
        {
          final Move m = mvts.get(i);
          if ((piece != m.getPiece()) || (dst != m.getTo()) || (m.equals(pMouvement)))
          {
            mvts.remove(i);
          }
        }
        boolean preciser = true;
        for (int i = mvts.size() - 1; i >= 0; i--)
        {
          final Move m = mvts.get(i);
          if (xSrc != m.getFrom().getFile())
          {
            mvts.remove(i);
            if (preciser)
            {
              sb.append((char) ('a' + xSrc));
              preciser = false;
            }
          }
        }
        final int ySrc = src.getRank();
        for (int i = mvts.size() - 1; i >= 0; i--)
        {
          final Move m = mvts.get(i);
          if (ySrc != m.getFrom().getRank())
          {
            sb.append((char) ('1' + ySrc));
            break;
          }
        }
      }

      if ((pEtat.getPieceAt(dst) != null) || ((dst == pEtat.getEnPassant()) && (t == PAWN)))
      {
        // Prise...
        if (t == PAWN)
        {
          sb.append((char) ('a' + xSrc));
        }
        sb.append('x');
      }

      sb.append(dst.getFENString());

      if (t == PAWN)
      {
        // Cas particuliers...
        if (dst == pEtat.getEnPassant())
        {
          // ... de la prise en passant...
          sb.append(" e.p.");
        }
        else
        {
          // ... ou de la promotion...
          final int yDst = dst.getRank();
          if ((trait && (yDst == RANK_COUNT - 1)) || ((!trait) && (yDst == 0)))
          {
            // Le '=' n'est pas dans la version de SAN de la FIDE : sb.append('=');
            sb.append(QUEEN.getSANLetter());
          }
        }
      }
    }

    if (apres.isInCheck(!trait))
    {
      // Echec / Mat ...
      sb.append('+');
      if (nbMvts == 0)
      {
        sb.append('+');
      }
    }
    else if (nbMvts == 0)
    {
      // Pat ...
      sb.append("(=)");
    }

    final String res = sb.toString();
    assert SAN_VALIDATOR.matcher(res).matches();
    return res;
  }
}
