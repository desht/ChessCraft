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

import static fr.free.jchecs.core.Constants.FILE_COUNT;
import static fr.free.jchecs.core.Constants.RANK_COUNT;

import java.io.Serializable;

/**
 * Description d'une cellule de l'échiquier.
 * <p>
 * Les instances de cette classe sont des <b>singletons immuables</b> : classe sûre vis-à-vis des
 * threads et permettant des comparaisons directes sur les références d'objets.
 * </p>
 * 
 * @author David Cotton
 */
public final class Square implements Comparable<Square>, Serializable
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -5504534226984844152L;

  /** Liste des coordonnées de cellules. */
  private static final Square [] SQUARES;
  static
  {
    SQUARES = new Square [ FILE_COUNT * RANK_COUNT ];
    int i = 0;
    for (int y = 0; y < RANK_COUNT; y++)
    {
      for (int x = 0; x < FILE_COUNT; x++)
      {
        SQUARES[i++] = new Square(x, y);
      }
    }
  }

  /** Colonne. */
  private final int _file;

  /** Ligne. */
  private final int _rank;

  /** Indice de la cellule. */
  private final transient int _index;

  /** Chaine de description de la cellule. */
  private final transient String _string;

  /** Chaine FEN identifiant la cellule. */
  private final transient String _fenString;

  /**
   * Instancie une nouvelle coordonnée.
   * 
   * @param pColonne Colonne de la cellule (de 0 à 7).
   * @param pLigne Ligne de la colonne (de 0 à 7).
   */
  private Square(final int pColonne, final int pLigne)
  {
    assert (pColonne >= 0) && (pColonne < FILE_COUNT);
    assert (pLigne >= 0) && (pLigne < RANK_COUNT);

    _file = pColonne;
    _rank = pLigne;
    _index = pColonne + pLigne * RANK_COUNT;

    final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append("[file=").append((char) ('a' + getFile()));
    sb.append(",rank=").append((char) ('1' + getRank()));
    sb.append(']');
    _string = sb.toString();

    sb.delete(0, sb.length());
    sb.append((char) ('a' + getFile())).append((char) ('1' + getRank()));
    _fenString = sb.toString();
  }

  /**
   * Fixe l'ordre de tri entre les cases.
   * 
   * @param pCase Case avec laquelle comparer.
   * @return -1 si pCase est inférieure, 0 si égale et 1 si supérieure.
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(final Square pCase)
  {
    if (pCase == null)
    {
      throw new NullPointerException();
    }

    return getFENString().compareTo(pCase.getFENString());
  }

  /**
   * Teste l'égalité entre deux descriptions de cellules.
   * 
   * @param pObjet Objet avec lequel comparer.
   * @return Vrai si les deux objets sont égaux.
   */
  @Override
  public boolean equals(final Object pObjet)
  {
    if (pObjet == this)
    {
      return true;
    }

    if (!(pObjet instanceof Square))
    {
      return false;
    }

    final Square o = (Square) pObjet;
    return _index == o._index;
  }

  /**
   * Renvoi la chaine FEN identifiant la cellule.
   * 
   * @return Identifiant FEN.
   */
  public String getFENString()
  {
    assert _fenString != null;
    return _fenString;
  }

  /**
   * Renvoi la colonne de la cellule.
   * 
   * @return Colonne de la cellule.
   */
  public int getFile()
  {
    assert (_file >= 0) && (_file < FILE_COUNT);
    return _file;
  }

  /**
   * Renvoi l'indice de la cellule.
   * 
   * @return Indece de la cellule (entre 0 et 63).
   */
  public int getIndex()
  {
    assert (_index >= 0) && (_index < FILE_COUNT * RANK_COUNT);
    return _index;
  }

  /**
   * Renvoi la ligne de la cellule.
   * 
   * @return Ligne de la cellule.
   */
  public int getRank()
  {
    assert (_rank >= 0) && (_rank < RANK_COUNT);
    return _rank;
  }

  /**
   * Surcharge du calcul de la clé de hachage.
   * 
   * @return Clé de hachage.
   */
  @Override
  public int hashCode()
  {
    return _index;
  }

  /**
   * Résout la désérialisation d'un objet pour en garantir le comportement "singleton".
   * 
   * @return Instance correspondante dans la JVM.
   */
  private Object readResolve()
  {
    return valueOf(getFile(), getRank());
  }

  /**
   * Renvoi une chaine représentant la cellule.
   * 
   * @return Chaine représentant la case.
   */
  @Override
  public String toString()
  {
    assert _string != null;
    return _string;
  }

  /**
   * Renvoi l'instance correspondant à un indice.
   * 
   * @param pIndice Indice de la case (entre 0 et 63).
   * @return Instance correspondante.
   */
  public static Square valueOf(final int pIndice)
  {
    assert (pIndice >= 0) && (pIndice < FILE_COUNT * RANK_COUNT);

    return SQUARES[pIndice];
  }

  /**
   * Renvoi l'instance correspondant à une notation FEN.
   * 
   * @param pChaine Chaine FEN décrivant la case.
   * @return Instance correspondante.
   */
  public static Square valueOf(final String pChaine)
  {
    if (pChaine == null)
    {
      throw new NullPointerException("Missing square string");
    }
    if (pChaine.length() != 2)
    {
      throw new IllegalArgumentException("Illegal square string [" + pChaine + ']');
    }

    return valueOf(pChaine.charAt(0) - 'a', pChaine.charAt(1) - '1');
  }

  /**
   * Renvoi l'instance correspondant à une cellule.
   * 
   * @param pColonne Colonne de la cellule (de 0 à 7).
   * @param pLigne Ligne de la colonne (de 0 à 7).
   * @return Instance correspondante.
   */
  public static Square valueOf(final int pColonne, final int pLigne)
  {
    if ((pColonne < 0) || (pColonne >= FILE_COUNT))
    {
      throw new IllegalArgumentException("Illegal file [" + pColonne + ']');
    }
    if ((pLigne < 0) || (pLigne >= RANK_COUNT))
    {
      throw new IllegalArgumentException("Illegal rank [" + pLigne + ']');
    }

    return SQUARES[pColonne + pLigne * FILE_COUNT];
  }

  /**
   * Renvoi la liste des cases.
   * 
   * @return Liste des cases.
   */
  public static Square [] values()
  {
    final Square [] res = new Square [ FILE_COUNT * RANK_COUNT ];
    System.arraycopy(SQUARES, 0, res, 0, FILE_COUNT * RANK_COUNT);

    return res;
  }
}
