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

import java.util.Comparator;

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Interface présentée par les moteurs d'IA pour le jeu d'échecs.
 * 
 * @author David Cotton
 */
public interface Engine
{
  /**
   * Renvoi le temps total passé en traitement par le moteur.
   * 
   * @return Temps total de traitement par le moteur (en ms).
   */
  long getElapsedTime();

  /**
   * Renvoi le nombre total de demi-coups évalués par le moteur.
   * 
   * @return Nombre total de demi-coups évalués par le moteur.
   */
  int getHalfmoveCount();

  /**
   * Renvoi la fonction d'évaluation utilisée par le moteur.
   * 
   * @return Fonction d'évaluation utilisée.
   */
  Heuristic getHeuristic();

  /**
   * Renvoi la limite haute de la profondeur de recherche supportées par le moteur.
   * 
   * @return Limite haute de la profondeur de recherche (>= getMinimalSearchDepth()).
   */
  int getMaximalSearchDepth();

  /**
   * Renvoi la limite basse de la profondeur de recherche supportées par le moteur.
   * 
   * @return Limite basse de la profondeur de recherche (>= 1).
   */
  int getMinimalSearchDepth();

  /**
   * Recherche un mouvement répondant à un état de l'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @return Mouvement trouvé.
   */
  Move getMoveFor(final MoveGenerator pEtat);

  /**
   * Renvoi la fonction de tri des mouvements.
   * 
   * @return Fonction de tri des mouvements.
   */
  Comparator<Move> getMoveSorter();

  /**
   * Renvoi le score obtenu par le dernier mouvement calculé.
   * 
   * @return Score du dernier mouvement.
   */
  int getScore();

  /**
   * Renvoi la valeur limite de la profondeur de recherche (en demi-coups).
   * 
   * @return Limite de la profondeur de recherche ([getMinimalSearchDepth(),
   *         getMaximalSearchDepth()]).
   */
  int getSearchDepthLimit();

  /**
   * Indique si l'utilisation de la bibliothèque d'ouvertures est activée.
   * 
   * @return "true" si les ouvertures sont utilisées, "false" sinon.
   */
  boolean isOpeningsEnabled();

  /**
   * Modifie la fonction d'évaluation utilisée par le moteur.
   * 
   * @param pHeuristique Nouvelle fonction d'évaluation à utiliser.
   */
  void setHeuristic(final Heuristic pHeuristique);

  /**
   * Modifie la fonction d'ordenancement des mouvements.
   * 
   * @param pComparateur Nouvelle fonction de tri des mouvements.
   */
  void setMoveSorter(final Comparator<Move> pComparateur);

  /**
   * Active / désactive l'utilisation de la bibliothèque d'ouvertures.
   * 
   * @param pActif A "true" pour activer l'utilisation des ouvertures, à "false" sinon.
   */
  void setOpeningsEnabled(final boolean pActif);

  /**
   * Aliment la valeur de la limite de la profondeur de recherche (en demi-coups).
   * 
   * @param pLimite Limite de la profondeur de recherche ([getMinimalSearchDepth(),
   *          getMaximalSearchDepth()]).
   */
  void setSearchDepthLimit(final int pLimite);
}
