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

import fr.free.jchecs.core.Board;

/**
 * Classe implémentant la gestion d'une table de transposition intégrable dans un moteur d'I.A.
 * 
 * @author David Cotton
 */
final class TranspositionTable
{
  /** Nombre maximum de collisions entre les clés avant l'abandon. */
  private static final int MAX_COLLISIONS = 3;

  /** Identifiant d'une valeur de type "ALPHA". */
  private static final int ALPHA = 0;

  /** Identifiant d'une valeur de type "BETA". */
  private static final int BETA = 1;

  /** Identifiant d'une valeur de type "EXACT". */
  private static final int EXACT = 2;

  /** Tableau accueillant les clés de hachage identifiant les états de jeu. */
  private final int [] _hashCodes;

  /** Tableau types d'éléments. */
  private final byte [] _types;

  /** Tableau des profondeurs. */
  private final byte [] _depths;

  /** Tableau des valeurs. */
  private final int [] _values;

  /**
   * Instancie une nouvelle table de transposition.
   * 
   * @param pCapacite Taille maximale de la table de transposition.
   */
  TranspositionTable(final int pCapacite)
  {
    assert pCapacite > 0;

    _hashCodes = new int [ pCapacite ];
    _types = new byte [ pCapacite ];
    _depths = new byte [ pCapacite ];
    _values = new int [ pCapacite ];
  }

  /**
   * Vide la table de transposition.
   */
  void clear()
  {
    Arrays.fill(_hashCodes, 0);
  }

  /**
   * Renvoi l'éventuel valeur correspondant à un état de jeu dans le contexte de I.A. en cours.
   * 
   * @param pEtat Etat recherché.
   * @param pProfondeur Profondeur du résulat.
   * @param pAlpha Valeur alpha.
   * @param pBeta Valeur beta.
   * @return Valeur correspondante, ou null si inconnue.
   */
  Integer get(final Board pEtat, final int pProfondeur, final int pAlpha, final int pBeta)
  {
    assert pEtat != null;
    // TODO: assert pAlpha <= pBeta;

    final int cleCherchee = pEtat.hashCode();
    final int capacite = _hashCodes.length;
    int pos = Math.abs(cleCherchee % capacite);
    int cle = _hashCodes[pos];
    for (int i = MAX_COLLISIONS; (cle != 0) && (--i >= 0); /* Pré-décrémenté */)
    {
      if (cle == cleCherchee)
      {
        if (_depths[pos] >= pProfondeur)
        {
          final int type = _types[pos];
          final int val = _values[pos];
          if ((type == EXACT) || ((type == ALPHA) && (val <= pAlpha))
              || ((type == BETA) && (val >= pBeta)))
          {
            return Integer.valueOf(val);
          }
        }
        break;
      }
      pos++;
      if (pos >= capacite)
      {
        pos -= capacite;
      }
      cle = _hashCodes[pos];
    }

    return null;
  }

  /**
   * Stocke la valeur donnée à un état dans la table de transposition.
   * 
   * @param pEtat Etat de jeu.
   * @param pProfondeur Profondeur liée au résultat.
   * @param pAlpha Valeur alpha.
   * @param pBeta Valeur beta.
   * @param pValeur Valeur du résultat.
   */
  void put(final Board pEtat, final int pProfondeur, final int pAlpha, final int pBeta,
      final int pValeur)
  {
    assert pEtat != null;
    // TODO: assert pAlpha <= pBeta;

    final int cleEtat = pEtat.hashCode();
    final int capacite = _hashCodes.length;
    int pos = Math.abs(cleEtat % capacite);
    int cle = _hashCodes[pos];
    for (int i = MAX_COLLISIONS; (cle != 0) && (cle != cleEtat) && (--i >= 0); /* Pré-décrémenté */)
    {
      pos++;
      if (pos >= capacite)
      {
        pos -= capacite;
      }
      cle = _hashCodes[pos];
    }

    if ((cle == 0) || ((cle == cleEtat) && (_depths[pos] < pProfondeur)))
    {
      byte type = EXACT;
      if (pProfondeur > 0)
      {
        if (pValeur > pBeta)
        {
          type = BETA;
        }
        else if (pValeur < pAlpha)
        {
          type = ALPHA;
        }
      }

      _hashCodes[pos] = cleEtat;
      _types[pos] = type;
      _depths[pos] = (byte) pProfondeur;
      _values[pos] = pValeur;
    }
  }
}
