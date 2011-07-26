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
import static org.junit.Assert.fail;

import static fr.free.jchecs.core.BoardFactory.State.EMPTY;
import static fr.free.jchecs.core.BoardFactory.State.STARTING;
import static fr.free.jchecs.core.BoardFactory.Type.ARRAY;
import static fr.free.jchecs.core.FENUtils.STANDART_STARTING_FEN;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.FENUtils.toFEN;
import static fr.free.jchecs.core.FENUtils.toFENKey;

import org.junit.Test;

/**
 * Tests unitaires de la classe utilitaire pour la notation FEN.
 * 
 * @author David Cotton
 */
public final class FENUtilsTest
{
  /** Chaine FEN d'un plateau vide. */
  private static final String EMPTY_FEN = "8/8/8/8/8/8/8/8 w KQkq - 0 1";

  /**
   * Pour que JUnit puisse instancier les tests.
   */
  public FENUtilsTest()
  {
    // Rien de spécifique...
  }

  /**
   * Teste la convertion chaine FEN / état d'échiquier.
   */
  @Test
  public void testToBoard()
  {
    try
    {
      toBoard(null);
    }
    catch (final FENException e)
    {
      fail(e.toString());
    }
    catch (final NullPointerException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/8/PPPPPPPP w - - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/4P4/8/8/8/PPPPPPPP w - - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/4Z3/8/8/8/PPPPPPPP w - - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP ? - - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkqKQ - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w DRdr - 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq xe9 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq e9 0 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq - -1 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq - ? 1");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq - 0 0");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      toBoard("pppppppp/8/8/8/8/8/8/PPPPPPPP w KQkq - 0 ?");
    }
    catch (final FENException e)
    {
      // C'est ce qui est attendu...
    }

    try
    {
      assertEquals(toBoard(STANDART_STARTING_FEN), BoardFactory.valueOf(ARRAY, STARTING));
      assertEquals(toBoard(EMPTY_FEN), BoardFactory.valueOf(ARRAY, EMPTY));
    }
    catch (final FENException e)
    {
      fail(e.toString());
    }
  }

  /**
   * Teste la convertion état d'échiquier / chaine FEN.
   */
  @Test
  public void testToFEN()
  {
    assertEquals(toFEN(BoardFactory.valueOf(ARRAY, EMPTY)), EMPTY_FEN);
    assertEquals(toFEN(BoardFactory.valueOf(ARRAY, STARTING)), STANDART_STARTING_FEN);
  }

  /**
   * Teste la méthode "toFENKey(null)".
   */
  @Test(expected = NullPointerException.class)
  public void testToFENKeyNull()
  {
    toFENKey(null);
  }

  /**
   * Teste la méthode "toFEN(null)".
   */
  @Test(expected = NullPointerException.class)
  public void testToFENNull()
  {
    toFEN(null);
  }
}
