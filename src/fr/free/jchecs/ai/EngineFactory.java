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
import java.util.Set;
import java.util.logging.Logger;

/**
 * Classe fabrique des instances de moteurs d'IA.
 * 
 * @author David Cotton
 */
public final class EngineFactory
{
  /** Liste des moteurs d'IA internes. */
  private static final Map<String, Class<? extends Engine>> INTERNAL_ENGINES =
      new HashMap<String, Class<? extends Engine>>();
  static
  {
    INTERNAL_ENGINES.put("jChecs.AlphaBeta", AlphaBetaEngine.class);
    INTERNAL_ENGINES.put("jChecs.Debug", DebugEngine.class);
    INTERNAL_ENGINES.put("jChecs.MiniMax", MiniMaxEngine.class);
    INTERNAL_ENGINES.put("jChecs.MiniMax++", EnhancedMiniMaxEngine.class);
    INTERNAL_ENGINES.put("jChecs.NegaScout", NegaScoutEngine.class);
    INTERNAL_ENGINES.put("jChecs.Random", RandomEngine.class);
  }

  /** Log de la classe. */
  private static final Logger LOGGER = Logger.getLogger(EngineFactory.class.getName());

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private EngineFactory()
  {
    // Rien de spécifique...
  }

  /**
   * Renvoi la liste des moteurs IA disponibles.
   * 
   * @return Liste des noms des moteurs d'IA disponibles.
   */
  public static String [] getNames()
  {
    final Set<String> lst = INTERNAL_ENGINES.keySet();
    return lst.toArray(new String [ lst.size() ]);
  }

  /**
   * Renvoi une nouvelle instance du moteur d'IA par défaut.
   * 
   * @return Nouvelle instance du moteur d'IA par défaut.
   */
  public static Engine newInstance()
  {
    return newInstance("jChecs.NegaScout");
  }

  /**
   * Renvoi une nouvelle instance du moteur d'IA dont le nom identifiant est transmis.
   * 
   * @param pNom Nom identifiant le moteur IA.
   * @return Nouvelle instance du moteur d'IA correspondant (ou null si aucune correspondance).
   */
  public static Engine newInstance(final String pNom)
  {
    assert pNom != null;

    Engine res = null;

    final Class<? extends Engine> cls = INTERNAL_ENGINES.get(pNom);
    if (cls != null)
    {
      try
      {
        res = cls.newInstance();
      }
      catch (final InstantiationException e)
      {
        LOGGER.fine(e.toString());
      }
      catch (final IllegalAccessException e)
      {
        LOGGER.fine(e.toString());
      }
    }

    if (res == null)
    {
      LOGGER.warning("Invalid engine [" + pNom + ']');
    }

    return res;
  }
}
