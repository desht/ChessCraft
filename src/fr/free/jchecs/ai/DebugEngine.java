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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.free.jchecs.core.Board;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Moteur IA de debugage : recherche en dur le meilleur de l'ensemble des coups possibles avec une
 * profondeur de 3 demi-coups.
 * 
 * @author David Cotton
 */
final class DebugEngine extends AbstractEngine
{
  /**
   * Instancie un nouveau moteur IA de debugage.
   */
  DebugEngine()
  {
    super(3, 3, 3);
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
    final Map<Board, BoardScore> echiquiersRang1 = new HashMap<Board, BoardScore>(l);
    final Map<Board, BoardScore> echiquiersRang2 = new HashMap<Board, BoardScore>(l * 25);
    final List<BoardScore> echiquiersRang3 = new ArrayList<BoardScore>(l * 25 * 25);
    Move res = pCoups[0];
    addHalfmove(l);
    for (final Move mvt1 : pCoups)
    {
      final MoveGenerator etat1 = pEtat.derive(mvt1, true);
      echiquiersRang1.put(etat1, new BoardScore(mvt1, null, Integer.MAX_VALUE));
      final Move [] coups2 = etat1.getValidMoves(etat1.isWhiteActive());
      addHalfmove(coups2.length);
      for (final Move mvt2 : coups2)
      {
        final MoveGenerator etat2 = etat1.derive(mvt2, true);
        echiquiersRang2.put(etat2, new BoardScore(mvt2, etat1, Integer.MIN_VALUE));
        final Move [] coups3 = etat2.getValidMoves(etat2.isWhiteActive());
        addHalfmove(coups3.length);
        for (final Move mvt3 : coups3)
        {
          final MoveGenerator etat3 = etat2.derive(mvt3, true);
          final BoardScore bs =
              new BoardScore(mvt3, etat2, getHeuristic().evaluate(etat3, !etat3.isWhiteActive()));
          echiquiersRang3.add(bs);
        }
      }
    }

    for (final BoardScore bs : echiquiersRang3)
    {
      final BoardScore parent = echiquiersRang2.get(bs.getParent());
      if (parent.getNote() < bs.getNote())
      {
        parent.setNote(bs.getNote());
      }
    }

    for (final BoardScore bs : echiquiersRang2.values())
    {
      final BoardScore parent = echiquiersRang1.get(bs.getParent());
      if (parent.getNote() > bs.getNote())
      {
        parent.setNote(bs.getNote());
      }
    }

    int meilleur = Integer.MIN_VALUE;
    for (final BoardScore bs : echiquiersRang1.values())
    {
      if (meilleur < bs.getNote())
      {
        meilleur = bs.getNote();
        res = bs.getMove();
      }
    }

    setScore(meilleur);

    assert l == echiquiersRang1.values().size();
    assert res != null;
    return res;
  }

  /**
   * Score d'un état.
   */
  private static final class BoardScore
  {
    /** Mouvement précédant cet échiquier. */
    private final Move _move;

    /** Eventuel état père. */
    private final Board _parent;

    /** Note de l'échiquier. */
    private int _note;

    /**
     * Instancie un nouveau score d'échiquier.
     * 
     * @param pMouvement Mouvement précédant.
     * @param pParent Eventuel état père (peut être à null).
     * @param pNote Note initiale.
     */
    BoardScore(final Move pMouvement, final Board pParent, final int pNote)
    {
      assert pMouvement != null;

      _move = pMouvement;
      _parent = pParent;
      _note = pNote;
    }

    /**
     * Renvoi le mouvement lié.
     * 
     * @return Mouvement lié.
     */
    Move getMove()
    {
      assert _move != null;
      return _move;
    }

    /**
     * Renvoi la note de l'échiquier.
     * 
     * @return Note de l'échiquier.
     */
    int getNote()
    {
      return _note;
    }

    /**
     * Renvoi l'éventuel état père.
     * 
     * @return Etat père (peut être à null).
     */
    Board getParent()
    {
      return _parent;
    }

    /**
     * Alimente la note de l'échiquier.
     * 
     * @param pNote Note de l'échiquier.
     */
    void setNote(final int pNote)
    {
      _note = pNote;
    }
  }
}
