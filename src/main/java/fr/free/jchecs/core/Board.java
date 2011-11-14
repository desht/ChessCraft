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

import java.io.Serializable;

/**
 * Interface mise à disposition par les classes décrivant une état instantanné de la partie.
 * <p>
 * Les instances de descriptions d'état doivent être immuables. Les comparaisons et la méthode de
 * calcul des clés de hachage sont supposées compatibles entre les différentes implémentations.
 * </p>
 * 
 * @author David Cotton
 */
public interface Board extends Comparable<Board>, Serializable
{
  /**
   * Renvoi l'état du droit de roquer côté roi (petit roque) pour une couleur.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return Etat du droit de roquer côté roi pour la couleur.
   */
  boolean canCastleLong(final boolean pBlanc);

  /**
   * Renvoi l'état du droit de roquer côté reine (grand roque) pour une couleur.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return Etat du droit de roquer côté reine pour la couleur.
   */
  boolean canCastleShort(final boolean pBlanc);

  /**
   * Renvoi l'éventuelle case cible de la prise "en passant" en cours.
   * 
   * @return Case cible de la price "en passant" (peut être à null).
   */
  Square getEnPassant();

  /**
   * Renvoi le numéro du coup.
   * 
   * @return Numéro de coup (> 0).
   */
  int getFullmoveNumber();

  /**
   * Renvoi la valeur du compteur de demi-coups depuis la dernière prise ou le dernier mouvement de
   * pion.
   * 
   * @return Nombre de demi-coups (>= 0).
   */
  int getHalfmoveCount();

  /**
   * Renvoi l'éventuelle pièce présente sur la case indiquée.
   * 
   * @param pCase Case à tester.
   * @return Pièce présente sur la case (ou null si aucune).
   */
  Piece getPieceAt(final Square pCase);

  /**
   * Renvoi l'éventuelle pièce présente sur la case dont les coordonnées sont indiquées.
   * 
   * @param pColonne Colonne de la case à tester (de 0 à 7).
   * @param pLigne Ligne de la case à tester (de 0 à 7).
   * @return Pièce présente sur la case (ou null).
   */
  Piece getPieceAt(final int pColonne, final int pLigne);

  /**
   * Indique si le trait est aux blancs.
   * 
   * @return "true" si le trait est aux blancs, "false" s'il est aux noirs.
   */
  boolean isWhiteActive();
}
