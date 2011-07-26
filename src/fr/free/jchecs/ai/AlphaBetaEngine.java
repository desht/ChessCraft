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

import java.util.Arrays;
import java.util.Comparator;

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Moteur d'IA basé sur un alpha/beta (façon NegaMax) sur 5 demi-coups.
 * 
 * @author David Cotton
 */
final class AlphaBetaEngine extends AbstractEngine
{
  /**
   * Instancie un nouveau moteur IA alpha/beta.
   */
  AlphaBetaEngine()
  {
    super(3, 6, 5);

    setMoveSorter(new KillerMoveSorter());
  }

  /**
   * Recherche le meilleur coup évalué à partir d'une position.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pProfondeur Profondeur d'évaluation actuelle.
   * @param pAlpha Seuil alpha.
   * @param pBeta Seuil beta.
   * @return Meilleure évaluation obtenue à ce niveau.
   */
  private int alphabeta(final MoveGenerator pEtat, final int pProfondeur, final int pAlpha,
      final int pBeta)
  {
    assert pEtat != null;
    assert pProfondeur >= 0;
    assert pAlpha <= pBeta;

    final boolean trait = pEtat.isWhiteActive();

    if (pProfondeur == 0)
    {
      return getHeuristic().evaluate(pEtat, trait);
    }

    final Move [] coups = pEtat.getValidMoves(trait);
    final int l = coups.length;
    if (l == 0)
    {
      return getHeuristic().evaluate(pEtat, trait);
    }

    int res = MATE_VALUE - 1;

    final Comparator<Move> tri = getMoveSorter();
    final KillerMoveSorter killer;
    if (tri instanceof KillerMoveSorter)
    {
      killer = (KillerMoveSorter) tri;
    }
    else
    {
      killer = null;
    }
    Arrays.sort(coups, tri);
    addHalfmove(l);
    int alpha = pAlpha;
    for (final Move mvt : coups)
    {
      final int note = -alphabeta(pEtat.derive(mvt, true), pProfondeur - 1, -pBeta, -alpha);
      if (note > res)
      {
        res = note;
        if (res > alpha)
        {
          alpha = res;
          if (alpha > pBeta)
          {
            if (killer != null)
            {
              killer.put(mvt);
            }
            return res;
          }
        }
      }
    }

    return res;
  }

  /**
   * Corps de la recherche du "meilleur" demi-coup pour un état de l'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pCoups Liste des mouvement initiaux valides.
   * @return Mouvement trouvé.
   */
  @Override
  protected Move searchMoveFor(final MoveGenerator pEtat, final Move [] pCoups)
  {
    assert pEtat != null;
    assert pCoups != null;

    final int l = pCoups.length;
    assert l > 0;
    addHalfmove(l);
    final Comparator<Move> tri = getMoveSorter();
    Arrays.sort(pCoups, tri);
    Move res = pCoups[0];
    int alpha = MATE_VALUE - 1;
    for (final Move mvt : pCoups)
    {
      final MoveGenerator etat = pEtat.derive(mvt, true);
      final int note = -alphabeta(etat, getSearchDepthLimit() - 1, MATE_VALUE, -alpha);
      if ((note > alpha) || ((note == alpha) && RANDOMIZER.nextBoolean()))
      {
        // Un peu de hasard sert à partager les évaluations identiques : jeu plus agréable.
        alpha = note;
        res = mvt;
      }
    }

    setScore(alpha);

    if (tri instanceof KillerMoveSorter)
    {
      ((KillerMoveSorter) tri).clear();
    }

    assert res != null;
    return res;
  }
}
