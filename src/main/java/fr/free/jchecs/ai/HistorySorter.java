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
 * Trie les mouvements en fonction de l'historique de la recherche, puis par valeur des prises et
 * valeur des pièces.
 * 
 * @author David Cotton
 */
final class HistorySorter implements ContextSorter
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = 6653686322537719840L;

  /** Priorité de base d'un mouvement présent dans l'historique. */
  private static final int HISTORY_MOVE_PRIORITY = 1000000 * QUEEN.getValue();

  /** Historique des mouvements. */
  private final int [] _history = new int [ 64 * 64 ];

  /**
   * Crée une nouvelle instance.
   */
  HistorySorter()
  {
    // Rien de spécifique...
  }

  /**
   * Efface l'historique.
   */
  public void clear()
  {
    Arrays.fill(_history, 0);
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

    return _history[pMouvement.getFrom().getIndex() + 64 * pMouvement.getTo().getIndex()]
        + HISTORY_MOVE_PRIORITY;
  }

  /**
   * Ajoute un mouvement à l'historique.
   * 
   * @param pMouvement Mouvement à mémoriser.
   */
  public void put(final Move pMouvement)
  {
    assert pMouvement != null;

    _history[pMouvement.getFrom().getIndex() + 64 * pMouvement.getTo().getIndex()]++;
  }
}
