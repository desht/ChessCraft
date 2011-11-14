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
package fr.free.jchecs.ai;

import static fr.free.jchecs.ai.AbstractEngine.MATE_VALUE;
import static fr.free.jchecs.core.Constants.FILE_COUNT;
import static fr.free.jchecs.core.Constants.RANK_COUNT;

import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Piece;
import fr.free.jchecs.core.PieceType;
import fr.free.jchecs.core.Square;

/**
 * Fonction d'évaluation basée sur le matériel et la position des pièces présentes sur le plateau.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
final class BoardControlHeuristic implements Heuristic
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -7163145298434616262L;

  /**
   * Bonus/Malus d'un cavalier (blanc par défaut) en fonction de sa position.
   */
  private static final int [] KNIGHT_POSITIONS = { //  
      -50, -30, -30, -30, -30, -30, -30, -50, // a1 ... h1
        -30, -20, -20, -20, -20, -20, -20, -30, // a2 ... h2
        -20, 0, 20, 20, 20, 20, 0, -20, // a3 ... h3
        -20, 0, 20, 20, 20, 20, 0, -20, // a4 ... h4
        -20, 0, 10, 20, 20, 10, 0, -20, // a5 ... h5
        -20, 0, 10, 10, 10, 10, 0, -20, // a6 ... h6
        -20, -10, 0, 0, 0, 0, -10, -20, // a7 ... h7
        -40, -20, -20, -20, -20, -20, -20, -40, // a8 ... h8
      };
  static
  {
    assert KNIGHT_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'un pion (blanc par défaut) en fonction de sa position.
   */
  private static final int [] PAWN_POSITIONS = { 0, 0, 0, 0, 0, 0, 0, 0, // a1 ... h1
    2, 2, 2, -2, -2, 2, 2, 2, // a2 ... h2
    -2, -2, -2, 4, 4, -2, -2, -2, // a3 ... h3
    0, 0, 0, 4, 4, 0, 0, 0, // a4 ... h4
    2, 4, 6, 8, 8, 6, 4, 2, // a5 ... h5
    4, 6, 8, 10, 10, 8, 6, 4, // a6 ... h6
    4, 6, 8, 10, 10, 8, 6, 4, // a7 ... h7
    500, 500, 500, 500, 500, 500, 500, 500, // a8 ... h8
  };
  static
  {
    assert PAWN_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus de base liés à la position d'une pièce (symétrique : adapté aux deux couleurs).
   */
  private static final int [] DEFAULT_POSITIONS = { 0, 0, 0, 0, 0, 0, 0, 0, // a1 ... h1
    0, 0, 0, 5, 5, 0, 0, 0, // a2 ... h2
    0, 0, 5, 5, 5, 5, 0, 0, // a3 ... h3
    0, 5, 5, 10, 10, 5, 5, 0, // a4 ... h4
    0, 5, 5, 10, 10, 5, 5, 0, // a5 ... h5
    0, 0, 5, 5, 5, 5, 0, 0, // a6 ... h6
    0, 0, 0, 5, 5, 0, 0, 0, // a7 ... h7
    0, 0, 0, 0, 0, 0, 0, 0, // a8 ... h8
  };
  static
  {
    assert DEFAULT_POSITIONS.length == 64;
  }

  /**
   * Crée une nouvelle instance.
   */
  BoardControlHeuristic()
  {
    // Rien de spécifique...
  }

  /**
   * Renvoi la valeur estimée d'un état du jeu, pour les fonctions de recherche du meilleur coup.
   * 
   * @param pEtat Etat du jeu.
   * @param pTrait Positionné à "true" si l'on veut une évaluation du point de vue des blancs.
   * @return Valeur estimée.
   * @see Heuristic#evaluate(MoveGenerator,boolean)
   */
  public int evaluate(final MoveGenerator pEtat, final boolean pTrait)
  {
    assert pEtat != null;

    int res = -pEtat.getHalfmoveCount();

    for (final Square s : Square.values())
    {
      final Piece piece = pEtat.getPieceAt(s);
      if (piece != null)
      {
        final boolean traitPiece = piece.isWhite();
        final PieceType typePiece = piece.getType();
        final int val = typePiece.getValue();
        final int pos;
        switch (typePiece)
        {
          case BISHOP :
          case QUEEN :
          case ROOK :
            pos = DEFAULT_POSITIONS[s.getIndex()];
            break;
          case KING :
            if ((traitPiece != pTrait) && (pEtat.getFullmoveNumber() > 10)
                && pEtat.isInCheck(traitPiece))
            {
              if (pEtat.getValidMoves(traitPiece).length == 0)
              {
                // Malus pour un mat...
                pos = MATE_VALUE;
              }
              else
              {
                // Malus pour un échec...
                pos = -250;
              }
            }
            else
            {
              // Pas de valeur de position pour le roi.
              pos = 0;
            }
            break;
          case KNIGHT :
            if (traitPiece)
            {
              pos = KNIGHT_POSITIONS[s.getIndex()];
            }
            else
            {
              pos = KNIGHT_POSITIONS[((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile()];
            }
            break;
          case PAWN :
            if (traitPiece)
            {
              pos = PAWN_POSITIONS[s.getIndex()];
            }
            else
            {
              pos = PAWN_POSITIONS[((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile()];
            }
            break;
          default :
            assert false;
            pos = 0;
        }
        final int score = val + pos;
        if (traitPiece == pTrait)
        {
          res += score;
        }
        else
        {
          res -= score;
        }
      }
    }

    return res;
  }
}
