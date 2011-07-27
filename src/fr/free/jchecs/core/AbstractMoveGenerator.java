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

import static fr.free.jchecs.core.PieceType.KING;

/**
 * Squelette de l'implémentation d'une classe représentant un état de la partie permettant de
 * générer des mouvements.
 * 
 * @author David Cotton
 */
@SuppressWarnings("serial")
abstract class AbstractMoveGenerator extends AbstractBoard implements MoveGenerator
{
  /** Position du roi noir. */
  private Square _blackKingSquare;

  /** Drapeau indiquant si le roi noir à roqué. */
  private boolean _blackCastled;

  /** Position du roi blanc. */
  private Square _whiteKingSquare;

  /** Drapeau indiquant si le roi blanc à roqué. */
  private boolean _whiteCastled;

  /**
   * Crée une nouvelle instance.
   */
  protected AbstractMoveGenerator()
  {
    // Rien de spécifique...
  }

  /**
   * Crée une nouvelle instance, initialisée à partir de l'état reçu.
   * 
   * @param pEtat Instance initiale.
   */
  protected AbstractMoveGenerator(final Board pEtat)
  {
    super(pEtat);

    for (final Square s : Square.values())
    {
      final Piece p = pEtat.getPieceAt(s);
      if ((p != null) && (p.getType() == KING))
      {
        setKingSquare(p.isWhite(), s);
      }
    }
  }

  /**
   * Crée une nouvelle instance, copiée à partir de l'instance reçue.
   * 
   * @param pEtat Instance à copier.
   */
  protected AbstractMoveGenerator(final AbstractMoveGenerator pEtat)
  {
    super(pEtat);

    _blackKingSquare = pEtat._blackKingSquare;
    _blackCastled = pEtat._blackCastled;
    _whiteKingSquare = pEtat._whiteKingSquare;
    _whiteCastled = pEtat._whiteCastled;
  }

  /**
   * Renvoi la case contenant le roi d'une couleur.
   * 
   * @param pCouleur Mis à "true" si l'on recherche le roi blanc, à "false" sinon.
   * @return Case contenant le roi.
   */
  public Square getKingSquare(final boolean pCouleur)
  {
    if (pCouleur)
    {
      assert _whiteKingSquare != null;
      return _whiteKingSquare;
    }

    assert _blackKingSquare != null;
    return _blackKingSquare;
  }

  /**
   * Indique si le roi d'une couleur a roqué.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return A "vrai" si le roi correspondant à roqué.
   */
  public final boolean isCastled(final boolean pBlanc)
  {
    if (pBlanc)
    {
      return _whiteCastled;
    }

    return _blackCastled;
  }

  /**
   * Alimente l'indicateur de roi ayant roqué pour une couleur.
   * 
   * @param pBlanc Positionné à "true" pour alimenter l'état des blancs.
   * @param pEtat Etat de l'indicateur de roque effectué pour la couleur.
   */
  protected final void setCastled(final boolean pBlanc, final boolean pEtat)
  {
    if (pBlanc)
    {
      _whiteCastled = pEtat;
    }
    else
    {
      _blackCastled = pEtat;
    }
  }

  /**
   * Alimente la case contenant le roi d'une couleur.
   * 
   * @param pCouleur Mis à "true" si l'on alimente le roi blanc, à "false" sinon.
   * @param pCase Case contenant le roi.
   */
  protected final void setKingSquare(final boolean pCouleur, final Square pCase)
  {
    assert pCase != null;

    if (pCouleur)
    {
      _whiteKingSquare = pCase;
    }
    else
    {
      _blackKingSquare = pCase;
    }
  }
}
