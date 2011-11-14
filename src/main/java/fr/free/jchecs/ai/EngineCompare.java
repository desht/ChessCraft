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
 * Classe utilitaire permettant de comparer les résultats des moteurs d'IA.
 * 
 * @author David Cotton
 */
public final class EngineCompare
{
  /**
   * Classe utilitaire : ne pas instancier.
   */
  private EngineCompare()
  {
    // Rien de spécifique...
  }

  /**
   * Compare les moteurs par rapport au moteur de débugage.
   * 
   * @param pArgs Arguments de la ligne de commande : ignorés, aucun argument attendu.
   */
  public static void main(final String [] pArgs)
  {
    final Engine debugEngine = new DebugEngine();
    final Engine minimaxEngine = new MiniMaxEngine();
    final Engine enhancedMinimaxEngine = new EnhancedMiniMaxEngine();
    final Engine alphabetaEngine = new AlphaBetaEngine();
    final Engine negascoutEngine = new NegaScoutEngine();
    final int searchLimit = 3;
    debugEngine.setSearchDepthLimit(searchLimit);
    minimaxEngine.setSearchDepthLimit(searchLimit);
    enhancedMinimaxEngine.setSearchDepthLimit(searchLimit);
    alphabetaEngine.setSearchDepthLimit(searchLimit);
    negascoutEngine.setSearchDepthLimit(searchLimit);
    final boolean openings = false;
    debugEngine.setOpeningsEnabled(openings);
    minimaxEngine.setOpeningsEnabled(openings);
    enhancedMinimaxEngine.setOpeningsEnabled(openings);
    alphabetaEngine.setOpeningsEnabled(openings);
    negascoutEngine.setOpeningsEnabled(openings);
    final Heuristic heuristic = new BoardControlHeuristic();
    debugEngine.setHeuristic(heuristic);
    minimaxEngine.setHeuristic(heuristic);
    enhancedMinimaxEngine.setHeuristic(heuristic);
    alphabetaEngine.setHeuristic(heuristic);
    negascoutEngine.setHeuristic(heuristic);

    final int nbManches = 2;
    final int nbCoups = 30;
    for (int i = 0; i < nbManches; i++)
    {
      MoveGenerator etat =
          BoardFactory.valueOf(BoardFactory.Type.MAILBOX, BoardFactory.State.STARTING);
      while (true)
      {
        if (etat.getFullmoveNumber() >= nbCoups)
        {
          break;
        }
        final boolean trait = etat.isWhiteActive();
        if (etat.getValidMoves(trait).length == 0)
        {
          break;
        }
        Engine eng = debugEngine;
        eng.getMoveFor(etat);
        final int noteDbg = eng.getScore();
        final long nodesDbg = eng.getHalfmoveCount();
        System.out.printf(" Dbg=%+05d (%05d - %06d)", Integer.valueOf(noteDbg), Long
            .valueOf(nodesDbg), Long.valueOf(eng.getElapsedTime() / 1000));
        eng = minimaxEngine;
        final Move mvt = eng.getMoveFor(etat);
        final int noteMM = eng.getScore();
        final long nodesMM = eng.getHalfmoveCount();
        System.out.printf(" mM=%+05d (%05d - %06d)", Integer.valueOf(noteMM),
            Long.valueOf(nodesMM), Long.valueOf(eng.getElapsedTime() / 1000));
        assert noteDbg == noteMM;
        // assert nodesDbg >= nodesMM;
        eng = enhancedMinimaxEngine;
        eng.getMoveFor(etat);
        final int noteEMM = eng.getScore();
        final long nodesEMM = eng.getHalfmoveCount();
        System.out.printf(" mM+=%+05d (%05d - %06d)", Integer.valueOf(noteEMM), Long
            .valueOf(nodesEMM), Long.valueOf(eng.getElapsedTime() / 1000));
        assert noteDbg == noteEMM;
        // assert nodesDbg >= nodesMM;
        eng = alphabetaEngine;
        eng.getMoveFor(etat);
        final int noteAB = eng.getScore();
        final long nodesAB = eng.getHalfmoveCount();
        System.out.printf(" AB=%+05d (%05d - %06d)", Integer.valueOf(noteAB),
            Long.valueOf(nodesAB), Long.valueOf(eng.getElapsedTime() / 1000));
        assert noteMM == noteAB;
        assert nodesMM >= nodesAB;
        eng = negascoutEngine;
        eng.getMoveFor(etat);
        final int noteNg = eng.getScore();
        final long nodesNg = eng.getHalfmoveCount();
        System.out.printf(" Ng=%+05d (%05d - %06d)", Integer.valueOf(noteNg),
            Long.valueOf(nodesNg), Long.valueOf(eng.getElapsedTime() / 1000));
        assert noteAB == noteNg;
        // assert nodesAB >= nodesNg;
        System.out.println();
        etat = etat.derive(mvt, true);
      }
    }
  }
}
