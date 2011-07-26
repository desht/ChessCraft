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
 * Description d'un mouvement d'une pièce.
 * <p>
 * Les instances de cette classe sont <b>immuables</b>, donc sûres vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
public final class Move implements Serializable
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = 8372326813848101389L;

  /** Pièce déplacée. */
  private final Piece _piece;

  /** Case de départ. */
  private final Square _from;

  /** Case d'arrivée. */
  private final Square _to;

  /** Eventuelle pièce capturée. */
  private final Piece _captured;

  /** Buffer stockant l'entier identifiant un mouvement. */
  private Integer _id;

  /**
   * Instancie une nouvelle description de mouvement, sans prise.
   * 
   * @param pPiece Pièce à bouger.
   * @param pOrigine Case à l'origine du mouvement.
   * @param pDestination Case finale du mouvement.
   */
  public Move(final Piece pPiece, final Square pOrigine, final Square pDestination)
  {
    assert pPiece != null;
    assert pOrigine != null;
    assert pDestination != null;
    assert pOrigine != pDestination;

    _piece = pPiece;
    _from = pOrigine;
    _to = pDestination;
    _captured = null;
  }

  /**
   * Instancie une nouvelle description de mouvement.
   * 
   * @param pPiece Pièce à bouger.
   * @param pOrigine Case à l'origine du mouvement.
   * @param pDestination Case finale du mouvement.
   * @param pPrise Pièce prise (ou null si aucune).
   */
  public Move(final Piece pPiece, final Square pOrigine, final Square pDestination,
      final Piece pPrise)
  {
    assert pPiece != null;
    assert pOrigine != null;
    assert pDestination != null;
    assert pOrigine != pDestination;
    assert (pPrise == null) || (pPiece.isWhite() != pPrise.isWhite());

    _piece = pPiece;
    _from = pOrigine;
    _to = pDestination;
    _captured = pPrise;
  }

  /**
   * Teste l'égalité entre deux mouvements.
   * 
   * @param pObjet Objet avec lequel comparer le mouvement (peut être à null).
   * @return "true" si l'on a deux mouvements identiques.
   */
  @Override
  public boolean equals(final Object pObjet)
  {
    if (this == pObjet)
    {
      return true;
    }

    if (pObjet instanceof Move)
    {
      final Move m = (Move) pObjet;
      return (_from == m._from) && (_piece == m._piece) && (_to == m._to)
          && (_captured == m._captured);
    }

    return false;
  }

  /**
   * Renvoi l'éventuelle pièce capturée.
   * 
   * @return Pièce capturée (ou null si aucune).
   */
  public Piece getCaptured()
  {
    return _captured;
  }

  /**
   * Renvoi la case de départ du mouvement.
   * 
   * @return Case de départ.
   */
  public Square getFrom()
  {
    assert _from != null;
    return _from;
  }

  /**
   * Renvoi la pièce déplacée.
   * 
   * @return Pièce déplacée.
   */
  public Piece getPiece()
  {
    assert _piece != null;
    return _piece;
  }

  /**
   * Renvoi la case d'arrivée du mouvement.
   * 
   * @return Case d'arrivée.
   */
  public Square getTo()
  {
    assert _to != null;
    return _to;
  }

  /**
   * Renvoi le code de hachage du mouvement.
   * 
   * @return Code de hachage.
   */
  @Override
  public int hashCode()
  {
    return toId();
  }

  /**
   * Renvoi l'entier identifiant un mouvement.
   * 
   * @return Entier identifiant un mouvement.
   */
  public int toId()
  {
    if (_id == null)
    {
      int id = (_piece.ordinal() << 20) + (_from.getIndex() << 14) + (_to.getIndex() << 8);
      if (_captured != null)
      {
        id += (_captured.ordinal() + 1) << 4;
      }
      _id = Integer.valueOf(id);
    }

    return _id.intValue();
  }

  /**
   * Renvoi une chaine représentant le mouvement.
   * 
   * @return Chaine décrivant le mouvement.
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append("[piece=").append(_piece);
    sb.append(",from=").append(_from);
    sb.append(",to=").append(_to);
    sb.append(",captured=").append(_captured).append(']');

    return sb.toString();
  }

  /**
   * Renvoi une instance de mouvement correspondant à l'entier identifiant reçu.
   * 
   * @param pId Entier identifiant un mouvement.
   * @return Instance du mouvement correspondant.
   */
  public static Move valueOf(final int pId)
  {
    final Piece pce = Piece.values()[(pId >> 20) & 0xF];
    final Square src = Square.valueOf((pId >> 14) & 0x3F);
    final Square dst = Square.valueOf((pId >> 8) & 0x3F);
    final int idCpt = (pId >> 4) & 0xF;
    final Piece cpt;
    if (idCpt <= 0)
    {
      cpt = null;
    }
    else
    {
      cpt = Piece.values()[idCpt - 1];
    }

    return new Move(pce, src, dst, cpt);
  }
}
