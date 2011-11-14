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

import static fr.free.jchecs.core.BoardFactory.State.STARTING;
import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;
import static fr.free.jchecs.core.FENUtils.toFEN;
import static fr.free.jchecs.core.SANUtils.toSAN;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Description d'une partie en cours.
 * 
 * @author David Cotton
 */
public final class Game
{
  /** Temps (en ms) alloué à un joueur, pour une partie. */
  private static final long GAME_DURATION = 15 * 60 * 1000;

  /** Support des propriétés liées. */
  final PropertyChangeSupport _propertyChangeSupport = new PropertyChangeSupport(this);

  /** Description du joueur noir. */
  private final Player _blackPlayer = new Player(false);

  /** Liste des positions de l'échiquier. */
  private final List<MoveGenerator> _positions = new ArrayList<MoveGenerator>();

  /** Liste des mouvements éxécutés. */
  private final List<Move> _moves = new ArrayList<Move>();

  /** Liste des notations SAN des mouvements. */
  private final List<String> _sanMoves = new ArrayList<String>();

  /** Description du joueur blanc. */
  private final Player _whitePlayer = new Player(true);

  /** Valeur courante du compteur de temps des noirs. */
  long _blackTimer;

  /** Mouvement courant (>= 0). */
  private int _currentMove;

  /** Position courante (> 0). */
  private int _currentPosition;

  /** Valeur du dernier relevé du timer. */
  long _lastTimerTick;

  /** Timer de l'horloge. */
  private Timer _timer;

  /** Valeur courante du compteur de temps des blancs. */
  long _whiteTimer;

  /**
   * Instancie une nouvelle partie.
   */
  public Game()
  {
    resetTo(BoardFactory.valueOf(FASTEST, STARTING));
  }

  /**
   * Ajoute un objet à l'écoute des changements de propriétés.
   * 
   * @param pPropriete Propriété à écouter.
   * @param pEcouteur Objet à ajouter à l'écoute.
   */
  public void addPropertyChangeListener(final String pPropriete,
      final PropertyChangeListener pEcouteur)
  {
    assert pPropriete != null;
    assert pEcouteur != null;

    _propertyChangeSupport.addPropertyChangeListener(pPropriete, pEcouteur);
  }

  /**
   * Renvoi la description courante de l'échiquier.
   * 
   * @return Description courante de l'échiquier.
   */
  public MoveGenerator getBoard()
  {
    assert (_currentPosition > 0) && (_currentPosition <= _positions.size());

    return _positions.get(_currentPosition - 1);
  }

  /**
   * Renvoi l'éventuel mouvement en cours.
   * 
   * @return Mouvement courant (ou null si aucun).
   */
  public Move getCurrentMove()
  {
    Move res = null;

    if ((_currentMove > 0) && (_currentMove <= _moves.size()))
    {
      res = _moves.get(getCurrentMoveIndex());
    }

    return res;
  }

  /**
   * Renvoi l'indice du mouvement courant.
   * 
   * @return Index du mouvement actuel.
   */
  public int getCurrentMoveIndex()
  {
    assert (_currentMove >= 0) && (_currentMove <= _moves.size());
    return _currentMove - 1;
  }

  /**
   * Renvoi la chaine FEN correspondant à la position courante.
   * 
   * @return Chaîne FEN de la position courante.
   */
  public String getFENPosition()
  {
    assert (_currentPosition > 0) && (_currentPosition <= _positions.size());

    return toFEN(getBoard());
  }

  /**
   * Renvoi le nombre de mouvements stocké.
   * 
   * @return Nombre de mouvements.
   */
  public int getMovesCount()
  {
    return _moves.size();
  }

  /**
   * Renvoi l'ensemble des mouvements juqu'au mouvement courant.
   * 
   * @return Liste des mouvements jusqu'au mouvement courant.
   */
  public Move [] getMovesToCurrent()
  {
    final Move [] res = new Move [ _currentMove ];

    for (int i = 0; i < _currentMove; i++)
    {
      res[i] = _moves.get(i);
    }

    return res;
  }

  /**
   * Renvoi la définition de joueur correspondant à une couleur.
   * 
   * @param pCouleur A "true" pour les joueur blanc, "false" pour le noir.
   * @return Joueur correspondant.
   */
  public Player getPlayer(final boolean pCouleur)
  {
    if (pCouleur)
    {
      return _whitePlayer;
    }

    return _blackPlayer;
  }

  /**
   * Renvoi la liste des chaînes SAN correspondant aux mouvements.
   * 
   * @return Liste des chaînes SAN.
   */
  public String [] getSANStrings()
  {
    return _sanMoves.toArray(new String [ _sanMoves.size() ]);
  }

  /**
   * Renvoi la chaîne FEN de la position de départ.
   * 
   * @return Chaîne FEN de la position de départ.
   */
  public String getStartingPosition()
  {
    return toFEN(_positions.get(0));
  }

  /**
   * Renvoi l'état de la partie en cours.
   * 
   * @return Etat de la partie.
   */
  public State getState()
  {
    final State res;

    final MoveGenerator etat = getBoard();
    final boolean trait = etat.isWhiteActive();
    if (etat.getValidMoves(trait).length == 0)
    {
      if (etat.isInCheck(trait))
      {
        if (trait)
        {
          res = State.BLACK_MATES;
        }
        else
        {
          res = State.WHITE_MATES;
        }
      }
      else
      {
        res = State.STALEMATE;
      }
    }
    else if (etat.getHalfmoveCount() > 50)
    {
      res = State.DRAWN_BY_50_MOVE_RULE;
    }
    else
    {
      final MoveGenerator enCours = getBoard();
      int rep = 0;
      for (int i = 0; i < _currentPosition; i++)
      {
        if (enCours.equals(_positions.get(i)))
        {
          rep++;
          if (rep >= 3)
          {
            return State.DRAWN_BY_TRIPLE_REPETITION;
          }
        }
      }

      res = State.IN_PROGRESS;
    }

    return res;
  }

  /**
   * Renvoi la valeur courante d'un timer pour un joueur.
   * 
   * @param pCouleur A "true" pour le timer des blancs, à "false" pour les noirs.
   * @return Valeur courante du timer correspondant.
   */
  public long getTimer(final boolean pCouleur)
  {
    if (pCouleur)
    {
      return _whiteTimer;
    }

    return _blackTimer;
  }

  /**
   * Aller au premier mouvement.
   */
  public void goFirst()
  {
    if (_currentMove > 0)
    {
      _currentMove = 0;
      _currentPosition = 1;
      _propertyChangeSupport.firePropertyChange("position", null, null);
    }
  }

  /**
   * Aller au dernier mouvement.
   */
  public void goLast()
  {
    final int s = _moves.size();
    if (_currentMove < s)
    {
      _currentMove = s;
      _currentPosition = _positions.size();
      _propertyChangeSupport.firePropertyChange("position", null, null);
    }
  }

  /**
   * Aller au mouvement suivant.
   */
  public void goNext()
  {
    if (_currentMove < _moves.size())
    {
      _currentMove++;
      _currentPosition++;
      _propertyChangeSupport.firePropertyChange("position", null, null);
    }
  }

  /**
   * Aller au mouvement précédent.
   */
  public void goPrevious()
  {
    if (_currentMove > 0)
    {
      _currentMove--;
      _currentPosition--;
      _propertyChangeSupport.firePropertyChange("position", null, null);
    }
  }

  /**
   * Ajoute un mouvement à partir de la position courante.
   * 
   * @param pMouvement Mouvement à ajouter.
   */
  public void moveFromCurrent(final Move pMouvement)
  {
    assert pMouvement != null;

    while (_moves.size() > _currentMove)
    {
      _moves.remove(_moves.size() - 1);
      _sanMoves.remove(_sanMoves.size() - 1);
    }
    while (_positions.size() > _currentPosition)
    {
      _positions.remove(_positions.size() - 1);
    }

    MoveGenerator etat = getBoard();
    final boolean trait = etat.isWhiteActive();
    final StringBuilder san = new StringBuilder();
    if (trait)
    {
      san.append(etat.getFullmoveNumber()).append(". ");
    }
    san.append(toSAN(etat, pMouvement));
    san.append(' ');
    etat = etat.derive(pMouvement, true);
    _positions.add(etat);
    _currentPosition = _positions.size();
    _moves.add(pMouvement);
    _currentMove = _moves.size();

    switch (getState())
    {
      case IN_PROGRESS :
        break;
      case WHITE_MATES :
        san.append("1-0");
        break;
      case BLACK_MATES :
        san.append("0-1");
        break;
      case STALEMATE :
        san.append("1/2-1/2");
        break;
      case DRAWN_BY_50_MOVE_RULE :
      case DRAWN_BY_TRIPLE_REPETITION :
        san.append("1/2-1/2 {Repetition}");
        break;
      default :
        assert false;
    }
    _sanMoves.add(san.toString());

    assert _moves.size() == _sanMoves.size();
    assert _positions.size() == (_moves.size() + 1);
    _propertyChangeSupport.firePropertyChange("position", null, null);
  }

  /**
   * Supprime un objet à l'écoute des changements de propriétés.
   * 
   * @param pPropriete Propriété à écouter.
   * @param pEcouteur Objet à ajouter à l'écoute.
   */
  public void removePropertyChangeListener(final String pPropriete,
      final PropertyChangeListener pEcouteur)
  {
    assert pPropriete != null;
    assert pEcouteur != null;

    _propertyChangeSupport.removePropertyChangeListener(pPropriete, pEcouteur);
  }

  /**
   * Ré-initialise la description de la partie à la position transmise.
   * 
   * @param pEtat Nouvel état de l'échiquier.
   */
  public void resetTo(final MoveGenerator pEtat)
  {
    assert pEtat != null;

    _moves.clear();
    _sanMoves.clear();
    _currentMove = _moves.size();
    _positions.clear();
    _positions.add(pEtat);
    _currentPosition = _positions.size();
    if (_timer != null)
    {
      _timer.cancel();
    }
    _blackTimer = GAME_DURATION;
    _whiteTimer = GAME_DURATION;
    _lastTimerTick = System.currentTimeMillis();
    _timer = new Timer();

    _propertyChangeSupport.firePropertyChange("position", null, null);
    _timer.scheduleAtFixedRate(new TimerTask()
    {
      /**
       * Action déclenchée périodiquement par le timer gérant l'horloge.
       */
      @Override
      public void run()
      {
        if (getState() == State.IN_PROGRESS)
        {
          final long time = System.currentTimeMillis();
          if (getBoard().isWhiteActive())
          {
            _whiteTimer -= time - _lastTimerTick;
          }
          else
          {
            _blackTimer -= time - _lastTimerTick;
          }
          _lastTimerTick = time;
          _propertyChangeSupport.firePropertyChange("timer", null, null);
        }
      }
    }, 250, 1000);
  }

  /** Enumération des états possibles d'une partie. */
  public static enum State
  {
    /** En cours. */
    IN_PROGRESS,

    /** Victoire des noirs. */
    BLACK_MATES,

    /** Victoire des blancs. */
    WHITE_MATES,

    /** Pat. */
    STALEMATE,

    /** Terminée suite à la répétition de la même position 3 fois ou plus. */
    DRAWN_BY_TRIPLE_REPETITION,

    /** Terminée par la règle des 50 coups. */
    DRAWN_BY_50_MOVE_RULE;
  }
}
