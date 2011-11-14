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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;
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

import java.util.Random;

import org.junit.Test;

/**
 * Tests unitaires des classes générant des états de la partie.
 * 
 * @author David Cotton
 */
public final class MoveGeneratorTest
{
  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public MoveGeneratorTest()
  {
    // Rien de spécifique...
  }

  /**
   * Vérifie si deux listes de mouvements sont identiques.
   * 
   * @param pNom Nom de la classe testée.
   * @param pListe1 Première liste de mouvements.
   * @param pListe2 Deuxième liste de mouvements.
   */
  private static void sameMoves(final String pNom, final Move [] pListe1, final Move [] pListe2)
  {
    assert pNom != null;
    assert pListe1 != null;
    assert pListe2 != null;

    if (pListe1.length != pListe2.length)
    {
      for (final Move m : pListe1)
      {
        System.out.println("1:" + m);
      }
      for (final Move m : pListe2)
      {
        System.out.println("2:" + m);
      }
    }
    assertTrue(pNom + " (" + pListe1.length + "!=" + pListe2.length + ')',
        pListe1.length == pListe2.length);

    for (final Move src : pListe1)
    {
      boolean trouve = false;
      for (final Move dst : pListe2)
      {
        if (dst.equals(src))
        {
          trouve = true;
          break;
        }
      }
      assertTrue(pNom, trouve);
    }
  }

  /**
   * Vérifie si deux listes de cases sont identiques (même ensemble de cases).
   * 
   * @param pNom Nom de la classe testée.
   * @param pListe1 Première liste de cases.
   * @param pListe2 Deuxième liste de cases.
   */
  private static void sameSquares(final String pNom, final Square [] pListe1,
      final Square [] pListe2)
  {
    assert pNom != null;
    assert pListe1 != null;
    assert pListe2 != null;

    if (pListe1.length != pListe2.length)
    {
      for (final Square s : pListe1)
      {
        System.out.println("1:" + s);
      }
      for (final Square s : pListe2)
      {
        System.out.println("2:" + s);
      }
    }
    assertTrue(pNom + " (" + pListe1.length + "!=" + pListe2.length + ')',
        pListe1.length == pListe2.length);

    for (final Square src : pListe1)
    {
      boolean trouve = false;
      for (final Square dst : pListe2)
      {
        if (dst == src)
        {
          trouve = true;
          break;
        }
      }
      if (!trouve)
      {
        for (final Square s : pListe1)
        {
          System.out.println("1:" + s);
        }
        for (final Square s : pListe2)
        {
          System.out.println("2:" + s);
        }
      }
      assertTrue(pNom, trouve);
    }
  }

  /**
   * Teste la méthode de dérivation de plateau.
   */
  @Test
  public void testDerive()
  {
    final Piece [] dispositionFinale =
        { WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, null, WHITE_KING, null, WHITE_KNIGHT, WHITE_ROOK,
          WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, null, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN,
          null, null, null, null, WHITE_PAWN, null, null, null, null, null, WHITE_BISHOP, null,
          null, null, null, null, null, null, null, null, null, null, null, null, null, BLACK_PAWN,
          null, null, null, null, null, BLACK_KNIGHT, BLACK_PAWN, BLACK_BISHOP, BLACK_PAWN,
          BLACK_PAWN, BLACK_PAWN, WHITE_QUEEN, BLACK_PAWN, BLACK_PAWN, BLACK_ROOK, BLACK_KNIGHT,
          null, BLACK_QUEEN, BLACK_KING, BLACK_BISHOP, null, BLACK_ROOK, };
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      final String nomClasse = etat.getClass().getSimpleName();
      etat = etat.derive(new Move(WHITE_PAWN, Square.valueOf("e2"), Square.valueOf("e3")), true);
      etat = etat.derive(new Move(BLACK_PAWN, Square.valueOf("b7"), Square.valueOf("b6")), true);
      etat = etat.derive(new Move(WHITE_BISHOP, Square.valueOf("f1"), Square.valueOf("c4")), true);
      etat = etat.derive(new Move(BLACK_KNIGHT, Square.valueOf("g8"), Square.valueOf("h6")), true);
      etat = etat.derive(new Move(WHITE_QUEEN, Square.valueOf("d1"), Square.valueOf("f3")), true);
      etat = etat.derive(new Move(BLACK_BISHOP, Square.valueOf("c8"), Square.valueOf("b7")), true);
      assertTrue(nomClasse, etat.isWhiteActive());
      assertTrue(nomClasse, etat.getHalfmoveCount() == 4);
      etat =
          etat.derive(
              new Move(WHITE_QUEEN, Square.valueOf("f3"), Square.valueOf("f7"), BLACK_PAWN), true);
      assertFalse(nomClasse, etat.isWhiteActive());
      assertTrue(nomClasse, etat.canCastleLong(true));
      assertTrue(nomClasse, etat.canCastleLong(false));
      assertTrue(nomClasse, etat.canCastleShort(true));
      assertTrue(nomClasse, etat.canCastleShort(false));
      assertTrue(nomClasse, etat.getFullmoveNumber() == 4);
      assertTrue(nomClasse, etat.getHalfmoveCount() == 0);
      for (final Square s : Square.values())
      {
        assertSame(nomClasse, etat.getPieceAt(s), dispositionFinale[s.getIndex()]);
      }
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles d'une position.
   */
  @Test
  public void testGetAllTargets()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, etatPrec.getAllTargets(s), etat.getAllTargets(s));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de fou.
   */
  @Test
  public void testGetBishopTargets()
  {
    MoveGenerator ePrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (ePrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, ePrec.getBishopTargets(s, true), etat.getBishopTargets(s, true));
          sameSquares(nomClasse, ePrec.getBishopTargets(s, false), etat.getBishopTargets(s, false));
        }
      }
      ePrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche de la case d'un roi.
   */
  @Test
  public void testGetKingSquare()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        assertSame(nomClasse, etatPrec.getKingSquare(true), etat.getKingSquare(true));
        assertSame(nomClasse, etatPrec.getKingSquare(false), etat.getKingSquare(false));
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de roi.
   */
  @Test
  public void testGetKingTargets()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, etatPrec.getKingTargets(s, true), etat.getKingTargets(s, true));
          sameSquares(nomClasse, etatPrec.getKingTargets(s, false), etat.getKingTargets(s, false));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de cavalier.
   */
  @Test
  public void testGetKnightTargets()
  {
    MoveGenerator ePrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (ePrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, ePrec.getKnightTargets(s, true), etat.getKnightTargets(s, true));
          sameSquares(nomClasse, ePrec.getKnightTargets(s, false), etat.getKnightTargets(s, false));
        }
      }
      ePrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de pion.
   */
  @Test
  public void testGetPawnTargets()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, etatPrec.getPawnTargets(s, true), etat.getPawnTargets(s, true));
          sameSquares(nomClasse, etatPrec.getPawnTargets(s, false), etat.getPawnTargets(s, false));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de dame.
   */
  @Test
  public void testGetQueenTargets()
  {
    MoveGenerator ePrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (ePrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, ePrec.getQueenTargets(s, true), etat.getQueenTargets(s, true));
          sameSquares(nomClasse, ePrec.getQueenTargets(s, false), etat.getQueenTargets(s, false));
        }
      }
      ePrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles de tour.
   */
  @Test
  public void testGetRookTargets()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, etatPrec.getRookTargets(s, true), etat.getRookTargets(s, true));
          sameSquares(nomClasse, etatPrec.getRookTargets(s, false), etat.getRookTargets(s, false));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases mouvements valides.
   */
  @Test
  public void testGetValidMoves()
  {
    final Move [] attendu =
        { new Move(WHITE_PAWN, Square.valueOf("a2"), Square.valueOf("a3")),
          new Move(WHITE_PAWN, Square.valueOf("a2"), Square.valueOf("a4")),
          new Move(WHITE_PAWN, Square.valueOf("b2"), Square.valueOf("b3")),
          new Move(WHITE_PAWN, Square.valueOf("b2"), Square.valueOf("b4")),
          new Move(WHITE_PAWN, Square.valueOf("c2"), Square.valueOf("c3")),
          new Move(WHITE_PAWN, Square.valueOf("c2"), Square.valueOf("c4")),
          new Move(WHITE_PAWN, Square.valueOf("d2"), Square.valueOf("d3")),
          new Move(WHITE_PAWN, Square.valueOf("d2"), Square.valueOf("d4")),
          new Move(WHITE_PAWN, Square.valueOf("e2"), Square.valueOf("e3")),
          new Move(WHITE_PAWN, Square.valueOf("e2"), Square.valueOf("e4")),
          new Move(WHITE_PAWN, Square.valueOf("f2"), Square.valueOf("f3")),
          new Move(WHITE_PAWN, Square.valueOf("f2"), Square.valueOf("f4")),
          new Move(WHITE_PAWN, Square.valueOf("g2"), Square.valueOf("g3")),
          new Move(WHITE_PAWN, Square.valueOf("g2"), Square.valueOf("g4")),
          new Move(WHITE_PAWN, Square.valueOf("h2"), Square.valueOf("h3")),
          new Move(WHITE_PAWN, Square.valueOf("h2"), Square.valueOf("h4")),
          new Move(WHITE_KNIGHT, Square.valueOf("b1"), Square.valueOf("a3")),
          new Move(WHITE_KNIGHT, Square.valueOf("b1"), Square.valueOf("c3")),
          new Move(WHITE_KNIGHT, Square.valueOf("g1"), Square.valueOf("f3")),
          new Move(WHITE_KNIGHT, Square.valueOf("g1"), Square.valueOf("h3")), };
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        sameMoves(nomClasse, etatPrec.getValidMoves(true), etat.getValidMoves(true));
        sameMoves(nomClasse, etatPrec.getValidMoves(false), etat.getValidMoves(false));
        sameMoves(nomClasse, etatPrec.getValidMoves(true), attendu);
        sameMoves(nomClasse, etat.getValidMoves(true), attendu);
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche des cases cibles valides à partir d'une position.
   */
  @Test
  public void testGetValidTargets()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          sameSquares(nomClasse, etatPrec.getValidTargets(s), etat.getValidTargets(s));
        }
        assertTrue(nomClasse, etatPrec.getValidTargets(Square.valueOf("a1")).length == 0);
        assertTrue(nomClasse, etatPrec.getValidTargets(Square.valueOf("b1")).length == 2);
        assertTrue(nomClasse, etatPrec.getValidTargets(Square.valueOf("b2")).length == 2);
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de détection d'une attaque sur une case.
   */
  @Test
  public void testIsAttacked()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        for (final Square s : Square.values())
        {
          assertTrue(nomClasse, etatPrec.isAttacked(s, true) == etat.isAttacked(s, true));
          assertTrue(nomClasse, etatPrec.isAttacked(s, false) == etat.isAttacked(s, false));
        }
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des méthodes de recherche indiqunt un roque effectué.
   */
  @Test
  public void testIsCastled()
  {
    MoveGenerator etatPrec = null;
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      final MoveGenerator etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      if (etatPrec != null)
      {
        final String nomClasse = etat.getClass().getSimpleName();
        assertTrue(nomClasse, etatPrec.isCastled(true) == etat.isCastled(true));
        assertTrue(nomClasse, etatPrec.isCastled(false) == etat.isCastled(false));
      }
      etatPrec = etat;
    }
  }

  /**
   * Teste l'équivalence des résultats lors du déroulement des parties.
   */
  @Test
  public void testPlaying()
  {
    final Random randomizer = new Random(1000);
    final int eLength = BoardFactory.Type.values().length;
    final MoveGenerator [] etats = new MoveGenerator [ eLength ];
    final Move [][] mvts = new Move [ eLength ] [];
    for (int p = 100; p >= 0; p--)
    {
      for (final BoardFactory.Type t : BoardFactory.Type.values())
      {
        etats[t.ordinal()] = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      }
      for (int cps = 100; cps >= 0; cps--)
      {
        for (int i = 0; i < eLength; i++)
        {
          mvts[i] = etats[i].getValidMoves(etats[i].isWhiteActive());
          if (i > 0)
          {
            sameMoves(etats[i].getClass().getSimpleName(), mvts[i - 1], mvts[i]);
          }
        }
        final int mLength = mvts[0].length;
        if (mLength == 0)
        {
          break;
        }
        final Move mvt = mvts[0][randomizer.nextInt(mLength)];
        for (int i = 0; i < eLength; i++)
        {
          etats[i] = etats[i].derive(mvt, true);
          if (etats[i].isCastled(true))
          {
            assertFalse(etats[i].getClass().getSimpleName(), etats[i].canCastleLong(true));
            assertFalse(etats[i].getClass().getSimpleName(), etats[i].canCastleShort(true));
          }
          if (etats[i].isCastled(false))
          {
            assertFalse(etats[i].getClass().getSimpleName(), etats[i].canCastleLong(false));
            assertFalse(etats[i].getClass().getSimpleName(), etats[i].canCastleShort(false));
          }
          if (i > 0)
          {
            assertEquals(etats[i].getClass().getSimpleName(), etats[i - 1], etats[i]);
            assertTrue(etats[i].getClass().getSimpleName(),
                etats[i - 1].isCastled(true) == etats[i].isCastled(true));
            assertTrue(etats[i].getClass().getSimpleName(),
                etats[i - 1].isCastled(false) == etats[i].isCastled(false));
          }
        }
      }
    }
  }

  /**
   * Teste la résistance des représentations face au multithread.
   */
  @Test
  public void testThreadSafety()
  {
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      if (t == FASTEST)
      {
        continue;
      }
      final PlayingThread [] process = new PlayingThread [ 20 ];
      for (int i = 0; i < 50; i++)
      {
        for (int j = 0; j < process.length; j++)
        {
          process[j] = new PlayingThread(t);
          process[j].start();
        }
        for (final PlayingThread p : process)
        {
          try
          {
            p.join();
            if (!p.isOk())
            {
              fail(p.toString() + " failed");
            }
          }
          catch (final InterruptedException e)
          {
            fail(e.toString());
          }
        }
      }
    }
  }

  /**
   * Thread exécutant une partie.
   */
  private static final class PlayingThread extends Thread
  {
    /** Type de la représentation à utiliser. */
    private final BoardFactory.Type _type;

    /** Indicateur de bon fonctionnement. */
    private boolean _ok;

    /**
     * Instancie un nouveau processus de jeu.
     * 
     * @param pType Type de représentation de l'état.
     */
    PlayingThread(final BoardFactory.Type pType)
    {
      assert pType != null;

      _type = pType;
      _ok = false;
    }

    /**
     * Renvoi l'état de l'indicateur de bon fonctionnement.
     * 
     * @return Vrai si le thread s'est bien déroulé.
     */
    boolean isOk()
    {
      return _ok;
    }

    /**
     * Joue une partie.
     */
    @Override
    public void run()
    {
      final Random randomizer = new Random(1000);
      MoveGenerator etat = BoardFactory.valueOf(_type, BoardFactory.State.STARTING);
      for (int cps = 50; cps >= 0; cps--)
      {
        final Move [] mvts = etat.getValidMoves(etat.isWhiteActive());
        final int nbMvts = mvts.length;
        if (nbMvts == 0)
        {
          break;
        }
        etat = etat.derive(mvts[randomizer.nextInt(nbMvts)], true);
      }

      _ok = true;
    }
  }
}
