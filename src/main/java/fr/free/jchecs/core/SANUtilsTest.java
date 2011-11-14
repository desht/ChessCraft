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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static fr.free.jchecs.core.BoardFactory.State.EMPTY;
import static fr.free.jchecs.core.BoardFactory.State.STARTING;
import static fr.free.jchecs.core.BoardFactory.Type.ARRAY;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.Piece.BLACK_KNIGHT;
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_BISHOP;
import static fr.free.jchecs.core.Piece.WHITE_KNIGHT;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_QUEEN;
import static fr.free.jchecs.core.SANUtils.SAN_VALIDATOR;
import static fr.free.jchecs.core.SANUtils.toMove;
import static fr.free.jchecs.core.SANUtils.toSAN;

import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Tests unitaires de la classe utilitaire pour la notation SAN.
 * 
 * @author David Cotton
 */
public final class SANUtilsTest
{
  /** Chaine FEN d'une position avec une prise ambiguë venant d'un cavalier. */
  private static final String AMBIGUOUS_KNIGHT_FEN =
      "rnbqk1nr/ppp2ppp/3p4/2b1p3/2N5/5N2/PPPPPPPP/R1BQKB1R w KQkq - 6 3";

  /** Chaine FEN d'une position avec une prise ambiguë venant d'un pion. */
  private static final String AMBIGUOUS_PAWN_FEN =
      "r1b1kbnr/pppp1ppp/4p3/6q1/1n6/P1P1P3/1P1P1PPP/RNBQKBNR w KQkq - 6 3";

  /** Chaine FEN d'une position avec une prise ambiguë venant d'une reine. */
  private static final String AMBIGUOUS_QUEEN_FEN = "8/k7/8/1Q1p1Q2/8/5Q2/8/7K w - - 40 40";

  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public SANUtilsTest()
  {
    // Rien de spécifique...
  }

  /**
   * Teste l'expression de validation d'une chaîne SAN.
   */
  @Test
  public void testSANValidator()
  {
    final Pattern regexp = SAN_VALIDATOR;

    // Gestion des roques...
    assertTrue(regexp.matcher("0-0").matches());
    assertTrue(regexp.matcher("0-0-0").matches());
    assertTrue(regexp.matcher("0-0-0#").matches());
    // Répétitions...
    assertFalse(regexp.matcher("0-00-0").matches());
    // Espaces en trop...
    assertFalse(regexp.matcher(" 0-0 ").matches());
    // Des "o" majuscules plutôt que des zéros...
    assertFalse(regexp.matcher("O-O").matches());
    assertFalse(regexp.matcher("O-O-O").matches());

    // Mouvements de pions sans prise, avec promotion...
    assertTrue(regexp.matcher("a8Q").matches());
    assertTrue(regexp.matcher("c1K").matches());
    assertTrue(regexp.matcher("h1Q+").matches());
    assertTrue(regexp.matcher("h1Q++").matches());
    assertTrue(regexp.matcher("h1Q#").matches());
    assertTrue(regexp.matcher("f8Q(=)").matches());
    // Le "=" pour noter une promotion, fréquent dans PGN, ne fait pas partie de SAN.
    assertFalse(regexp.matcher("c1=K").matches());
    // Rangs incompatibles avec une promotion...
    assertFalse(regexp.matcher("b7Q").matches());
    assertFalse(regexp.matcher("g3R").matches());
    // Colonnes ou rangs inconnus...
    assertFalse(regexp.matcher("ab2Q").matches());
    assertFalse(regexp.matcher("i4Q").matches());

    // Mouvements de pions sans prise et sans promotion...
    assertTrue(regexp.matcher("e5").matches());
    assertTrue(regexp.matcher("h8").matches());
    assertTrue(regexp.matcher("e5").matches());
    // Colonnes ou rangs inconnus...
    assertFalse(regexp.matcher("a0").matches());
    assertFalse(regexp.matcher("bc3").matches());
    assertFalse(regexp.matcher("c9").matches());
    assertFalse(regexp.matcher("f12").matches());

    // Mouvements de pions avec prise et sans promotion...
    assertTrue(regexp.matcher("gxf8").matches());
    assertTrue(regexp.matcher("axb3+").matches());
    assertTrue(regexp.matcher("cxd3 e.p.").matches());
    assertTrue(regexp.matcher("cxd6 e.p.").matches());
    // Rang incompatible pour une prise en passant...
    assertFalse(regexp.matcher("cxd7 e.p.").matches());
    assertFalse(regexp.matcher("gxh2 e.p.").matches());
    // Notation de la prise en passant erronée...
    assertFalse(regexp.matcher("dxe3 ep").matches());
    assertFalse(regexp.matcher("fxd6 e.p").matches());
    // Mouvements de pions avec prise et avec promotion...
    assertTrue(regexp.matcher("cxd8Q").matches());
    // Colonne de départ manquante (obligatoire d'après SAN pour les prises avec pion).
    assertFalse(regexp.matcher("xd8Q").matches());

    // Mouvements de pièces sans prise...
    assertTrue(regexp.matcher("Ke1").matches());
    assertTrue(regexp.matcher("Qd3++").matches());
    assertTrue(regexp.matcher("Nef3").matches());
    assertTrue(regexp.matcher("N5d7").matches());
    assertTrue(regexp.matcher("Qe2f3").matches());
    // Levée d'ambiguïté erronée...
    assertFalse(regexp.matcher("Njh2").matches());
    assertFalse(regexp.matcher("N9b7").matches());
    // Promotion d'une pièce ?!
    assertFalse(regexp.matcher("Rh8Q").matches());

    // Mouvements de pièces avec prise...
    assertTrue(regexp.matcher("Kxe1(=)").matches());
    assertTrue(regexp.matcher("Qf5xd5(=)").matches());
    assertTrue(regexp.matcher("Nexf3").matches());
    assertTrue(regexp.matcher("N5xd7").matches());
    // Marqueur de prise mal placé...
    assertFalse(regexp.matcher("Nxef3").matches());
    // Prise en passant avec une pièce !?
    assertFalse(regexp.matcher("Bxe6 e.p.").matches());
  }

  /**
   * Teste la convertion chaine SAN / mouvement.
   */
  @Test
  public void testToMove()
  {
    MoveGenerator etat = BoardFactory.valueOf(ARRAY, STARTING);
    try
    {
      Move mvt = toMove(etat, "e3");
      assertEquals(new Move(WHITE_PAWN, Square.valueOf("e2"), Square.valueOf("e3")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "Na6");
      assertEquals(new Move(BLACK_KNIGHT, Square.valueOf("b8"), Square.valueOf("a6")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "Bc4");
      assertEquals(new Move(WHITE_BISHOP, Square.valueOf("f1"), Square.valueOf("c4")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "c5");
      assertEquals(new Move(BLACK_PAWN, Square.valueOf("c7"), Square.valueOf("c5")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "Qf3");
      assertEquals(new Move(WHITE_QUEEN, Square.valueOf("d1"), Square.valueOf("f3")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "b6");
      assertEquals(new Move(BLACK_PAWN, Square.valueOf("b7"), Square.valueOf("b6")), mvt);
      etat = etat.derive(mvt, true);
      mvt = toMove(etat, "Qxf7++");
      assertEquals(new Move(WHITE_QUEEN, Square.valueOf("f3"), Square.valueOf("f7"), BLACK_PAWN),
          mvt);
      etat = etat.derive(mvt, true);
    }
    catch (final SANException e)
    {
      fail(e.toString());
    }
    assertFalse(etat.isWhiteActive());
    assertTrue(etat.isInCheck(false));

    try
    {
      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_PAWN_FEN));
      Move mvt = toMove(etat, "cxb4");
      assertEquals(new Move(WHITE_PAWN, Square.valueOf("c3"), Square.valueOf("b4"), BLACK_KNIGHT),
          mvt);

      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_KNIGHT_FEN));
      mvt = toMove(etat, "Nfxe5");
      assertEquals(new Move(WHITE_KNIGHT, Square.valueOf("f3"), Square.valueOf("e5"), BLACK_PAWN),
          mvt);

      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_QUEEN_FEN));
      mvt = toMove(etat, "Qf5xd5(=)");
      assertEquals(new Move(WHITE_QUEEN, Square.valueOf("f5"), Square.valueOf("d5"), BLACK_PAWN),
          mvt);
    }
    catch (final FENException e)
    {
      fail(e.toString());
    }
    catch (final SANException e)
    {
      fail(e.toString());
    }
  }

  /**
   * Teste la convertion mouvement / chaine SAN.
   */
  @Test
  public void testToSAN()
  {
    MoveGenerator etat = BoardFactory.valueOf(ARRAY, STARTING);
    Move mvt = new Move(WHITE_PAWN, Square.valueOf("e2"), Square.valueOf("e3"));
    assertEquals("e3", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(BLACK_KNIGHT, Square.valueOf("b8"), Square.valueOf("a6"));
    assertEquals("Na6", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(WHITE_BISHOP, Square.valueOf("f1"), Square.valueOf("c4"));
    assertEquals("Bc4", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(BLACK_PAWN, Square.valueOf("c7"), Square.valueOf("c5"));
    assertEquals("c5", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(WHITE_QUEEN, Square.valueOf("d1"), Square.valueOf("f3"));
    assertEquals("Qf3", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(BLACK_PAWN, Square.valueOf("b7"), Square.valueOf("b6"));
    assertEquals("b6", toSAN(etat, mvt));
    etat = etat.derive(mvt, true);
    mvt = new Move(WHITE_QUEEN, Square.valueOf("f3"), Square.valueOf("f7"), BLACK_PAWN);
    assertEquals("Qxf7++", toSAN(etat, mvt));

    try
    {
      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_KNIGHT_FEN));
      mvt = new Move(WHITE_KNIGHT, Square.valueOf("f3"), Square.valueOf("e5"), BLACK_PAWN);
      assertEquals("Nfxe5", toSAN(etat, mvt));
      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_PAWN_FEN));
      mvt = new Move(WHITE_PAWN, Square.valueOf("c3"), Square.valueOf("b4"), BLACK_KNIGHT);
      assertEquals("cxb4", toSAN(etat, mvt));
      etat = BoardFactory.valueOf(ARRAY, EMPTY).derive(toBoard(AMBIGUOUS_QUEEN_FEN));
      mvt = new Move(WHITE_QUEEN, Square.valueOf("f5"), Square.valueOf("d5"), BLACK_PAWN);
      assertEquals("Qf5xd5(=)", toSAN(etat, mvt));
    }
    catch (final FENException e)
    {
      fail(e.toString());
    }
  }
}
