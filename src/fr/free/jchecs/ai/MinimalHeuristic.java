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

import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Piece;
import fr.free.jchecs.core.Square;

/**
 * Implémentation d'une fonction d'évaluation minimale : se base uniquement le matériel,
 * c'est-à-dire la somme des valeurs théoriques des pièces présentes sur le plateau.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
final class MinimalHeuristic implements Heuristic
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -6580558874227828006L;

  /**
   * Crée une nouvelle instance.
   */
  MinimalHeuristic()
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

    int res = 0;

    for (final Square s : Square.values())
    {
      final Piece piece = pEtat.getPieceAt(s);
      if (piece != null)
      {
        final int valeur = piece.getType().getValue();
        if (piece.isWhite() == pTrait)
        {
          res += valeur;
        }
        else
        {
          res -= valeur;
        }
      }
    }

    return res;
  }
}
