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

import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Moteur d'IA basé sur un mini/max ammélioré par un élagae simple, sur 4 demi-coups.
 * <p>
 * Cette version du mini/max est optimisée par élagage partiel équivalent à un demi alpha-beta.
 * </p>
 * 
 * @author David Cotton
 */
final class EnhancedMiniMaxEngine extends AbstractEngine
{
  /** Drapeau indiquant si le trait est aux blancs. */
  private boolean _trait;

  /**
   * Instancie un nouveau moteur IA mini/max ammélioré.
   */
  EnhancedMiniMaxEngine()
  {
    super(2, 5, 4);
  }

  /**
   * Recherche la meilleure réplique évaluée à partir d'une position.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pProfondeur Profondeur d'évaluation actuelle.
   * @param pMax Valeur du max du noeud père (petite optimisation, en attendant un vrai Alpha/Béta).
   * @return Meilleure évaluation obtenue à ce niveau.
   */
  private int findMin(final MoveGenerator pEtat, final int pProfondeur, final int pMax)
  {
    assert pEtat != null;
    assert pProfondeur >= 0;

    assert pEtat.isWhiteActive() != _trait;

    if (pProfondeur == 0)
    {
      return getHeuristic().evaluate(pEtat, _trait);
    }

    int res = -MATE_VALUE;

    final Move [] coups = pEtat.getValidMoves(pEtat.isWhiteActive());
    Arrays.sort(coups, getMoveSorter());
    addHalfmove(coups.length);
    for (final Move mvt : coups)
    {
      final MoveGenerator etat = pEtat.derive(mvt, true);
      final int note = findMax(etat, pProfondeur - 1, res);
      if (note < pMax)
      {
        // Début d'élagage, ajouté par rapport à un mini/max pur...
        return note;
      }
      if (note < res)
      {
        res = note;
      }
    }

    return res;
  }

  /**
   * Recherche le meilleur coup évalué à partir d'une position.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pProfondeur Profondeur d'évaluation actuelle.
   * @param pMin Valeur du min du noeud père (petite optimisation, en attendant un vrai Alpha/Béta).
   * @return Meilleure évaluation obtenue à ce niveau.
   */
  private int findMax(final MoveGenerator pEtat, final int pProfondeur, final int pMin)
  {
    assert pEtat != null;
    assert pProfondeur >= 0;

    assert pEtat.isWhiteActive() == _trait;

    if (pProfondeur == 0)
    {
      return getHeuristic().evaluate(pEtat, _trait);
    }

    int res = MATE_VALUE;

    final Move [] coups = pEtat.getValidMoves(pEtat.isWhiteActive());
    Arrays.sort(coups, getMoveSorter());
    addHalfmove(coups.length);
    for (final Move mvt : coups)
    {
      final MoveGenerator etat = pEtat.derive(mvt, true);
      final int note = findMin(etat, pProfondeur - 1, res);
      if (note > pMin)
      {
        // Début d'élagage, ajouté par rapport à un mini/max pur...
        return note;
      }
      if (note > res)
      {
        res = note;
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

    _trait = pEtat.isWhiteActive();

    final int l = pCoups.length;
    assert l > 0;
    addHalfmove(l);
    Arrays.sort(pCoups, getMoveSorter());
    Move res = pCoups[0];
    int meilleur = MATE_VALUE - 1;
    for (final Move mvt : pCoups)
    {
      final MoveGenerator etat = pEtat.derive(mvt, true);
      final int note = findMin(etat, getSearchDepthLimit() - 1, meilleur);
      if ((note > meilleur) || ((note == meilleur) && RANDOMIZER.nextBoolean()))
      {
        // Un peu de hasard sert à partager les évaluations identiques : jeu plus agréable.
        meilleur = note;
        res = mvt;
      }
    }

    setScore(meilleur);

    assert res != null;
    return res;
  }
}
