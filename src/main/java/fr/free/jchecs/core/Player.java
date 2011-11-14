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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import fr.free.jchecs.ai.Engine;

/**
 * Description d'un joueur.
 * 
 * @author David Cotton
 */
public final class Player
{
  /** Support des propriétés liées. */
  private final PropertyChangeSupport _propertyChangeSupport = new PropertyChangeSupport(this);

  /** Indicateur de la couleur du joueur. */
  private final boolean _white;

  /** Moteur d'IA lié au joueur (peut être à null). */
  private Engine _engine;

  /** Nom du joueur. */
  private String _name;

  /**
   * Instancie un nouveau joueur.
   * 
   * @param pCouleur A vrai si le joueur joue les blancs, à faux sinon.
   */
  public Player(final boolean pCouleur)
  {
    _white = pCouleur;
    _name = "";
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
   * Renvoi le moteur d'IA lié au joueur.
   * 
   * @return Moteur d'IA (peut être à null).
   */
  public Engine getEngine()
  {
    return _engine;
  }

  /**
   * Renvoi le nom du joueur.
   * 
   * @return Nom du joueur.
   */
  public String getName()
  {
    assert _name != null;
    return _name;
  }

  /**
   * Renvoi l'indicateur de la couleur jouée.
   * 
   * @return indicateur de couleur.
   */
  public boolean isWhite()
  {
    return _white;
  }

  /**
   * Alimente le moteur d'IA lié au joueur.
   * 
   * @param pIA Moteur d'IA (peut être à null).
   */
  public void setEngine(final Engine pIA)
  {
    if (_engine != pIA)
    {
      final Engine prec = _engine;
      _engine = pIA;
      _propertyChangeSupport.firePropertyChange("engine", prec, _engine);
    }
  }

  /**
   * Alimente le nom du joueur.
   * 
   * @param pNom Nom du joueur.
   */
  public void setName(final String pNom)
  {
    assert pNom != null;

    if (!_name.equals(pNom))
    {
      final String prec = _name;
      _name = pNom;
      _propertyChangeSupport.firePropertyChange("name", prec, _name);
    }
  }
}
