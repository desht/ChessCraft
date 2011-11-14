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
import static fr.free.jchecs.core.PieceType.KING;
import static fr.free.jchecs.core.PieceType.PAWN;

import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Piece;
import fr.free.jchecs.core.PieceType;
import fr.free.jchecs.core.Square;

/**
 * Fonction d'évaluation basée sur le matériel, la position des pièces présentes sur le plateau et
 * leur mobilité.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
final class MobilityHeuristic implements Heuristic
{
  /** Nombre de pièces à partir duquel on considère être en fin de partie. */
  private static final int END_GAME = 8;

  /** Nombre de pièces à partir duquel on considère être en milieu de partie. */
  private static final int MIDDLE_GAME = 16;

  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = 8752973612245818678L;

  /**
   * Bonus/Malus d'un fou (blanc par défaut) en fonction de sa position.
   */
  private static final int [] BISHOP_POSITIONS = { -5, -5, -5, -5, -5, -5, -5, -5, // a1 ... h1
    -5, 10, 5, 10, 10, 5, 10, -5, // a2 ... h2
    -5, 5, 3, 12, 12, 3, 5, -5, // a3 ... h3
    -5, 3, 12, 3, 3, 12, 3, -5, // a4 ... h4
    -5, 3, 12, 3, 3, 12, 3, -5, // a5 ... h5
    -5, 5, 3, 12, 12, 3, 5, -5, // a6 ... h6
    -5, 10, 5, 10, 10, 5, 10, -5, // a7 ... h7
    -5, -5, -5, -5, -5, -5, -5, -5, // a8 ... h8
  };
  static
  {
    assert BISHOP_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'un roi (blanc par défaut) en fonction de sa position.
   */
  private static final int [] KING_POSITIONS = { 2, 3, 5, -5, 0, -4, 6, 4, // a1 ... h1
    -3, -3, -5, -5, -5, -5, -3, -3, // a2 ... h2
    -5, -5, -8, -8, -8, -8, -5, -5, // a3 ... h3
    -8, -8, -13, -13, -13, -13, -8, -8, // a4 ... h4
    -13, -13, -21, -21, -21, -21, -13, -13, // a5 ... h5
    -21, -21, -34, -34, -34, -34, -21, -21, // a6 ... h6
    -34, -34, -55, -55, -55, -55, -34, -34, // a7 ... h7
    -55, -55, -89, -89, -89, -89, -55, -55, // a8 ... h8
  };
  static
  {
    assert KING_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'un roi (blanc par défaut) en fonction de sa position, en fin de partie.
   */
  private static final int [] KING_END_POSITIONS = { -5, -3, -1, 0, 0, -1, -3, -5, // a1 ... h1
    -3, 5, 5, 5, 5, 5, 5, -3, // a2 ... h2
    -1, 5, 10, 10, 10, 10, 5, -1, // a3 ... h3
    0, 5, 10, 15, 15, 10, 5, 0, // a4 ... h4
    0, 5, 10, 15, 15, 10, 5, 0, // a5 ... h5
    -1, 5, 10, 10, 10, 10, 5, -1, // a6 ... h6
    -3, 5, 5, 5, 5, 5, 5, -3, // a7 ... h7
    -5, -3, -1, 0, 0, -1, -3, -5, // a8 ... h8
  };
  static
  {
    assert KING_END_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'un cavalier (blanc par défaut) en fonction de sa position.
   */
  private static final int [] KNIGHT_POSITIONS = { -10, -5, -3, -1, -1, -3, -5, -10, // a1 ... h1
    -5, 0, 0, 3, 3, 0, 0, -5, // a2 ... h2
    -3, 0, 5, 5, 5, 5, 0, -3, // a3 ... h3
    -1, 1, 5, 10, 10, 5, 1, -1, // a4 ... h4
    -1, 1, 7, 12, 12, 7, 1, -1, // a5 ... h5
    -3, 0, 5, 7, 7, 5, 0, -3, // a6 ... h6
    -5, 0, 0, 3, 3, 0, 0, -5, // a7 ... h7
    -10, -5, -3, -1, -1, -3, -5, -10, // a8 ... h8
  };
  static
  {
    assert KNIGHT_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'un pion (blanc par défaut) en fonction de sa position.
   */
  private static final int [] PAWN_POSITIONS = { 0, 0, 0, 0, 0, 0, 0, 0, // a1 ... h1
    0, 0, 0, -5, -5, 0, 0, 0, // a2 ... h2
    1, 2, 4, 4, 4, 3, 2, 1, // a3 ... h3
    2, 4, 7, 8, 8, 6, 4, 2, // a4 ... h4
    3, 6, 11, 12, 12, 9, 6, 3, // a5 ... h5
    4, 8, 12, 16, 16, 12, 8, 4, // a6 ... h6
    5, 10, 15, 20, 20, 15, 10, 5, // a7 ... h7
    100, 100, 100, 100, 100, 100, 100, 100, // a8 ... h9
  };
  static
  {
    assert PAWN_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'une reine (blanche par défaut) en fonction de sa position.
   */
  private static final int [] QUEEN_POSITIONS = { -5, -5, -5, 0, 0, -5, -5, -5, // a1 ... h1
    0, 0, 3, 3, 3, 0, 0, 0, // a2 ... h2
    0, 3, 3, 3, 3, 0, 0, 0, // a3 ... h3
    0, 0, 0, 5, 5, 0, 0, 0, // a4 ... h4
    0, 0, 0, 5, 5, 0, 0, 0, // a5 ... h5
    -5, -5, 0, 0, 0, 0, 0, 0, // a6 ... h6
    -5, -5, 0, 0, 0, 0, 0, 0, // a7 ... h7
    -5, -5, 0, 0, 0, 0, 0, 0, // a8 ... h8
  };
  static
  {
    assert QUEEN_POSITIONS.length == 64;
  }

  /**
   * Bonus/Malus d'une tour (blanche par défaut) en fonction de sa position.
   */
  private static final int [] ROOK_POSITIONS = { 0, 0, 0, 5, 5, 0, 0, 0, // a1 ... h1
    -2, 0, 0, 0, 0, 0, 0, -2, // a2 ... h2
    -2, 0, 0, 0, 0, 0, 0, -2, // a3 ... h3
    -2, 0, 0, 0, 0, 0, 0, -2, // a4 ... h4
    -2, 0, 0, 0, 0, 0, 0, -2, // a5 ... h5
    -2, 0, 0, 0, 0, 0, 0, -2, // a6 ... h6
    10, 10, 10, 10, 10, 10, 10, 10, // a7 ... h7
    0, 0, 0, 0, 0, 0, 0, 0, // a8 ... h8
  };
  static
  {
    assert ROOK_POSITIONS.length == 64;
  }

  /**
   * Crée une nouvelle instance.
   */
  MobilityHeuristic()
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

    final int [] pionsTrait = new int [ FILE_COUNT ];
    final int [] pionsAdversaire = new int [ FILE_COUNT ];

    int res = -pEtat.getHalfmoveCount() * 2;

    int nbPieces = 0;
    for (final Square s : Square.values())
    {
      final Piece piece = pEtat.getPieceAt(s);
      if (piece != null)
      {
        nbPieces++;
        if (piece.getType() == PAWN)
        {
          final int idx = s.getFile();
          if (piece.isWhite() == pTrait)
          {
            pionsTrait[idx]++;
            if (pionsTrait[idx] > 1)
            {
              res -= 5;
            }
          }
          else
          {
            pionsAdversaire[idx]++;
            if (pionsAdversaire[idx] > 1)
            {
              res += 5;
            }
          }
        }
      }
    }

    for (final Square s : Square.values())
    {
      final Piece piece = pEtat.getPieceAt(s);
      if (piece != null)
      {
        final boolean traitPiece = piece.isWhite();
        final PieceType typePiece = piece.getType();
        final int mat = typePiece.getValue();
        final int pos;
        final int mob;
        switch (typePiece)
        {
          case BISHOP :
            if (traitPiece)
            {
              pos = BISHOP_POSITIONS[s.getIndex()];
            }
            else
            {
              pos = BISHOP_POSITIONS[((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile()];
            }
            if (nbPieces >= MIDDLE_GAME)
            {
              mob = pEtat.getBishopTargets(s, traitPiece).length * 4;
            }
            else
            {
              mob = 0;
            }
            break;
          case KING :
            if ((pEtat.getFullmoveNumber() > 5) && pEtat.isInCheck(traitPiece))
            {
              pos = 0;
              if (pEtat.getValidMoves(traitPiece).length == 0)
              {
                // Mat : inutile d'aller plus loin...
                if (traitPiece == pTrait)
                {
                  return MATE_VALUE;
                }

                return -MATE_VALUE;
              }

              // Malus pour un échec...
              mob = -250;
            }
            else
            {
              final int idx;
              if (traitPiece)
              {
                idx = s.getIndex();
              }
              else
              {
                idx = ((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile();
              }
              if (nbPieces >= END_GAME)
              {
                pos = KING_POSITIONS[idx];
              }
              else
              {
                pos = KING_END_POSITIONS[idx];
              }
              if ((pEtat.getFullmoveNumber() <= 32) && pEtat.isCastled(traitPiece))
              {
                // Pour favoriser le roque en début de partie...
                mob = 25;
              }
              else
              {
                mob = 0;
              }
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
            if (nbPieces >= MIDDLE_GAME)
            {
              mob = pEtat.getKnightTargets(s, traitPiece).length * 4;
            }
            else
            {
              mob = 0;
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
            if (nbPieces > END_GAME)
            {
              mob = 0;
            }
            else
            {
              mob = pos;
            }
            break;
          case QUEEN :
            int posReine = 0;
            if (traitPiece)
            {
              posReine = QUEEN_POSITIONS[s.getIndex()];
            }
            else
            {
              posReine =
                  QUEEN_POSITIONS[((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile()];
            }
            if (pEtat.getFullmoveNumber() < 12)
            {
              // Essayer d'éviter de déplacer la reine trop tôt...
              if ((traitPiece && (s.getIndex() != 3)) || ((!traitPiece) && (s.getIndex() != 59)))
              {
                posReine -= 30;
              }
            }
            pos = posReine;
            if ((nbPieces >= END_GAME) && (nbPieces <= MIDDLE_GAME))
            {
              mob = pEtat.getQueenTargets(s, traitPiece).length;
            }
            else
            {
              mob = 0;
            }
            break;
          case ROOK :
            if (traitPiece)
            {
              pos = ROOK_POSITIONS[s.getIndex()];
            }
            else
            {
              pos = ROOK_POSITIONS[((RANK_COUNT - 1) - s.getRank()) * FILE_COUNT + s.getFile()];
            }
            if (nbPieces >= END_GAME)
            {
              final int nbPions;
              if (traitPiece == pTrait)
              {
                nbPions = pionsTrait[s.getFile()];
              }
              else
              {
                nbPions = pionsAdversaire[s.getFile()];
              }
              if (nbPions == 0)
              {
                mob = 10;
              }
              else
              {
                mob = 0;
              }
            }
            else
            {
              mob = pEtat.getRookTargets(s, traitPiece).length * 2;
            }
            break;
          default :
            assert false;
            pos = 0;
            mob = 0;
        }
        int att = 0;
        if (typePiece != KING)
        {
          if (pEtat.isAttacked(s, traitPiece))
          {
            att += mat / 20;
          }
          if (pEtat.isAttacked(s, !traitPiece))
          {
            att -= mat / 10;
          }
        }
        final int score = mat + pos + mob + att;
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
