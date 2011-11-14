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

import java.util.HashMap;
import java.util.Map;

import fr.free.jchecs.core.BoardFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Classe utilitaire permettant de tester les performances des moteurs IA.
 * 
 * @author David Cotton
 */
public final class EngineBench
{
  /**
   * Classe utilitaire : ne pas intancier.
   */
  private EngineBench()
  {
    // Rien de spécifique...
  }

  /**
   * Teste l'efficacité des moteurs de recherche du meilleur mouvement.
   * 
   * @param pArgs Arguments de la ligne de commande : ignorés, aucun argument attendu.
   */
  public static void main(final String [] pArgs)
  {
    final int nbManches = 10;
    final int nbCoups = 50;
    System.out.println("Parties croisées (en " + nbManches + " manches de " + nbCoups
        + " coups maximum) entre les moteurs :");
    final String [] nomsMoteurs = EngineFactory.getNames();
    final Engine [] listeMoteurs = new Engine [ nomsMoteurs.length ];
    final int nbMoteurs = listeMoteurs.length;
    final Map<String, Long> nbDemiCoups = new HashMap<String, Long>();
    final Map<String, Long> durees = new HashMap<String, Long>();
    for (int i = 0; i < nomsMoteurs.length; i++)
    {
      listeMoteurs[i] = EngineFactory.newInstance(nomsMoteurs[i]);
      nbDemiCoups.put(nomsMoteurs[i], Long.valueOf(0));
      durees.put(nomsMoteurs[i], Long.valueOf(0));
    }
    final Map<Boolean, Engine> joueurs = new HashMap<Boolean, Engine>();
    for (int b = 0; b < nbMoteurs; b++)
    {
      joueurs.put(Boolean.TRUE, listeMoteurs[b]);
      for (int n = 0; n < nbMoteurs; n++)
      {
        joueurs.put(Boolean.FALSE, listeMoteurs[n]);
        System.out.print(" - " + listeMoteurs[b] + " / " + listeMoteurs[n]);
        int scoreBlancs = 0;
        int scoreNoirs = 0;
        for (int i = 0; i < nbManches; i++)
        {
          System.out.print('.');
          MoveGenerator etat =
              BoardFactory.valueOf(BoardFactory.Type.FASTEST, BoardFactory.State.STARTING);
          while (true)
          {
            if (etat.getFullmoveNumber() >= nbCoups)
            {
              scoreBlancs++;
              scoreNoirs++;
              break;
            }
            final boolean trait = etat.isWhiteActive();
            if (etat.getValidMoves(trait).length == 0)
            {
              if (etat.isInCheck(trait))
              {
                if (trait)
                {
                  scoreNoirs += 2;
                }
                else
                {
                  scoreBlancs += 2;
                }
              }
              else
              {
                scoreBlancs++;
                scoreNoirs++;
              }
              break;
            }
            final Engine ia = joueurs.get(Boolean.valueOf(trait));
            final Move mvt = ia.getMoveFor(etat);
            etat = etat.derive(mvt, true);
          }
          nbDemiCoups.put(nomsMoteurs[n], Long.valueOf(nbDemiCoups.get(nomsMoteurs[n]).longValue()
              + listeMoteurs[n].getHalfmoveCount()));
          durees.put(nomsMoteurs[n], Long.valueOf(durees.get(nomsMoteurs[n]).longValue()
              + listeMoteurs[n].getElapsedTime()));
          nbDemiCoups.put(nomsMoteurs[b], Long.valueOf(nbDemiCoups.get(nomsMoteurs[b]).longValue()
              + listeMoteurs[b].getHalfmoveCount()));
          durees.put(nomsMoteurs[b], Long.valueOf(durees.get(nomsMoteurs[b]).longValue()
              + listeMoteurs[b].getElapsedTime()));
        }
        System.out.println();
        System.out.println("     => " + (scoreBlancs / 2.0F) + " / " + (scoreNoirs / 2.0F));
      }
    }
    System.out.println("Performances atteintes :");
    for (final Engine eng : listeMoteurs)
    {
      final String nomMoteur = eng.toString();
      final long demisCoups = nbDemiCoups.get(nomMoteur).longValue();
      final long duree = durees.get(nomMoteur).longValue();
      System.out.println(" - " + nomMoteur + " : " + demisCoups + " demi-coups évalués en " + duree
          + "ms, soit " + (int) (1000.0 / duree * demisCoups) + " demi-coups/s");
    }
  }
}
