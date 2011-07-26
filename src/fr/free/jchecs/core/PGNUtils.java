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
import static fr.free.jchecs.core.Constants.APPLICATION_NAME;
import static fr.free.jchecs.core.Constants.APPLICATION_VERSION;
import static fr.free.jchecs.core.FENUtils.STANDART_STARTING_FEN;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.SANUtils.toMove;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import fr.free.jchecs.ai.EngineFactory;

/**
 * Classes fournissant des fonctions utilitaires pour gérer la notation PGN.
 * <p>
 * Classe sûre vis-à-vis des threads.
 * </p>
 * 
 * @author David Cotton
 */
public final class PGNUtils
{
  /** Modèle de découpage de chaines suivant les espaces. */
  private static final Pattern SPLITTER = Pattern.compile("[ ]+");

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private PGNUtils()
  {
    // Rien de spécifique...
  }

  /**
   * Formate une date suivant le standard PGN.
   * 
   * @param pDate Date à formater.
   * @return Chaine au format attendu par PGN.
   */
  private static String formatPGNDate(final Date pDate)
  {
    assert pDate != null;

    return new SimpleDateFormat("yyyy.MM.dd").format(pDate);
  }

  /**
   * Renvoi la description de partie correspondant à la première partie rencontrée dans un flux au
   * format PGN.
   * 
   * @param pFlux Flux contenant les données au format PGN.
   * @return Description de partie correspondante.
   * @throws PGNException en cas d'erreur dans le format du flux PGN.
   */
  public static Game toGame(final BufferedReader pFlux) throws PGNException
  {
    assert pFlux != null;

    final Game res = new Game();

    try
    {
      String ligneLue = pFlux.readLine();
      while (ligneLue != null)
      {
        String ligne = ligneLue.trim();
        if (ligne.startsWith("["))
        {
          // Interprétation des tags d'en-tête...
          final int debTag = ligne.indexOf(" \"");
          if ((debTag >= 0) && ligne.endsWith("\"]"))
          {
            final String contenu = ligne.substring(debTag + 2, ligne.indexOf("\"]")).trim();
            if (ligne.startsWith("[Black \""))
            {
              final Player joueur = res.getPlayer(false);
              if (contenu.startsWith(APPLICATION_NAME + '.'))
              {
                joueur.setEngine(EngineFactory.newInstance(contenu));
                joueur.setName(contenu.substring(APPLICATION_NAME.length() + 1));
              }
              else
              {
                joueur.setEngine(null);
                joueur.setName(contenu);
              }
            }
            else if (ligne.startsWith("[White \""))
            {
              final Player joueur = res.getPlayer(true);
              if (contenu.startsWith(APPLICATION_NAME + '.'))
              {
                joueur.setEngine(EngineFactory.newInstance(contenu));
                joueur.setName(contenu.substring(APPLICATION_NAME.length() + 1));
              }
              else
              {
                joueur.setEngine(null);
                joueur.setName(contenu);
              }
            }
            else if (ligne.startsWith("[FEN \""))
            {
              try
              {
                final Board depart = toBoard(contenu);
                res.resetTo(BoardFactory.valueOf(FASTEST, STARTING).derive(depart));
              }
              catch (final FENException e)
              {
                throw new PGNException("Invalid FEN tag", e);
              }
            }
          }
        }
        else
        {
          // Concaténation de la liste des mouvements...
          final StringBuilder sb = new StringBuilder();
          while (ligneLue != null)
          {
            ligne = ligneLue.trim();
            if (ligne.startsWith("[Event"))
            {
              break;
            }
            sb.append(' ').append(ligne);
            ligneLue = pFlux.readLine();
          }
          // Nettoyage de la chaine...
          int p = 0;
          int prof = 0;
          while (p < sb.length())
          {
            final char c = sb.charAt(p);
            if ((c == '(') || (c == '{'))
            {
              // Supprime les commentaires, et les propositions de nul...
              prof++;
            }
            if ((prof != 0) || (c == '+') || (c == '#'))
            {
              // Supprime les marqueurs d'échecs et de mat.
              sb.deleteCharAt(p);
            }
            else
            {
              if (c == 'O')
              {
                // Convertir les "o" majuscules en zéro...
                sb.setCharAt(p, '0');
              }
              p++;
            }
            if ((c == '}') || (c == ')'))
            {
              prof--;
            }
          }
          p = 0;
          while (p < sb.length())
          {
            final char c = sb.charAt(p);
            if (c == '.')
            {
              // Supprime les numéros de coups...
              int deb = p - 1;
              while ((deb >= 0) && Character.isDigit(sb.charAt(deb)))
              {
                deb--;
              }
              int fin = p + 1;
              while ((fin < sb.length()) && ((" .".indexOf(sb.charAt(fin))) >= 0))
              {
                fin++;
              }
              sb.delete(deb + 1, fin);
            }
            else if (c == '$')
            {
              // Supprime les annotations numériques...
              int fin = p + 1;
              while ((fin < sb.length()) && Character.isDigit(sb.charAt(fin)))
              {
                fin++;
              }
              sb.delete(p, fin + 1);
            }
            else
            {
              p++;
            }
          }

          for (final String mvt : SPLITTER.split(sb.toString()))
          {
            if ("*".equals(mvt) || "1-0".equals(mvt) || "0-1".equals(mvt) || "1/2-1/2".equals(mvt))
            {
              break;
            }
            if (mvt.length() > 0)
            {
              try
              {
                res.moveFromCurrent(toMove(res.getBoard(), toNormalizedSAN(mvt)));
              }
              catch (final SANException e)
              {
                throw new PGNException("Invalid PGN stream", e);
              }
            }
          }

          break;
        }
        ligneLue = pFlux.readLine();
      }
    }
    catch (final IOException e)
    {
      throw new PGNException("PGN stream reading error", e);
    }

    return res;
  }

  /**
   * Converti une chaîne de mouvement "PGN" en chaine de mouvement "SAN" normalisé.
   * 
   * @param pChaine Chaine de mouvement PGN.
   * @return Chaine de mouvement SAN.
   */
  public static String toNormalizedSAN(final String pChaine)
  {
    assert pChaine != null;

    final StringBuilder res = new StringBuilder(pChaine);

    // Le marqueur 'P' pour les pions est possible avec PGN, pas avec SAN...
    if (res.charAt(0) == 'P')
    {
      res.deleteCharAt(0);
    }

    // Les promotions sont indiquées avec un '=' sous PGN, pas sous SAN...
    int p = res.indexOf("=");
    while (p >= 0)
    {
      res.deleteCharAt(p);
      p = res.indexOf("=");
    }

    // Les suffixes d'annotations '?' et '!' ne font pas partie de SAN...
    while ((res.length() > 0) && ("?!".indexOf(res.charAt(res.length() - 1)) >= 0))
    {
      res.deleteCharAt(res.length() - 1);
    }

    // Certains programmes ajoutent des '@' (totalement hors normes)...
    p = res.indexOf("@");
    while (p >= 0)
    {
      res.deleteCharAt(p);
      p = res.indexOf("@");
    }

    return res.toString();
  }

  /**
   * Renvoi une chaîne correspondant à l'image de la partie au format PGN.
   * 
   * @param pPartie Description de la partie.
   * @return Chaine contenant la représentation de la partie au format PGN.
   */
  public static String toPGN(final Game pPartie)
  {
    if (pPartie == null)
    {
      throw new NullPointerException("Missing game description");
    }

    final StringBuilder sb = new StringBuilder();

    sb.append("[Event \"" + APPLICATION_NAME + " v" + APPLICATION_VERSION + " chess game\"]\n");
    String site = "?";
    try
    {
      final InetAddress lh = InetAddress.getLocalHost();
      site = lh.getHostName();
    }
    catch (final UnknownHostException e)
    {
      // Pas grave, on peut se passer de cette information...
    }
    sb.append("[Site \"").append(site).append("\"]\n");
    sb.append("[Date \"").append(formatPGNDate(new Date())).append("\"]\n");
    sb.append("[Round \"-\"]\n");
    Player joueur = pPartie.getPlayer(true);
    String nom = joueur.getName();
    if (joueur.getEngine() != null)
    {
      nom = APPLICATION_NAME + '.' + nom;
    }
    sb.append("[White \"").append(nom).append("\"]\n");
    joueur = pPartie.getPlayer(false);
    nom = joueur.getName();
    if (joueur.getEngine() != null)
    {
      nom = APPLICATION_NAME + '.' + nom;
    }
    sb.append("[Black \"").append(nom).append("\"]\n");
    final String resultat;
    switch (pPartie.getState())
    {
      case BLACK_MATES :
        resultat = "0-1";
        break;
      case DRAWN_BY_50_MOVE_RULE :
      case DRAWN_BY_TRIPLE_REPETITION :
      case STALEMATE :
        resultat = "1/2-1/2";
        break;
      case WHITE_MATES :
        resultat = "1-0";
        break;
      case IN_PROGRESS :
        resultat = "*";
        break;
      default :
        resultat = "*";
        assert false;
    }
    sb.append("[Result \"").append(resultat).append("\"]\n");
    final String depart = pPartie.getStartingPosition();
    if (!depart.equals(STANDART_STARTING_FEN))
    {
      sb.append("[SetUp \"1\"]\n");
      sb.append("[FEN \"" + depart + "\"]\n");
    }
    sb.append('\n');
    int col = 0;
    for (final String san : pPartie.getSANStrings())
    {
      // PGN attend des "o" majuscules plutôts que les zéros du SAN standard...
      String pgn = san.replace("0-0-0", "O-O-O");
      pgn = pgn.replace("0-0", "O-O");
      // ... et des "#" plutôt que "++" pour le mat...
      pgn = pgn.replace("++", "#");
      final int l = pgn.length();
      col += l;
      if (col >= 80)
      {
        sb.append('\n');
        col = l;
      }
      sb.append(pgn);
    }
    col += resultat.length();
    if (col >= 80)
    {
      sb.append('\n');
    }
    sb.append(resultat).append('\n');

    return sb.toString();
  }

}
