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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static fr.free.jchecs.core.Constants.FILE_COUNT;
import static fr.free.jchecs.core.Constants.RANK_COUNT;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.Piece.BLACK_BISHOP;
import static fr.free.jchecs.core.Piece.BLACK_KING;
import static fr.free.jchecs.core.Piece.BLACK_KNIGHT;
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.BLACK_QUEEN;
import static fr.free.jchecs.core.Piece.BLACK_ROOK;
import static fr.free.jchecs.core.Piece.WHITE_BISHOP;
import static fr.free.jchecs.core.Piece.WHITE_KING;
import static fr.free.jchecs.core.Piece.WHITE_KNIGHT;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_QUEEN;
import static fr.free.jchecs.core.Piece.WHITE_ROOK;

import org.junit.Test;

/**
 * Tests unitaires des classes représentant des états de la partie.
 * 
 * @author David Cotton
 */
public final class BoardTest
{
  /** Etat de test n°1. */
  private Board _board1;

  /** Etat de test n°2. */
  private Board _board2;

  /** Etat de test n°3. */
  private Board _board3;

  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public BoardTest()
  {
    try
    {
      _board1 = toBoard("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2P5/PP3PPP/RNBQKBNR b KQkq d3 0 10");
      _board2 = toBoard("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2P5/PP3PPP/RNBQKBNR b KQkq e3 0 10");
      _board3 = toBoard("r1bqkbnr/pppp1ppp/2n5/4p3/3PP3/2P5/PP3PPP/RNBQKBNR b - e3 0 10");
    }
    catch (final FENException e)
    {
      System.out.println(e);
    }
  }

  /**
   * Teste la méthode "compareTo(x)".
   */
  @Test
  public void testCompareTo()
  {
    assertTrue(_board1.compareTo(_board1) == 0);

    assertTrue(_board1.compareTo(_board2) != 0);
    assertTrue(_board1.compareTo(_board3) != 0);
    assertTrue(_board2.compareTo(_board3) != 0);

    assertTrue(_board1.compareTo(_board2) == -_board2.compareTo(_board1));
    assertTrue(_board1.compareTo(_board3) == -_board3.compareTo(_board1));
    assertTrue(_board2.compareTo(_board3) == -_board3.compareTo(_board2));
  }

  /**
   * Teste la méthode "compareTo(null)".
   */
  @Test(expected = NullPointerException.class)
  public void testCompareToNull()
  {
    _board1.compareTo(null);
  }

  /**
   * Teste l'état initial vide.
   */
  @Test
  public void testEmpty()
  {
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      validateEmpty(BoardFactory.valueOf(t, BoardFactory.State.EMPTY));
    }
  }

  /**
   * Teste la méthode "equals".
   */
  @Test
  public void testEquals()
  {
    assertTrue(_board1.equals(_board1));
    assertFalse(_board1.equals(_board2));
    assertFalse(_board1.equals(_board3));
    assertFalse(_board2.equals(_board3));
    assertFalse(_board3.equals(_board1));
    assertFalse(_board3.equals(_board2));
    assertFalse(_board2.equals(_board1));

    for (final BoardFactory.State s : BoardFactory.State.values())
    {
      Board etatPrec = null;
      for (final BoardFactory.Type t : BoardFactory.Type.values())
      {
        final Board etat = BoardFactory.valueOf(t, s);
        final String nomClasse = etat.getClass().getSimpleName();
        if (etatPrec != null)
        {
          assertTrue(nomClasse, etat.equals(etat));
          assertTrue(nomClasse, etatPrec.equals(etatPrec));
          assertTrue(nomClasse, etatPrec.equals(etat));
          assertTrue(nomClasse, etat.equals(etatPrec));
          assertFalse(nomClasse, etat.equals(null));
          assertFalse(nomClasse, etatPrec.equals(null));
        }
        etatPrec = etat;
      }
    }

    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      Board etatPrec = null;
      for (final BoardFactory.State s : BoardFactory.State.values())
      {
        final Board etat = BoardFactory.valueOf(t, s);
        final String nomClasse = etat.getClass().getSimpleName();
        if (etatPrec != null)
        {
          assertTrue(nomClasse, etat.equals(etat));
          assertTrue(nomClasse, etatPrec.equals(etatPrec));
          assertFalse(nomClasse, etatPrec.equals(etat));
          assertFalse(nomClasse, etat.equals(etatPrec));
          assertFalse(nomClasse, etat.equals(null));
          assertFalse(nomClasse, etatPrec.equals(null));
        }
        etatPrec = etat;
      }
    }
  }

  /**
   * Teste l'équivalence des méthodes de lecture du contenu d'une case.
   */
  @Test
  public void testGetPieceAt()
  {
    Board etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final Board etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          assertSame(nomClasse, etat.getPieceAt(s), etat.getPieceAt(s.getFile(), s.getRank()));
          assertSame(nomClasse, etat.getPieceAt(s), etatPrec.getPieceAt(s));
          assertSame(nomClasse, etat.getPieceAt(s), etatPrec.getPieceAt(s.getFile(), s.getRank()));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste la méthode de calcul des clés de hachage.
   */
  @Test
  public void testHashCode()
  {
    assertFalse(_board1.hashCode() == _board2.hashCode());

    for (final BoardFactory.State s : BoardFactory.State.values())
    {
      Board etatPrec = null;
      for (final BoardFactory.Type t : BoardFactory.Type.values())
      {
        final Board etat = BoardFactory.valueOf(t, s);
        if (etatPrec != null)
        {
          final String nomClasse = etat.getClass().getSimpleName();
          assertTrue(nomClasse, etatPrec.hashCode() == etat.hashCode());
        }
        etatPrec = etat;
      }
    }
  }

  /**
   * Teste l'état initial standard.
   */
  @Test
  public void testStandard()
  {
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      validateStarting(BoardFactory.valueOf(t, BoardFactory.State.STARTING));
    }
  }

  /**
   * Teste de la méthode "toString".
   */
  @Test
  public void testToString()
  {
    final String attendu =
        "rnbqkbnr\npppppppp\n++++++++\n++++++++\n++++++++\n++++++++\nPPPPPPPP\nRNBQKBNR\n";
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      assertEquals(BoardFactory.valueOf(t, BoardFactory.State.STARTING).toString(), attendu);
    }
  }

  /**
   * Vérifie que l'état reçu corresponde bien à un état "EMPTY".
   * 
   * @param pEtat Etat de jeu.
   */
  private static void validateEmpty(final Board pEtat)
  {
    assert pEtat != null;

    final String nomClasse = pEtat.getClass().getSimpleName();

    for (final Square s : Square.values())
    {
      assertNull(nomClasse, pEtat.getPieceAt(s));
    }

    validateInitialFlags(pEtat);
  }

  /**
   * Vérifie que les drapeaux correspondent bien à un état initial.
   * 
   * @param pEtat Etat de jeu.
   */
  private static void validateInitialFlags(final Board pEtat)
  {
    assert pEtat != null;

    final String nomClasse = pEtat.getClass().getSimpleName();

    assertTrue(nomClasse, pEtat.isWhiteActive());

    assertTrue(nomClasse, pEtat.canCastleLong(true));
    assertTrue(nomClasse, pEtat.canCastleLong(false));
    assertTrue(nomClasse, pEtat.canCastleShort(true));
    assertTrue(nomClasse, pEtat.canCastleShort(false));

    assertTrue(nomClasse, pEtat.getEnPassant() == null);

    assertTrue(nomClasse, pEtat.getHalfmoveCount() == 0);

    assertTrue(nomClasse, pEtat.getFullmoveNumber() == 1);
  }

  /**
   * Vérifie que l'état reçu corresponde bien à un état "EMPTY".
   * 
   * @param pEtat Etat de jeu.
   */
  private static void validateStarting(final Board pEtat)
  {
    assert pEtat != null;

    final String nomClasse = pEtat.getClass().getSimpleName();

    for (int y = 2; y < RANK_COUNT - 2; y++)
    {
      for (int x = 0; x < FILE_COUNT; x++)
      {
        assertNull(nomClasse, pEtat.getPieceAt(x, y));
      }
    }
    for (int x = 0; x < FILE_COUNT; x++)
    {
      assertSame(nomClasse, pEtat.getPieceAt(x, 1), WHITE_PAWN);
      assertSame(nomClasse, pEtat.getPieceAt(x, RANK_COUNT - 2), BLACK_PAWN);
    }
    assertSame(nomClasse, pEtat.getPieceAt(0, 0), WHITE_ROOK);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 1, 0), WHITE_ROOK);
    assertSame(nomClasse, pEtat.getPieceAt(0, RANK_COUNT - 1), BLACK_ROOK);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 1, RANK_COUNT - 1), BLACK_ROOK);
    assertSame(nomClasse, pEtat.getPieceAt(1, 0), WHITE_KNIGHT);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 2, 0), WHITE_KNIGHT);
    assertSame(nomClasse, pEtat.getPieceAt(1, RANK_COUNT - 1), BLACK_KNIGHT);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 2, RANK_COUNT - 1), BLACK_KNIGHT);
    assertSame(nomClasse, pEtat.getPieceAt(2, 0), WHITE_BISHOP);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 3, 0), WHITE_BISHOP);
    assertSame(nomClasse, pEtat.getPieceAt(2, RANK_COUNT - 1), BLACK_BISHOP);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 3, RANK_COUNT - 1), BLACK_BISHOP);
    assertSame(nomClasse, pEtat.getPieceAt(3, 0), WHITE_QUEEN);
    assertSame(nomClasse, pEtat.getPieceAt(3, RANK_COUNT - 1), BLACK_QUEEN);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 4, 0), WHITE_KING);
    assertSame(nomClasse, pEtat.getPieceAt(FILE_COUNT - 4, RANK_COUNT - 1), BLACK_KING);

    validateInitialFlags(pEtat);
  }
}
