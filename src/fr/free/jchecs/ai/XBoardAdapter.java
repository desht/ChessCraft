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
import static fr.free.jchecs.core.BoardFactory.State.STARTING;
import static fr.free.jchecs.core.BoardFactory.Type.FASTEST;
import static fr.free.jchecs.core.Constants.APPLICATION_NAME;
import static fr.free.jchecs.core.Constants.APPLICATION_VERSION;
import static fr.free.jchecs.core.FENUtils.toBoard;
import static fr.free.jchecs.core.PieceType.PAWN;
import static fr.free.jchecs.core.SANUtils.toMove;
import static fr.free.jchecs.core.SANUtils.toSAN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import fr.free.jchecs.core.Board;
import fr.free.jchecs.core.BoardFactory;
import fr.free.jchecs.core.FENException;
import fr.free.jchecs.core.Game;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.SANException;
import fr.free.jchecs.core.Square;
import fr.free.jchecs.core.Game.State;

/**
 * Classe utilitaire assurant l'interface entre l'I.A. et une I.H.M. utilisant le protocole
 * XBoard/WinBoard.
 * 
 * @author David Cotton
 */
public final class XBoardAdapter
{
  /** Chaine identifiant l'application. */
  private static final String APPLICATION_STRING = APPLICATION_NAME + " " + APPLICATION_VERSION;

  /** Chaine listant les capacités du moteur. */
  private static final String FEATURES_STRING =
      "feature analyze=0 colors=0 myname=\"" + APPLICATION_STRING
          + "\" pause=0 ping=1 playother=0 san=1 setboard=1 sigint=0 sigterm=0 "
          + "time=0 usermove=1 variants=\"normal\" done=1";

  /** Log de la classe. */
  private static final Logger LOGGER = Logger.getLogger(XBoardAdapter.class.getName());

  /** Moteur d'I.A. utilisé. */
  private static final Engine ENGINE = EngineFactory.newInstance();

  /** Etat de la partie. */
  private static Game S_game = new Game();

  /** Etat du mode "force". */
  private static boolean S_forceMode;

  /** Etat du mode de jeu faible/fort. */
  private static boolean S_hardMode;

  /** Indicateur de position illegale. */
  private static boolean S_illegalPosition;

  /**
   * Classe utilitaire : ne pas instancier.
   */
  private XBoardAdapter()
  {
    // Rien de particulier...
  }

  /**
   * Lance l'interface.
   * 
   * @param pArgs Arguments de la ligne de commande : ignorés.
   */
  public static void main(final String [] pArgs)
  {
    assert pArgs != null;

    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (true)
    {
      String commande = null;
      try
      {
        commande = in.readLine();
      }
      catch (final IOException e)
      {
        LOGGER.severe(e.toString());
      }
      if (commande == null)
      {
        LOGGER.severe("Communication error.");
        System.exit(-1);
      }
      else
      {
        parseCommand(commande.trim());
      }
    }
  }

  /**
   * Interprète les commandes reçues.
   * 
   * @param pCommande Commande reçue.
   */
  private static void parseCommand(final String pCommande)
  {
    assert pCommande != null;

    if (pCommande.startsWith("accepted "))
    {
      // Tant mieux, mais il n'y a rien à faire.
    }
    else if (pCommande.startsWith("level "))
    {
      // Non impléméntée...
    }
    else if (pCommande.startsWith("ping "))
    {
      System.out.println("pong" + pCommande.substring(4));
    }
    else if (pCommande.startsWith("protover "))
    {
      System.out.println(FEATURES_STRING);
    }
    else if (pCommande.startsWith("rejected "))
    {
      System.out.println("tellusererror Missing feature " + pCommande.substring(9));
    }
    else if (pCommande.startsWith("result "))
    {
      // Non impléméntée...
    }
    else if (pCommande.startsWith("setboard "))
    {
      final String fen = pCommande.substring(9);

      Board etat = null;
      try
      {
        etat = toBoard(fen);
      }
      catch (final FENException e)
      {
        // Géré par la suite...
      }
      if (etat == null)
      {
        S_illegalPosition = true;
        System.out.println("tellusererror Illegal position");
      }
      else
      {
        S_illegalPosition = false;
        S_game.resetTo(BoardFactory.valueOf(FASTEST, EMPTY).derive(etat));
      }
    }
    else if (pCommande.startsWith("usermove "))
    {
      final String xbSAN = pCommande.substring(9);
      // Filtre les '=' que certains programmes peuvent en cas de promotion de pion...
      // ... et transforme les "o" majuscules utilisés par XBoard pour les roques en zéros.
      final String san = xbSAN.replace("=", "").replace('O', '0');
      Move mvt = null;
      try
      {
        mvt = toMove(S_game.getBoard(), san);
      }
      catch (final SANException e)
      {
        // géré par la suite...
      }
      if ((mvt == null) || S_illegalPosition)
      {
        System.out.println("Illegal move: " + xbSAN);
      }
      else
      {
        S_game.moveFromCurrent(mvt);
        if (!S_forceMode)
        {
          think();
        }
      }
    }
    else if (pCommande.equals("computer"))
    {
      // Non impléméntée...
    }
    else if (pCommande.equals("easy"))
    {
      S_hardMode = false;
    }
    else if (pCommande.equals("force"))
    {
      S_forceMode = true;
    }
    else if (pCommande.equals("go"))
    {
      if (S_illegalPosition)
      {
        System.out.println("Error (illegal position): go");
      }
      else
      {
        S_forceMode = false;
        think();
      }
    }
    else if (pCommande.equals("hard"))
    {
      S_hardMode = true;
    }
    else if (pCommande.equals("new"))
    {
      S_game.resetTo(BoardFactory.valueOf(FASTEST, STARTING));
      S_forceMode = false;
      S_illegalPosition = false;
    }
    else if (pCommande.equals("nopost"))
    {
      // Non impléméntée...
    }
    else if (pCommande.equals("post"))
    {
      // Non impléméntée...
    }
    else if (pCommande.equals("quit"))
    {
      System.exit(0);
    }
    else if (pCommande.equals("random"))
    {
      // Rien à faire.
    }
    else if (pCommande.equals("xboard"))
    {
      System.out.println(APPLICATION_STRING + " started in xboard mode.");
    }
    else
    {
      System.out.println("Error (unknown command): " + pCommande);
    }
  }

  /**
   * Teste la fin de partie.
   * 
   * @param pAbandon Positionné à vrai si l'on souhaite tester l'abandon.
   * @return Vrai si la partie n'est pas terminée.
   */
  private static boolean testResult(final boolean pAbandon)
  {
    String rep = null;

    final State etat = S_game.getState();
    switch (etat)
    {
      case BLACK_MATES :
        rep = "0-1 {Black mates}";
        break;
      case DRAWN_BY_50_MOVE_RULE :
        if (pAbandon)
        {
          // Ne chercher l'abandon que si l'on n'a pas un avantage suffisant (un pion)...
          final MoveGenerator dispo = S_game.getBoard();
          if (ENGINE.getHeuristic().evaluate(dispo, dispo.isWhiteActive()) < PAWN.getValue())
          {
            rep = "1/2-1/2 {Drawn by 50 moves rule}";
          }
        }
        break;
      case DRAWN_BY_TRIPLE_REPETITION :
        if (pAbandon)
        {
          // Ne chercher l'abandon que si l'on est en difficulté (unpion de retard)...
          final MoveGenerator dispo = S_game.getBoard();
          if (ENGINE.getHeuristic().evaluate(dispo, dispo.isWhiteActive()) < -PAWN.getValue())
          {
            rep = "1/2-1/2 {Drawn by triple repetition}";
          }
        }
        break;
      case IN_PROGRESS :
        break;
      case STALEMATE :
        rep = "1/2-1/2 {Stalemate}";
        break;
      case WHITE_MATES :
        rep = "1-0 {White mates}";
        break;
      default :
        assert false;
    }

    if (rep != null)
    {
      System.out.println(rep);
    }

    return rep == null;
  }

  /**
   * Recherche et renvoi le meilleur coup à partir de la position en cours.
   */
  private static void think()
  {
    if (testResult(true))
    {
      final MoveGenerator etat = BoardFactory.valueOf(FASTEST, EMPTY).derive(S_game.getBoard());

      int profondeur = 5;
      if (S_hardMode)
      {
        int nbPieces = 0;
        for (final Square s : Square.values())
        {
          if (etat.getPieceAt(s) != null)
          {
            nbPieces++;
          }
        }
        if (nbPieces <= 6)
        {
          profondeur++;
        }
      }
      ENGINE.setSearchDepthLimit(profondeur);

      final Move mvt = ENGINE.getMoveFor(etat);

      final String san = toSAN(etat, mvt);
      S_game.moveFromCurrent(mvt);
      // Transforme les zéros en "o" majuscules utilisés par XBoard pour les roques...
      // ... et supprime les marques de prise "en passant" inconnues de XBoard.
      final String xbSAN = san.replace(" e.p.", "").replace("O", "0");
      System.out.println("move " + xbSAN);

      testResult(false);
    }
  }
}
