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

import static fr.free.jchecs.core.PieceType.QUEEN;

import java.util.Arrays;

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.Piece;

/**
 * Trie les mouvements en fonction de "killer moves", puis par valeur des prises et valeur des
 * pièces.
 * 
 * @author David Cotton
 */
final class KillerMoveSorter implements ContextSorter
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = 1484760150439005473L;

  /** Priorité donnée à un "killer move". */
  private static final int KILLER_MOVE_PRIORITY = 1000000 * QUEEN.getValue();

  /** Buffer des "killer moves". */
  private final Move [] _killerMoves = new Move [ 5 ];

  /** Indice du prochain emplacement pouvant accueillir un "killer move". */
  private int _nextIndex;

  /**
   * Crée une nouvelle instance.
   */
  KillerMoveSorter()
  {
    // Rien de spécifique...
  }

  /**
   * Efface le buffer des "killer moves".
   */
  public void clear()
  {
    Arrays.fill(_killerMoves, null);
    _nextIndex = 0;
  }

  /**
   * Tri des mouvements.
   * 
   * @param pMvt1 Premier mouvement.
   * @param pMvt2 Deuxième mouvement.
   * @return -1, 0, 1 en accord avec le contrat de compare().
   */
  public int compare(final Move pMvt1, final Move pMvt2)
  {
    int v1 = get(pMvt1);
    int v2 = get(pMvt2);

    final Piece prise1 = pMvt1.getCaptured();
    final int vPiece1 = pMvt1.getPiece().getType().getValue();
    if (prise1 == null)
    {
      v1 += vPiece1;
    }
    else
    {
      v1 += 1000 * prise1.getType().getValue();
      v1 -= vPiece1;
    }

    final Piece prise2 = pMvt2.getCaptured();
    final int vPiece2 = pMvt2.getPiece().getType().getValue();
    if (prise2 == null)
    {
      v2 += vPiece2;
    }
    else
    {
      v2 += 1000 * prise2.getType().getValue();
      v2 -= vPiece2;
    }

    if (v1 > v2)
    {
      return -1;
    }
    else if (v1 < v2)
    {
      return 1;
    }

    return 0;
  }

  /**
   * Renvoie l'évaluation d'un mouvement.
   * 
   * @param pMouvement Mouvement à rechercher.
   * @return Valeur liée au mouvement (ou 0 si le mouvement n'est pas dans la liste).
   */
  public int get(final Move pMouvement)
  {
    assert pMouvement != null;

    if (pMouvement.equals(_killerMoves[0]) || pMouvement.equals(_killerMoves[1])
        || pMouvement.equals(_killerMoves[2]) || pMouvement.equals(_killerMoves[3])
        || pMouvement.equals(_killerMoves[4]))
    {
      return KILLER_MOVE_PRIORITY;
    }

    return 0;
  }

  /**
   * Mémorise un "killer move".
   * 
   * @param pMouvement Mouvement à mémoriser.
   */
  public void put(final Move pMouvement)
  {
    assert pMouvement != null;

    _killerMoves[_nextIndex++] = pMouvement;
    if (_nextIndex >= 5)
    {
      _nextIndex = 0;
    }
  }
}
