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
package fr.free.jchecs.ai;

import fr.free.jchecs.core.BoardFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Classe utilitaire permettant de tester l'efficacité de la gestion des ouvertures.
 * 
 * @author David Cotton
 */
public final class OpeningsBench
{
  /** Nombre de parties à exécuter. */
  private static final int GAMES_COUNT = 20;

  /** Nombre de coups par partie. */
  private static final int MOVES_COUNT = 50;

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private OpeningsBench()
  {
    // Rien de spécifique...
  }

  /**
   * Teste l'efficacité de la gestion des ouvertures.
   * 
   * @param pArgs Arguments de la ligne de commande : ignorés, aucun argument attendu.
   */
  public static void main(final String [] pArgs)
  {
    System.out.println("Parties croisées (en " + GAMES_COUNT + " manches de " + MOVES_COUNT
        + " coups maximum).");
    final Engine moteur = EngineFactory.newInstance();
    int avecOuvertures = 0;
    int sansOuvertures = 0;
    boolean ouvertures = true;
    for (int i = 0; i < GAMES_COUNT; i++)
    {
      MoveGenerator etat =
          BoardFactory.valueOf(BoardFactory.Type.FASTEST, BoardFactory.State.STARTING);
      while (true)
      {
        System.out.print('.');
        if (etat.getFullmoveNumber() >= MOVES_COUNT)
        {
          avecOuvertures++;
          sansOuvertures++;
          break;
        }
        final boolean trait = etat.isWhiteActive();
        if (etat.getValidMoves(trait).length == 0)
        {
          if (etat.isInCheck(trait))
          {
            if (trait == ouvertures)
            {
              sansOuvertures += 2;
              System.out.print(" sans");
            }
            else
            {
              avecOuvertures += 2;
              System.out.print(" avec");
            }
          }
          else
          {
            avecOuvertures++;
            sansOuvertures++;
          }
          break;
        }
        moteur.setOpeningsEnabled(trait == ouvertures);
        final Move mvt = moteur.getMoveFor(etat);
        etat = etat.derive(mvt, true);
      }
      System.out.println();
      ouvertures = !ouvertures;
    }
    System.out.println(" => " + (avecOuvertures / 2.0F) + " (Avec ouvertures) / "
        + (sansOuvertures / 2.0F) + " (Sans ouvertures)");
  }
}
