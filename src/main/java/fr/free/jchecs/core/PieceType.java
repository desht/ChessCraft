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
 * Enumération des types de pièces.
 * <p>
 * Les instances de cette classe sont des <b>singletons immuables</b> : classe sûre vis-à-vis des
 * threads et permettant des comparaisons directes sur les références d'objets.
 * </p>
 * 
 * @author David Cotton
 */
public enum PieceType
{
  /** Fou. */
  BISHOP("B", 350),

  /** Roi. */
  KING("K", 0),

  /** Cavalier. */
  KNIGHT("N", 300),

  /** Pion. */
  PAWN("", 100),

  /** Reine. */
  QUEEN("Q", 1000),

  /** Tour. */
  ROOK("R", 550);

  /** Caractère identifiant le type de la pièce en notation SAN. */
  private final String _sanLetter;

  /** Valeur théorique du type de pièce. */
  private final int _value;

  /**
   * Instancie un type de pièce.
   * 
   * @param pLettre Caractère identifiant SAN.
   * @param pValeur Valeur théorique du type de pièce.
   */
  private PieceType(final String pLettre, final int pValeur)
  {
    assert pLettre != null;
    assert pLettre.length() <= 1;
    assert pValeur >= 0;

    _sanLetter = pLettre;
    _value = pValeur;
  }

  /**
   * Renvoi le caractère SAN identifiant la pièce.
   * 
   * @return Caractère SAN.
   */
  public String getSANLetter()
  {
    assert _sanLetter != null;
    return _sanLetter;
  }

  /**
   * Renvoi la valeur théorique du type de pièce.
   * 
   * @return Valeur théorique.
   */
  public int getValue()
  {
    return _value;
  }
}
