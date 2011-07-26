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

import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;
import static fr.free.jchecs.core.Constants.FILE_COUNT;
import static fr.free.jchecs.core.Constants.RANK_COUNT;

/**
 * Classe utilitaire permettant de tester les performances des classes représentant des états de la
 * partie.
 * 
 * @author David Cotton
 */
public final class BoardBench
{
  /**
   * Classe utilitaire : ne pas instancier.
   */
  private BoardBench()
  {
    // Rien de spécifique...
  }

  /**
   * Teste la vitesse de lecture du contenu d'une case.
   * 
   * @param pArgs Arguments de la ligne de commande : ignorés, aucun argument attendu.
   */
  public static void main(final String [] pArgs)
  {
    final int nbTests = 400000;

    System.out.println("Benchmark (" + 64 * nbTests * 5 + ") : getPieceAt(Square)");
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      if (t == FASTEST)
      {
        continue;
      }
      final Board etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      final Square [] lst = Square.values();
      final long debut = System.currentTimeMillis();
      for (int f = lst.length - 1; f >= 0; f--)
      {
        final Square s = lst[f];
        for (int i = nbTests; i > 0; i--)
        {
          etat.getPieceAt(s);
          etat.getPieceAt(s);
          etat.getPieceAt(s);
          etat.getPieceAt(s);
          etat.getPieceAt(s);
        }
      }
      final long fin = System.currentTimeMillis();
      System.out.println("  " + etat.getClass().getSimpleName() + " = " + (fin - debut) + "ms");
    }
    System.out.println("Benchmark (" + 64 * nbTests * 5 + ") : getPieceAt(int,int)");
    for (final BoardFactory.Type t : BoardFactory.Type.values())
    {
      if (t == FASTEST)
      {
        continue;
      }
      final Board etat = BoardFactory.valueOf(t, BoardFactory.State.STARTING);
      final long debut = System.currentTimeMillis();
      for (int y = RANK_COUNT - 1; y >= 0; y--)
      {
        for (int x = FILE_COUNT - 1; x >= 0; x--)
        {
          for (int i = nbTests; i > 0; i--)
          {
            etat.getPieceAt(x, y);
            etat.getPieceAt(x, y);
            etat.getPieceAt(x, y);
            etat.getPieceAt(x, y);
            etat.getPieceAt(x, y);
          }
        }
      }
      final long fin = System.currentTimeMillis();
      System.out.println("  " + etat.getClass().getSimpleName() + " = " + (fin - debut) + "ms");
    }
  }
}
