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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static fr.free.jchecs.core.BoardFactory.State.STARTING;
import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;
import static fr.free.jchecs.core.Constants.APPLICATION_VERSION;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.FENUtils.toFEN;
import static fr.free.jchecs.core.PGNUtils.toGame;
import static fr.free.jchecs.core.PGNUtils.toNormalizedSAN;
import static fr.free.jchecs.core.PGNUtils.toPGN;
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_KNIGHT;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import fr.free.jchecs.ai.EngineFactory;

/**
 * Tests unitaires de la classe utilitaire pour la notation PGN.
 * 
 * @author David Cotton
 */
public final class PGNUtilsTest
{
  /** Nom de la machine. */
  private final String _site;

  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public PGNUtilsTest()
  {
    String site = "?";
    try
    {
      final InetAddress lh = InetAddress.getLocalHost();
      site = lh.getHostName();
    }
    catch (final UnknownHostException e)
    {
      // Pas grave, on peut se passer de cette information...
    }
    _site = site;
  }

  /**
   * Teste la convertion flux PGN / dscription de partie.
   */
  @Test
  public void testToGame()
  {
    final String pgn1 = "[Event \"jChecs vX.X.X chess game\"]\n" //
        + "[Site \"" + _site + "\"]\n" //
        + "[Date \"2006.12.31\"]\n" //
        + "[Round \"-\"]\n" //
        + "[White \"jChecs.AlphaBeta\"]\n" //
        + "[Black \"Test\"]\n" //
        + "[Result \"*\"]\n" //
        + "\n" //
        + "1. Pa4 h6? 2. N@c3 *\n";

    try
    {
      final Game partie = toGame(new BufferedReader(new StringReader(pgn1)));
      Player joueur = partie.getPlayer(true);
      assertEquals("AlphaBeta", joueur.getName());
      assertEquals(EngineFactory.newInstance("jChecs.AlphaBeta").getClass(), joueur.getEngine()
          .getClass());
      joueur = partie.getPlayer(false);
      assertEquals("Test", joueur.getName());
      assertNull(joueur.getEngine());
      assertTrue(partie.getState() == Game.State.IN_PROGRESS);
      assertEquals("rnbqkbnr/ppppppp1/7p/8/P7/2N5/1PPPPPPP/R1BQKBNR b KQkq - 1 2", toFEN(partie
          .getBoard()));
    }
    catch (final PGNException e)
    {
      fail(e.toString());
    }

    final String pgn2 = "[Event \"jChecs vX.X.X chess game\"]\n" //
        + "[Site \"" + _site + "\"]\n" //
        + "[Date \"2006.12.31\"]\n" //
        + "[Round \"-\"]\n" //
        + "[White \"\"]\n"//
        + "[Black \"\"]\n"//
        + "[Result \"*\"]\n"//
        + "[SetUp \"1\"]\n"//
        + "[FEN \"rnbqkb1r/pp3ppp/4pn2/2pp4/3P4/2PBP3/PP3PPP/RNBQK1NR w KQkq c6 0 5\"]\n"//
        + "\n"//
        + "5. e4 *\n";

    try
    {
      final Game partie = toGame(new BufferedReader(new StringReader(pgn2)));
      Player joueur = partie.getPlayer(true);
      assertEquals("", joueur.getName());
      assertNull(joueur.getEngine());
      joueur = partie.getPlayer(false);
      assertEquals("", joueur.getName());
      assertNull(joueur.getEngine());
      assertTrue(partie.getState() == Game.State.IN_PROGRESS);
      assertEquals("rnbqkb1r/pp3ppp/4pn2/2pp4/3P4/2PBP3/PP3PPP/RNBQK1NR w KQkq c6 0 5", partie
          .getStartingPosition());
      assertEquals("rnbqkb1r/pp3ppp/4pn2/2pp4/3PP3/2PB4/PP3PPP/RNBQK1NR b KQkq - 0 5", toFEN(partie
          .getBoard()));
    }
    catch (final PGNException e)
    {
      fail(e.toString());
    }
  }

  /**
   * Teste la convertion mouvement au format PGN / mouvement SAN standard.
   */
  @Test
  public void testToNormalizedSAN()
  {
    assertEquals("e3", toNormalizedSAN("e3"));
    assertEquals("e3", toNormalizedSAN("Pe3"));
    assertEquals("a8Q", toNormalizedSAN("a8Q"));
    assertEquals("a8Q", toNormalizedSAN("a8=Q"));
    assertEquals("Rd6", toNormalizedSAN("Rd6"));
    assertEquals("Rd6", toNormalizedSAN("Rd6?"));
    assertEquals("Rd6", toNormalizedSAN("Rd6!"));
    assertEquals("Rd6", toNormalizedSAN("Rd6!?"));
    assertEquals("Rd6", toNormalizedSAN("Rd6?!"));
    assertEquals("Rd6", toNormalizedSAN("Rd6!!"));
    assertEquals("Rd6", toNormalizedSAN("Rd6??"));
    assertEquals("Bc3", toNormalizedSAN("Bc3"));
    assertEquals("Bc3", toNormalizedSAN("B@c3"));
  }

  /**
   * Teste la convertion description de partie / chaine PGN.
   */
  @Test
  public void testToPGN()
  {
    final Game partie1 = new Game();
    partie1.moveFromCurrent(new Move(WHITE_PAWN, Square.valueOf("a2"), Square.valueOf("a4")));
    partie1.moveFromCurrent(new Move(BLACK_PAWN, Square.valueOf("g7"), Square.valueOf("g6")));
    partie1.moveFromCurrent(new Move(WHITE_KNIGHT, Square.valueOf("b1"), Square.valueOf("c3")));
    Player joueur = partie1.getPlayer(true);
    joueur.setEngine(null);
    joueur.setName("Test");
    joueur = partie1.getPlayer(false);
    joueur.setEngine(EngineFactory.newInstance("jChecs.NegaScout"));
    joueur.setName("NegaScout");

    final StringBuffer attendu1 = new StringBuffer("[Event \"jChecs v");
    attendu1.append(APPLICATION_VERSION).append(" chess game\"]\n");
    attendu1.append("[Site \"" + _site + "\"]\n");
    attendu1.append("[Date \"").append(new SimpleDateFormat("yyyy.MM.dd").format(new Date()))
        .append("\"]\n");
    attendu1.append("[Round \"-\"]\n");
    attendu1.append("[White \"Test\"]\n");
    attendu1.append("[Black \"jChecs.NegaScout\"]\n");
    attendu1.append("[Result \"*\"]\n");
    attendu1.append("\n");
    attendu1.append("1. a4 g6 2. Nc3 *\n");

    assertEquals(attendu1.toString(), toPGN(partie1));

    final Game partie2 = new Game();
    final String fen = "rnbqkb1r/pp3ppp/4pn2/2pp4/3P4/2PBP3/PP3PPP/RNBQK1NR w KQkq c6 0 5";
    Board plateau = null;
    try
    {
      plateau = toBoard(fen);
    }
    catch (final FENException e)
    {
      fail(e.toString());
    }
    partie2.resetTo(BoardFactory.valueOf(FASTEST, STARTING).derive(plateau));
    partie2.moveFromCurrent(new Move(WHITE_PAWN, Square.valueOf("e3"), Square.valueOf("e4")));

    final StringBuffer attendu2 = new StringBuffer("[Event \"jChecs v");
    attendu2.append(APPLICATION_VERSION).append(" chess game\"]\n");
    attendu2.append("[Site \"" + _site + "\"]\n");
    attendu2.append("[Date \"").append(new SimpleDateFormat("yyyy.MM.dd").format(new Date()))
        .append("\"]\n");
    attendu2.append("[Round \"-\"]\n");
    attendu2.append("[White \"\"]\n");
    attendu2.append("[Black \"\"]\n");
    attendu2.append("[Result \"*\"]\n");
    attendu2.append("[SetUp \"1\"]\n");
    attendu2.append("[FEN \"" + fen + "\"]\n");
    attendu2.append("\n");
    attendu2.append("5. e4 *\n");

    assertEquals(attendu2.toString(), toPGN(partie2));
  }

  /**
   * Teste la méthode "toPGN(null)".
   */
  @Test(expected = NullPointerException.class)
  public void testToPGNNull()
  {
    toPGN(null);
  }
}
