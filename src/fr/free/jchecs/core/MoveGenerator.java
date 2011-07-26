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

/**
 * Interface mise à disposition par les classes permettant de générer des mouvements.
 * 
 * @author David Cotton
 */
public interface MoveGenerator extends Board
{
  /**
   * Renvoi une nouvelle instance, initialisée à partir d'un état quelconque.
   * 
   * @param pEtat Etat de départ.
   * @return Copie de l'état.
   */
  MoveGenerator derive(final Board pEtat);

  /**
   * Renvoi une nouvelle instance décrivant l'état du jeu après application d'un mouvement.
   * 
   * @param pMouvement Description de mouvement.
   * @param pSuivant Drapeau positionné si l'on souhaite que le trait soit modifié.
   * @return Instance dérivée.
   */
  MoveGenerator derive(final Move pMouvement, final boolean pSuivant);

  /**
   * Renvoi toutes les cases cibles des mouvements possibles (y compris ceux mettant le roi en
   * échec) pour la pièce contenue par une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getAllTargets(final Square pOrigine);

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "fou" d'une certaine couleur (y
   * compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Positionné à vrai si la recherche concerne les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getBishopTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi la case contenant le roi d'une couleur.
   * 
   * @param pCouleur Mis à "true" si l'on recherche le roi blanc, à "false" sinon.
   * @return Case contenant le roi.
   */
  Square getKingSquare(final boolean pCouleur);

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type roi.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getKingTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type cavalier.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getKnightTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type pion.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getPawnTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "dame" d'une certaine couleur
   * (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Mis à vrai pour rechercher pour les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getQueenTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "tour" d'une certaine couleur
   * (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Mis à vrai pour rechercher pour les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  Square [] getRookTargets(final Square pOrigine, final boolean pBlanc);

  /**
   * Renvoi tous les mouvements valides pour une couleur.
   * 
   * @param pTrait Positionné à "true" pour indiquer une recherche pour les blancs.
   * @return Liste des mouvements valides.
   */
  Move [] getValidMoves(final boolean pTrait);

  /**
   * Renvoi toutes les cases cibles des mouvements valides à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @return Liste des cases cibles.
   */
  Square [] getValidTargets(final Square pOrigine);

  /**
   * Indique si une case est attaquée par une couleur.
   * 
   * @param pCible Case cible.
   * @param pCouleur Positionné à "true" pour tester l'attaque par les blancs.
   * @return Vrai si la case est attaquée.
   */
  boolean isAttacked(final Square pCible, final boolean pCouleur);

  /**
   * Indique si le roi d'une couleur a roqué.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return A "vrai" si le roi correspondant à roqué.
   */
  boolean isCastled(final boolean pBlanc);

  /**
   * Indique si le roi d'une couleur est en échec.
   * 
   * @param pCouleur Positionné à "true" pour tester l'échec sur les blancs, à "false" sinon.
   * @return Vrai si le roi est en échec.
   */
  boolean isInCheck(final boolean pCouleur);
}
