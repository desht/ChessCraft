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

import static fr.free.jchecs.core.PieceType.BISHOP;
import static fr.free.jchecs.core.PieceType.KING;
import static fr.free.jchecs.core.PieceType.KNIGHT;
import static fr.free.jchecs.core.PieceType.PAWN;
import static fr.free.jchecs.core.PieceType.QUEEN;
import static fr.free.jchecs.core.PieceType.ROOK;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumération des pièces.
 * <p>
 * Les instances de cette classe sont des <b>singletons immuables</b> : classe sûre vis-à-vis des
 * threads et permettant des comparaisons directes sur les références d'objets.
 * </p>
 * 
 * @author David Cotton
 */
public enum Piece
{
  /** Fou noir. */
  BLACK_BISHOP('b', false, BISHOP),

  /** Roi noir. */
  BLACK_KING('k', false, KING),

  /** Cavalier noir. */
  BLACK_KNIGHT('n', false, KNIGHT),

  /** Pion noir. */
  BLACK_PAWN('p', false, PAWN),

  /** Reine noire. */
  BLACK_QUEEN('q', false, QUEEN),

  /** Tour noire. */
  BLACK_ROOK('r', false, ROOK),

  /** Fou blanc. */
  WHITE_BISHOP('B', true, BISHOP),

  /** Roi blanc. */
  WHITE_KING('K', true, KING),

  /** Cavalier blanc. */
  WHITE_KNIGHT('N', true, KNIGHT),

  /** Pion blanc. */
  WHITE_PAWN('P', true, PAWN),

  /** Reine blanche. */
  WHITE_QUEEN('Q', true, QUEEN),

  /** Tour blanche. */
  WHITE_ROOK('R', true, ROOK);

  /** Transpositions lettres FEN / pièces. */
  private static final Map<Character, Piece> FEN_TO_PIECE = new HashMap<Character, Piece>();
  static
  {
    for (final Piece p : values())
    {
      FEN_TO_PIECE.put(Character.valueOf(p.getFENLetter()), p);
    }
  }

  /** Caractère identifiant la pièce en notation FEN. */
  private final char _fenLetter;

  /** Drapeau positionné à vrai si la pièce est blanche. */
  private final boolean _white;

  /** Type de la piece. */
  private final PieceType _type;

  /**
   * Instancie une description de pièce.
   * 
   * @param pLettre Caractère identifiant FEN.
   * @param pBlanc Drapeau à vrai pour une pièce blanche.
   * @param pType Type de la pièce.
   */
  private Piece(final char pLettre, final boolean pBlanc, final PieceType pType)
  {
    assert pType != null;

    _fenLetter = pLettre;
    _white = pBlanc;
    _type = pType;
  }

  /**
   * Renvoi le caractère FEN identifiant la pièce.
   * 
   * @return Caractère FEN.
   */
  public char getFENLetter()
  {
    return _fenLetter;
  }

  /**
   * Renvoi le type de la pièce.
   * 
   * @return Type de la pièce.
   */
  public PieceType getType()
  {
    assert _type != null;
    return _type;
  }

  /**
   * Renvoi vrai si la pièce est blanche.
   * 
   * @return Vrai si la pièce est blanche.
   */
  public boolean isWhite()
  {
    return _white;
  }

  /**
   * Renvoi l'instance de pièce correspond à un caractère FEN.
   * 
   * @param pLettre Caractère identifiant FEN.
   * @return Pièce correspondante.
   */
  public static Piece valueOf(final char pLettre)
  {
    return FEN_TO_PIECE.get(Character.valueOf(pLettre));
  }
}
