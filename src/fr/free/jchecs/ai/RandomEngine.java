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

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Moteur d'IA se limitant à choisir au hasard l'un des coups possibles.
 * 
 * @author David Cotton
 */
final class RandomEngine extends AbstractEngine
{
  /**
   * Instancie un nouveau moteur IA aléatoire.
   */
  RandomEngine()
  {
    super(1, 1, 1);
  }

  /**
   * Corps de la recherche du "meilleur" demi-coup pour un état de l'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pCoups Liste des mouvement initiaux valides.
   * @return Mouvement trouvé.
   */
  @Override
  protected Move searchMoveFor(final MoveGenerator pEtat, final Move [] pCoups)
  {
    assert pEtat != null;

    final int l = pCoups.length;
    assert pCoups.length > 0;
    addHalfmove(l);
    final Move res = pCoups[RANDOMIZER.nextInt(l)];

    assert res != null;
    return res;
  }
}
