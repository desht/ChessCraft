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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_BISHOP;

import org.junit.Test;

/**
 * Tests unitaires de la classe représentant un mouvement.
 * 
 * @author David Cotton
 */
public final class MoveTest
{
  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public MoveTest()
  {
    // Rien de spécifique...
  }

  /**
   * Teste la méthode "equals".
   */
  @Test
  public void testEquals()
  {
    final Move mvt1 = new Move(BLACK_PAWN, Square.valueOf("a2"), Square.valueOf("a4"));
    final Move mvt2 = new Move(BLACK_PAWN, Square.valueOf(8), Square.valueOf(24));
    final Move mvt3 = new Move(BLACK_PAWN, Square.valueOf(8), Square.valueOf(16));

    assertSame(mvt1, mvt1);
    assertEquals(mvt1, mvt1);

    assertNotSame(mvt1, mvt2);
    assertEquals(mvt1, mvt2);
    assertEquals(mvt2, mvt1);

    assertNotSame(mvt1, mvt3);
    assertFalse(mvt1.equals(mvt3));
    assertFalse(mvt3.equals(mvt1));

    assertNotSame(mvt2, mvt3);
    assertFalse(mvt2.equals(mvt3));
    assertFalse(mvt3.equals(mvt2));

    assertFalse(mvt1.equals(null));
  }

  /**
   * Teste la méthode de calcul des clés de hachage.
   */
  @Test
  public void testHashCode()
  {
    final Move mvt1 = new Move(BLACK_PAWN, Square.valueOf("a2"), Square.valueOf("a4"));
    final Move mvt2 = new Move(BLACK_PAWN, Square.valueOf(8), Square.valueOf(24));
    final Move mvt3 = new Move(BLACK_PAWN, Square.valueOf(16), Square.valueOf(24));

    assertTrue(mvt1.hashCode() == mvt2.hashCode());
    assertTrue(mvt1.hashCode() != mvt3.hashCode());
    assertTrue(mvt2.hashCode() != mvt3.hashCode());
  }

  /**
   * Teste la méthode de calcul des entiers identifiants de mouvements.
   */
  @Test
  public void testToId()
  {
    final Move mvt1 = new Move(BLACK_PAWN, Square.valueOf("a2"), Square.valueOf("a4"));
    final Move mvt2 = new Move(BLACK_PAWN, Square.valueOf(8), Square.valueOf(24));
    final Move mvt3 =
        new Move(WHITE_BISHOP, Square.valueOf("a3"), Square.valueOf("d6"), BLACK_PAWN);

    assertTrue(mvt1.toId() == mvt2.toId());
    assertTrue(mvt1.toId() != mvt3.toId());
    assertEquals(mvt1, Move.valueOf(mvt1.toId()));
    assertEquals(mvt1, Move.valueOf(mvt2.toId()));
    assertFalse(mvt1.equals(Move.valueOf(mvt3.toId())));
  }

  /**
   * Teste la méthode "toString".
   */
  @Test
  public void testToString()
  {
    final Move mvt = new Move(WHITE_BISHOP, Square.valueOf("a3"), Square.valueOf("d6"), BLACK_PAWN);
    final String attendu =
        "Move[piece=WHITE_BISHOP,from=Square[file=a,rank=3],"
            + "to=Square[file=d,rank=6],captured=BLACK_PAWN]";

    assertEquals(attendu, mvt.toString());
  }
}
