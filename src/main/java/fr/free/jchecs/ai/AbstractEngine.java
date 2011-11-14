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

import static fr.free.jchecs.core.BoardFactory.State.EMPTY;
import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import fr.free.jchecs.core.BoardFactory;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;

/**
 * Implémentation de base des moteurs d'IA pour les échecs.
 * 
 * @author David Cotton
 */
abstract class AbstractEngine implements Engine
{
  /** Générateur de nombres aléatoires. */
  protected static final Random RANDOMIZER = new Random();

  /** Valeur d'un Mat. */
  protected static final int MATE_VALUE = Integer.MIN_VALUE / 2;

  /** Modèle de découpage des enregistrements des ouvertures suivant les ';'. */
  static final Pattern SPLITTER = Pattern.compile(";");

  /** Buffer des ouvertures. */
  private static Map<Integer, int []> S_openings;
  static
  {
    final Thread preload = new Thread(new Runnable()
    {
      /**
       * Tâche de fond pour masquer le temps de chargement...
       */
      public void run()
      {
        getFromOpenings(BoardFactory.valueOf(FASTEST, EMPTY));
      }
    });
    preload.setPriority(Thread.MIN_PRIORITY);
    preload.start();
  }

  /** Limite basse de la profondeur de recherche. */
  private final int _minimalSearchDepth;

  /** Limite haute de la profondeur de recherche. */
  private final int _maximalSearchDepth;

  /** Temps total passé en traitement par le moteur. */
  private long _elapsedTime;

  /** Nombre total de demi-coups évalué par le moteur. */
  private int _halfmoveCount;

  /** Fonction d'évalutation utilisée par le moteur. */
  private Heuristic _heuristic;

  /** Fonction de tri des mouvements. */
  private Comparator<Move> _moveSorter;

  /** Drapeau signalant l'activation de la bibliothèque d'ouvertures. */
  private boolean _openingsEnabled;

  /** Score du dernier mouvement. */
  private int _score;

  /** Limite de la profondeur de recherche (en demi-coups). */
  private int _searchDepthLimit;

  /**
   * Instancie un nouveau moteur IA.
   * 
   * @param pProfMin Limite basse de la profondeur de recherche (>= 1).
   * @param pProfMax Limite haute de la profondeur de recherche (>= pProfMin).
   * @param pProfDef Limite par défaut de la profondeur de recherche ([pProfMin, pProfMax]).
   */
  protected AbstractEngine(final int pProfMin, final int pProfMax, final int pProfDef)
  {
    assert pProfMin >= 1;
    assert pProfMax >= pProfMin;

    _minimalSearchDepth = pProfMin;
    _maximalSearchDepth = pProfMax;

    setSearchDepthLimit(pProfDef);

    setHeuristic(new MobilityHeuristic());
    setMoveSorter(new StaticMoveSorter());
    setOpeningsEnabled(true);
  }

  /**
   * Ajoute une durée (en ms) au temps total de traitement par le moteur.
   * 
   * @param pDuree Duree en ms à ajouter.
   */
  protected final void addElapsedTime(final long pDuree)
  {
    assert pDuree >= 0;

    _elapsedTime += pDuree;
  }

  /**
   * Ajoute un décompte de demi-coups au nombre de demi-coups évalués par le moteur.
   * 
   * @param pNombre Nombre de demi-coups à ajouter.
   */
  protected final void addHalfmove(final int pNombre)
  {
    assert pNombre >= 0;

    _halfmoveCount += pNombre;
  }

  /**
   * Renvoi le temps total passé en traitement par le moteur.
   * 
   * @return Temps total de traitement par le moteur (en ms).
   */
  public final long getElapsedTime()
  {
    assert _elapsedTime >= 0;
    return _elapsedTime;
  }

  /**
   * Renvoi le mouvement correspondant à une position dans la bibliothèque d'ouverture.
   * 
   * @param pEtat Etat du jeu.
   * @return Mouvement correspondant (ou null)
   */
  static final synchronized Move getFromOpenings(final MoveGenerator pEtat)
  {
    assert pEtat != null;

    Move res = null;

    if (S_openings == null)
    {
      final InputStream is = Engine.class.getResourceAsStream("jchecs.opn");
      if (is != null)
      {
        S_openings = new HashMap<Integer, int []>();
        DataInputStream in = null;
        try
        {
          in = new DataInputStream(new GZIPInputStream(is));
          while (in.available() > 0)
          {
            final int nb = in.readByte();
            assert (nb > 0) && (nb <= 5);
            final int [] mvtsId = new int [ nb ];
            final int cle = in.readInt();
            for (int i = 0; i < nb; i++)
            {
              mvtsId[i] = (in.readUnsignedShort() << 8) + in.readUnsignedByte();
              assert (mvtsId[i] & 0xFF000000) == 0;
            }
            S_openings.put(Integer.valueOf(cle), mvtsId);
          }
        }
        catch (final EOFException e)
        {
          // Pas grave, le coup sera calculé...
        }
        catch (final IOException e)
        {
          // Pas grave, le coup sera calculé...
          assert false;
        }
        finally
        {
          if (in != null)
          {
            try
            {
              in.close();
            }
            catch (final IOException e1)
            {
              // On aura essayé :-)
            }
          }
        }
      }
    }

    int [] ids = null;
    if (S_openings != null)
    {
      ids = S_openings.get(Integer.valueOf(pEtat.hashCode()));
    }
    if (ids != null)
    {
      res = Move.valueOf(ids[RANDOMIZER.nextInt(ids.length)]);
      // Les hashcodes n'étant pas infaïbles, il vaut mieux valider le mouvement obtenu...
      boolean erreurHashcode = true;
      for (final Move mvt : pEtat.getValidMoves(pEtat.isWhiteActive()))
      {
        if (mvt.equals(res))
        {
          erreurHashcode = false;
          break;
        }
      }
      if (erreurHashcode)
      {
        res = null;
      }
    }

    return res;
  }

  /**
   * Renvoi le nombre total de demi-coups évalués par le moteur.
   * 
   * @return Nombre total de demi-coups évalués par le moteur.
   */
  public final int getHalfmoveCount()
  {
    assert _halfmoveCount >= 0;
    return _halfmoveCount;
  }

  /**
   * Renvoi la fonction d'évaluation utilisée par le moteur.
   * 
   * @return Fonction d'évaluation utilisée.
   */
  public final Heuristic getHeuristic()
  {
    assert _heuristic != null;
    return _heuristic;
  }

  /**
   * Renvoi la limite haute de la profondeur de recherche supportées par le moteur.
   * 
   * @return Limite haute de la profondeur de recherche (>= getMinimalSearchDepth()).
   */
  public final int getMaximalSearchDepth()
  {
    assert _maximalSearchDepth >= _searchDepthLimit;
    return _maximalSearchDepth;
  }

  /**
   * Renvoi la limite basse de la profondeur de recherche supportées par le moteur.
   * 
   * @return Limite basse de la profondeur de recherche (>= 1).
   */
  public final int getMinimalSearchDepth()
  {
    assert _minimalSearchDepth <= _searchDepthLimit;
    return _minimalSearchDepth;
  }

  /**
   * Recherche un mouvement répondant à un état de l'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @return Mouvement trouvé.
   */
  public final synchronized Move getMoveFor(final MoveGenerator pEtat)
  {
    assert pEtat != null;

    final long debut = System.currentTimeMillis();

    Move res = null;

    setScore(0);

    if (_openingsEnabled && (pEtat.getFullmoveNumber() < 20))
    {
      res = getFromOpenings(pEtat);
    }

    if (res == null)
    {
      // Calcul du meilleur coup...
      final Move [] coups = pEtat.getValidMoves(pEtat.isWhiteActive());
      assert coups.length > 0;

      res = searchMoveFor(pEtat, coups);
    }

    final long duree = System.currentTimeMillis() - debut;
    addElapsedTime(duree);

    assert res != null;
    return res;
  }

  /**
   * Renvoi la fonction de tri des mouvements.
   * 
   * @return Fonction de tri des mouvements.
   */
  public final Comparator<Move> getMoveSorter()
  {
    assert _moveSorter != null;
    return _moveSorter;
  }

  /**
   * Renvoi le score obtenu par le dernier mouvement calculé.
   * 
   * @return Score du dernier mouvement.
   */
  public final int getScore()
  {
    return _score;
  }

  /**
   * Renvoi la valeur limite de la profondeur de recherche (en demi-coups).
   * 
   * @return Limite de la profondeur de recherche ([getMinimalSearchDepth(),
   *         getMaximalSearchDepth()]).
   */
  public final int getSearchDepthLimit()
  {
    assert _searchDepthLimit >= _minimalSearchDepth;
    return _searchDepthLimit;
  }

  /**
   * Indique si l'utilisation de la bibliothèque d'ouvertures est activée.
   * 
   * @return "true" si les ouvertures sont utilisées, "false" sinon.
   */
  public final boolean isOpeningsEnabled()
  {
    return _openingsEnabled;
  }

  /**
   * Corps de la recherche du "meilleur" demi-coup pour un état de l'échiquier.
   * 
   * @param pEtat Etat de l'échiquier.
   * @param pCoups Liste des mouvement initiaux valides.
   * @return Mouvement trouvé.
   */
  protected abstract Move searchMoveFor(final MoveGenerator pEtat, final Move [] pCoups);

  /**
   * Modifie la fonction d'évaluation utilisée par le moteur.
   * 
   * @param pHeuristique Nouvelle fonction d'évaluation à utiliser.
   */
  public final void setHeuristic(final Heuristic pHeuristique)
  {
    assert pHeuristique != null;

    _heuristic = pHeuristique;
  }

  /**
   * Modifie la fonction d'ordenancement des mouvements.
   * 
   * @param pComparateur Nouvelle fonction de tri des mouvements.
   */
  public final void setMoveSorter(final Comparator<Move> pComparateur)
  {
    assert pComparateur != null;

    _moveSorter = pComparateur;
  }

  /**
   * Active / désactive l'utilisation de la bibliothèque d'ouvertures.
   * 
   * @param pActif A "true" pour activer l'utilisation des ouvertures, à "false" sinon.
   */
  public final void setOpeningsEnabled(final boolean pActif)
  {
    _openingsEnabled = pActif;
  }

  /**
   * Alimente le score obtenu par le dernier mouvement calculé.
   * 
   * @param pScore Score du dernier mouvement.
   */
  protected final void setScore(final int pScore)
  {
    _score = pScore;
  }

  /**
   * Aliment la valeur de la limite de la profondeur de recherche (en demi-coups).
   * 
   * @param pLimite Limite de la profondeur de recherche ([getMinimalSearchDepth(),
   *          getMaximalSearchDepth()]).
   */
  public final void setSearchDepthLimit(final int pLimite)
  {
    assert pLimite >= _minimalSearchDepth;

    _searchDepthLimit = pLimite;
  }
}
