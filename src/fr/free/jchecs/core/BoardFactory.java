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
 * Classe utilitaire délivrant des instances de représentation d'un état de jeu.
 * 
 * @author David Cotton
 */
public final class BoardFactory
{
  /**
   * Classe utilitaire : ne pas instancier.
   */
  private BoardFactory()
  {
    // Rien de spécifique...
  }

  /**
   * Renvoi une instance de description de l'état d'une partie.
   * 
   * @param pType Type de la méthode de représentation de l'instance souhaitée.
   * @param pEtat Etat de la partie.
   * @return Instance correspondante.
   */
  public static MoveGenerator valueOf(final Type pType, final State pEtat)
  {
    assert pType != null;
    assert pEtat != null;

    MoveGenerator res = null;
    switch (pType)
    {
      case ARRAY :
        switch (pEtat)
        {
          case EMPTY :
            res = ArrayBoard.EMPTY;
            break;
          case STARTING :
            res = ArrayBoard.STARTING;
            break;
          default :
            assert false;
        }
        break;
      // case BITMAPS :
      // switch (pEtat)
      // {
      // case EMPTY :
      // res = BitmapsBoard.EMPTY;
      // break;
      // case STARTING :
      // res = BitmapsBoard.STARTING;
      // break;
      // default :
      // assert false;
      // }
      // break;
      case FASTEST :
      case MAILBOX :
        switch (pEtat)
        {
          case EMPTY :
            res = new MailboxBoard(ArrayBoard.EMPTY);
            break;
          case STARTING :
            res = new MailboxBoard(ArrayBoard.STARTING);
            break;
          default :
            assert false;
        }
        break;
      case X88 :
        switch (pEtat)
        {
          case EMPTY :
            res = new X88Board(ArrayBoard.EMPTY);
            break;
          case STARTING :
            res = new X88Board(ArrayBoard.STARTING);
            break;
          default :
            assert false;
        }
        break;
      default :
        assert false;
    }

    assert res != null;
    return res;
  }

  /** Enumération des états initiaux reconnus. */
  public static enum State
  {
    /** Etat initial, sans pièces. */
    EMPTY,

    /** Etat initial standard. */
    STARTING;
  }

  /** Enumération des types de représentation d'une partie disponibles. */
  public static enum Type
  {
    /** Description basée sur un tableau à deux dimensions. */
    ARRAY,

    /** Description basée sur un tableau bordé, à une dimension. */
    MAILBOX,

    /** Description la plus rapide : actuellement équivalent à MAILBOX. */
    FASTEST,

    /** Description basée sur un tableau à une dimension avec indice filtré par la valeur 0x88. */
    X88;

    // /** Description basée sur des cartes binaires. */
    // BITMAPS;
  }
}
