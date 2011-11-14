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

import java.io.Serializable;
import java.util.Comparator;

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.Piece;

/**
 * Propose un tri statique des mouvements, classant les mouvements avec prise devant les mouvements
 * sans prise. Les mouvements avec prise sont classés de la prise la plus forte à la pris la plus
 * faible.
 * 
 * @author David Cotton
 */
final class StaticMoveSorter implements Comparator<Move>, Serializable
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -1345309675511434759L;

  /**
   * Crée une nouvelle instance.
   */
  StaticMoveSorter()
  {
    // Rien de spécifique...
  }

  /**
   * Tri des mouvements.
   * 
   * @param pMvt1 Premier mouvement.
   * @param pMvt2 Deuxième mouvement.
   * @return -1, 0, 1 en accord avec le contrat de compare().
   * @see Comparator#compare(Object, Object)
   */
  public int compare(final Move pMvt1, final Move pMvt2)
  {
    final Piece prise1 = pMvt1.getCaptured();
    final Piece prise2 = pMvt2.getCaptured();
    if (prise1 == null)
    {
      if (prise2 != null)
      {
        return 1;
      }
    }
    else
    {
      if ((prise2 == null) || (prise1.getType().getValue() > prise2.getType().getValue()))
      {
        return -1;
      }
    }

    final int val1 = pMvt1.getPiece().getType().getValue();
    final int val2 = pMvt2.getPiece().getType().getValue();
    int res = 0;
    if (val1 > val2)
    {
      res = -1;
    }
    else if (val1 < val2)
    {
      res = 1;
    }
    if ((prise1 != null) && (prise2 != null))
    {
      res = -res;
    }

    return res;
  }
}
