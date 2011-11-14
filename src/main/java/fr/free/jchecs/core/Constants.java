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

/**
 * Classe utilitaire fournissant les constantes de base de jChecs.
 * 
 * @author David Cotton
 */
public final class Constants
{
  /** Nom de l'auteur du programme (={@value}). */
  public static final String APPLICATION_AUTHOR = "D.Cotton";

  /** Adresse e-mail du programme (={@value}). */
  public static final String APPLICATION_MAIL = "jchecs@free.fr";

  /** Nom court du programme (={@value}). */
  public static final String APPLICATION_NAME = "jChecs";

  /** Version du programme (={@value}). */
  public static final String APPLICATION_VERSION = "0.1.0";

  /** Adresse Web du site du programme (={@value}). */
  public static final String APPLICATION_WEB = "http://jchecs.sourceforge.net";

  /** Nombre de colonnes de l'échiquier (={@value}). */
  public static final int FILE_COUNT = 8;

  /** Nombre de lignes de l'échiquier (={@value}). */
  public static final int RANK_COUNT = 8;

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private Constants()
  {
    // Rien de spécifique...
  }
}
