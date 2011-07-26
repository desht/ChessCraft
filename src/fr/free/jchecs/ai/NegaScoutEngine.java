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
 * Moteur d'IA basé sur l'algorithme NegaScout (aussi appelé Principal Variation Search), avec table
 * de transposition, sur 5 demi-coups.
 * 
 * @author David Cotton
 */
final class NegaScoutEngine extends AbstractEngine
{
  /** Table de transposition. */
  private static final TranspositionTable TRANSPOSITIONS = new TranspositionTable(1000000);

  /**
   * Instancie un nouveau moteur IA Negascout.
   */
  NegaScoutEngine()
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
   * @param pLimite Profondeur limite.
   * @return Meilleure évaluation obtenue à ce niveau.
   */
  private int negascout(final MoveGenerator pEtat, final int pProfondeur, final int pAlpha,
      final int pBeta, final int pLimite)
  {
    assert pEtat != null;
    assert pAlpha <= pBeta;

    final Integer resultat = TRANSPOSITIONS.get(pEtat, pProfondeur, pAlpha, pBeta);
    if (resultat != null)
    {
      return resultat.intValue();
    }

    final boolean trait = pEtat.isWhiteActive();

    if (pProfondeur <= pLimite)
    {
      final int valeur = getHeuristic().evaluate(pEtat, trait);
      TRANSPOSITIONS.put(pEtat, pProfondeur, pAlpha, pBeta, valeur);

      return valeur;
    }

    final Move [] coups = pEtat.getValidMoves(trait);
    final int l = coups.length;
    if (l == 0)
    {
      final int valeur = getHeuristic().evaluate(pEtat, trait);
      TRANSPOSITIONS.put(pEtat, pProfondeur, pAlpha, pBeta, valeur);

      return valeur;
    }

    int res = MATE_VALUE - 1;

    final Comparator<Move> tri = getMoveSorter();
    final ContextSorter ctx;
    if (tri instanceof ContextSorter)
    {
      ctx = (ContextSorter) tri;
    }
    else
    {
      ctx = null;
    }
    Arrays.sort(coups, tri);
    addHalfmove(l);
    int alpha = pAlpha;
    for (int i = 0; i < l; i++)
    {
      final Move mvt = coups[i];
      final MoveGenerator etat = pEtat.derive(mvt, true);
      final int limite;
      if (((pProfondeur == 1) && ((l <= 3) || (mvt.getCaptured() != null)))
          || etat.isInCheck(etat.isWhiteActive()))
      {
        limite = -1;
      }
      else
      {
        limite = 0;
      }
      int note;
      if (i == 0)
      {
        note = -negascout(etat, pProfondeur - 1, -pBeta, -alpha, limite);
      }
      else
      {
        note = -negascout(etat, pProfondeur - 1, -alpha - 1, -alpha, limite);
      }
      if (note > res)
      {
        if ((i > 0) && (alpha < note) && (note < pBeta) && (pProfondeur > limite + 2))
        {
          note = -negascout(etat, pProfondeur - 1, -pBeta, -note, limite);
        }
        res = note;
        if (res > alpha)
        {
          alpha = res;
          if (alpha > pBeta)
          {
            if (ctx != null)
            {
              ctx.put(mvt);
            }
            break;
          }
        }
      }
    }

    TRANSPOSITIONS.put(pEtat, pProfondeur, alpha, pBeta, res);
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
      final int note = -negascout(etat, getSearchDepthLimit() - 1, MATE_VALUE, -alpha, 0);
      if ((note > alpha) || ((note == alpha) && RANDOMIZER.nextBoolean()))
      {
        // Un peu de hasard sert à partager les évaluations identiques : jeu plus agréable.
        alpha = note;
        res = mvt;
      }
    }

    setScore(alpha);

    TRANSPOSITIONS.clear();
    if (tri instanceof ContextSorter)
    {
      ((ContextSorter) tri).clear();
    }

    assert res != null;
    return res;
  }
}
