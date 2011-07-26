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

import fr.free.jchecs.core.MoveGenerator;

/**
 * Interface présentée par les classes proposant une fonction d'évaluation.
 * 
 * @author David Cotton
 */
public interface Heuristic extends Serializable
{
  /**
   * Renvoi la valeur estimée d'un état du jeu, pour les fonctions de recherche du meilleur coup.
   * 
   * @param pEtat Etat du jeu.
   * @param pTrait Positionné à "true" si l'on veut une évaluation du point de vue des blancs.
   * @return Valeur estimée.
   */
  int evaluate(final MoveGenerator pEtat, final boolean pTrait);
}
