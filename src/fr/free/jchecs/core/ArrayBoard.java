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
import static fr.free.jchecs.core.Piece.BLACK_BISHOP;
import static fr.free.jchecs.core.Piece.BLACK_KING;
import static fr.free.jchecs.core.Piece.BLACK_KNIGHT;
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.BLACK_QUEEN;
import static fr.free.jchecs.core.Piece.BLACK_ROOK;
import static fr.free.jchecs.core.Piece.WHITE_BISHOP;
import static fr.free.jchecs.core.Piece.WHITE_KING;
import static fr.free.jchecs.core.Piece.WHITE_KNIGHT;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_QUEEN;
import static fr.free.jchecs.core.Piece.WHITE_ROOK;
import static fr.free.jchecs.core.PieceType.BISHOP;
import static fr.free.jchecs.core.PieceType.KING;
import static fr.free.jchecs.core.PieceType.KNIGHT;
import static fr.free.jchecs.core.PieceType.PAWN;
import static fr.free.jchecs.core.PieceType.QUEEN;
import static fr.free.jchecs.core.PieceType.ROOK;

/**
 * Représentation d'un état de la partie basée sur un tableau à deux dimensions pour stocker les
 * positions.
 * <p>
 * Cette façon de coder un plateau de jeu est plus naturelle et simple à valider mais offre de
 * faibles performances : elle est à éviter dans un moteur de génération des coups, mais est idéale
 * pour les tests unitaires de représentations plus complexes.
 * </p>
 * 
 * @author David Cotton
 */
final class ArrayBoard extends AbstractMoveGenerator
{
  /** Instance correspondant à un état initial, sans pièces. */
  static final MoveGenerator EMPTY = new ArrayBoard();

  /** Instance correspondant à l'état initial standard. */
  static final MoveGenerator STARTING;
  static
  {
    final ArrayBoard etat = new ArrayBoard();
    for (int x = 0; x < FILE_COUNT; x++)
    {
      etat._pieces[x][1] = WHITE_PAWN;
      etat._pieces[x][RANK_COUNT - 2] = BLACK_PAWN;
    }
    etat._pieces[0][0] = WHITE_ROOK;
    etat._pieces[1][0] = WHITE_KNIGHT;
    etat._pieces[2][0] = WHITE_BISHOP;
    etat._pieces[3][0] = WHITE_QUEEN;
    etat._pieces[FILE_COUNT - 4][0] = WHITE_KING;
    etat._pieces[FILE_COUNT - 3][0] = WHITE_BISHOP;
    etat._pieces[FILE_COUNT - 2][0] = WHITE_KNIGHT;
    etat._pieces[FILE_COUNT - 1][0] = WHITE_ROOK;
    etat._pieces[0][RANK_COUNT - 1] = BLACK_ROOK;
    etat._pieces[1][RANK_COUNT - 1] = BLACK_KNIGHT;
    etat._pieces[2][RANK_COUNT - 1] = BLACK_BISHOP;
    etat._pieces[3][RANK_COUNT - 1] = BLACK_QUEEN;
    etat._pieces[FILE_COUNT - 4][RANK_COUNT - 1] = BLACK_KING;
    etat._pieces[FILE_COUNT - 3][RANK_COUNT - 1] = BLACK_BISHOP;
    etat._pieces[FILE_COUNT - 2][RANK_COUNT - 1] = BLACK_KNIGHT;
    etat._pieces[FILE_COUNT - 1][RANK_COUNT - 1] = BLACK_ROOK;
    etat.setKingSquare(false, Square.valueOf(4, 7));
    etat.setKingSquare(true, Square.valueOf(4, 0));
    STARTING = etat;
  }

  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -7691142320420490263L;

  /** Liste des modificateurs pour les mouvements d'un roi. */
  private static final int [] KING_MOVES = { -1, 0, 1, 1, 1, 0, -1, -1 };

  /** Liste des modificateurs pour les mouvements d'un cavalier. */
  private static final int [] KNIGHT_MOVES = { -1, 1, 2, 2, 1, -1, -2, -2, };

  /** Description du plateau. */
  private final Piece [][] _pieces = new Piece [ FILE_COUNT ] [ RANK_COUNT ];

  /** Buffer de la clé de hachage (peut être à null). */
  private Integer _hashCode;

  /**
   * Crée une nouvelle instance interne.
   */
  private ArrayBoard()
  {
    // Rien de spécifique...
  }

  /**
   * Crée une nouvelle instance, initialisée à partir de l'état reçu en paramètre.
   * 
   * @param pEtat Instance initial.
   */
  private ArrayBoard(final Board pEtat)
  {
    super(pEtat);

    for (final Square s : Square.values())
    {
      _pieces[s.getFile()][s.getRank()] = pEtat.getPieceAt(s);
    }
  }

  /**
   * Crée une nouvelle instance, copie conforme de l'instance reçue.
   * 
   * @param pEtat Instance à copier.
   */
  private ArrayBoard(final ArrayBoard pEtat)
  {
    super(pEtat);

    for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
    {
      System.arraycopy(pEtat._pieces[x], 0, _pieces[x], 0, RANK_COUNT);
    }
  }

  /**
   * Renvoi une nouvelle instance, initialisée à partir d'un état quelconque.
   * 
   * @param pEtat Etat de départ.
   * @return Copie de l'état.
   */
  public MoveGenerator derive(final Board pEtat)
  {
    assert pEtat != null;

    return new ArrayBoard(pEtat);
  }

  /**
   * Renvoi une nouvelle instance décrivant l'état du jeu après application d'un mouvement.
   * 
   * @param pMouvement Description de mouvement.
   * @param pSuivant Drapeau positionné si l'on souhaite que le jeu passe au demi-coups suivant.
   * @return Instance dérivée.
   */
  public MoveGenerator derive(final Move pMouvement, final boolean pSuivant)
  {
    assert pMouvement != null;

    final ArrayBoard res = new ArrayBoard(this);

    // Ajuste les compteurs...
    if (pSuivant)
    {
      final boolean t = !isWhiteActive();
      res.setWhiteActive(t);
      if (t)
      {
        res.setFullmoveNumber(getFullmoveNumber() + 1);
      }
      if (pMouvement.getCaptured() == null)
      {
        res.setHalfmoveCount(getHalfmoveCount() + 1);
      }
      else
      {
        res.setHalfmoveCount(0);
      }
    }
    // Déplace la pièce...
    final Piece piece = pMouvement.getPiece();
    final PieceType typePiece = piece.getType();
    final boolean trait = piece.isWhite();
    final Square src = pMouvement.getFrom();
    final int xSrc = src.getFile();
    final int ySrc = src.getRank();
    assert res._pieces[xSrc][ySrc] == piece;
    res._pieces[xSrc][ySrc] = null;
    final Square dst = pMouvement.getTo();
    final int xDst = dst.getFile();
    final int yDst = dst.getRank();
    res._pieces[xDst][yDst] = piece;
    // ... éxécute un mouvement spécifique de type "roque" et gère le suivi des rois ...
    if (typePiece == KING)
    {
      res.setKingSquare(trait, dst);
      if (xSrc == 4)
      {
        if (xDst == 2)
        {
          // ... côté reine...
          final Piece tour = res._pieces[0][yDst];
          assert tour != null;
          assert tour.getType() == ROOK;
          res._pieces[0][yDst] = null;
          res._pieces[3][yDst] = tour;
          res.setCastled(trait, true);
        }
        else if (xDst == 6)
        {
          // ... côté roi...
          final Piece tour = res._pieces[FILE_COUNT - 1][yDst];
          assert tour != null;
          assert tour.getType() == ROOK;
          res._pieces[FILE_COUNT - 1][yDst] = null;
          res._pieces[5][yDst] = tour;
          res.setCastled(trait, true);
        }
      }
    }
    // ... éxécute un mouvement spécifique du type "en passant" ...
    if ((typePiece == PAWN) && (dst == getEnPassant()))
    {
      if (trait)
      {
        res._pieces[xDst][yDst - 1] = null;
      }
      else
      {
        res._pieces[xDst][yDst + 1] = null;
      }
    }
    // Gére la promotion des pions...
    if (typePiece == PAWN)
    {
      if (trait)
      {
        assert yDst > ySrc;
        if (yDst == RANK_COUNT - 1)
        {
          res._pieces[xDst][yDst] = WHITE_QUEEN;
        }
      }
      else
      {
        assert yDst < ySrc;
        if (yDst == 0)
        {
          res._pieces[xDst][yDst] = BLACK_QUEEN;
        }
      }
    }
    // Tient compte des interdictions de roquer que le mouvement peut provoquer...
    if (canCastleShort(trait))
    {
      if ((typePiece == KING) || ((typePiece == ROOK) && (xSrc == FILE_COUNT - 1)))
      {
        res.setCastleShort(trait, false);
      }
    }
    if (canCastleLong(trait))
    {
      if ((typePiece == KING) || ((typePiece == ROOK) && (xSrc == 0)))
      {
        res.setCastleLong(trait, false);
      }
    }
    // Détecte si une possibilité de prise "en passant" doit être signalée...
    res.setEnPassant(null);
    if (typePiece == PAWN)
    {
      // En profite pour aussi gérer le compteur de demis coups...
      if (pSuivant)
      {
        res.setHalfmoveCount(0);
      }
      if (trait)
      {
        if ((ySrc == 1) && (yDst == 3))
        {
          res.setEnPassant(Square.valueOf(xDst, 2));
        }
      }
      else
      {
        if ((ySrc == RANK_COUNT - 2) && (yDst == RANK_COUNT - 4))
        {
          res.setEnPassant(Square.valueOf(xDst, RANK_COUNT - 3));
        }
      }
    }

    return res;
  }

  /**
   * Méthode spécialisée pour tester l'égalité entre deux descriptions de ce type.
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

    if (pObjet instanceof ArrayBoard)
    {
      if (hashCode() != pObjet.hashCode())
      {
        return false;
      }

      final ArrayBoard o = (ArrayBoard) pObjet;

      for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
      {
        for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
        {
          if (_pieces[x][y] != o._pieces[x][y])
          {
            return false;
          }
        }
      }

      return equalsInternal(o);
    }

    return super.equals(pObjet);
  }

  /**
   * Renvoi toutes les cases cibles des mouvements possibles (y compris ceux mettant le roi en
   * échec) pour la pièce contenue par une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getAllTargets(final Square pOrigine)
  {
    assert pOrigine != null;

    Square [] res = null;

    final Piece piece = _pieces[pOrigine.getFile()][pOrigine.getRank()];
    if (piece != null)
    {
      final boolean trait = piece.isWhite();
      switch (piece.getType())
      {
        case BISHOP :
          res = getBishopTargets(pOrigine, trait);
          break;
        case KING :
          res = getKingTargets(pOrigine, trait);
          break;
        case KNIGHT :
          res = getKnightTargets(pOrigine, trait);
          break;
        case PAWN :
          res = getPawnTargets(pOrigine, trait);
          break;
        case QUEEN :
          res = getQueenTargets(pOrigine, trait);
          break;
        case ROOK :
          res = getRookTargets(pOrigine, trait);
          break;
        default :
          assert false;
      }
    }
    else
    {
      res = new Square [ 0 ];
    }

    return res;
  }

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "fou" d'une certaine couleur (y
   * compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Positionné à vrai si la recherche concerne les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getBishopTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tmp = new Square [ 13 ];
    int nb = 0;

    final int xSrc = pOrigine.getFile();
    final int ySrc = pOrigine.getRank();

    // Mouvements / prise vers le haut/gauche...
    int xDst = xSrc;
    int yDst = ySrc;
    while ((--xDst >= 0) && (++yDst < RANK_COUNT))
    {
      final Piece p = _pieces[xDst][yDst];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xDst, yDst);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
        break;
      }
    }

    // Mouvements / prise vers le haut/droit...
    xDst = xSrc;
    yDst = ySrc;
    while ((++xDst < FILE_COUNT) && (++yDst < RANK_COUNT))
    {
      final Piece p = _pieces[xDst][yDst];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xDst, yDst);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
        break;
      }
    }

    // Mouvements / prise vers le bas/gauche...
    xDst = xSrc;
    yDst = ySrc;
    while ((--xDst >= 0) && (--yDst >= 0))
    {
      final Piece p = _pieces[xDst][yDst];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xDst, yDst);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
        break;
      }
    }

    // Mouvements / prise vers le bas/droit...
    xDst = xSrc;
    yDst = ySrc;
    while ((++xDst < FILE_COUNT) && (--yDst >= 0))
    {
      final Piece p = _pieces[xDst][yDst];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xDst, yDst);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
        break;
      }
    }

    final Square [] res = new Square [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type roi.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getKingTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tmp = new Square [ 8 ];
    int nb = 0;

    final int xSrc = pOrigine.getFile();
    final int ySrc = pOrigine.getRank();
    final int kLength = KING_MOVES.length;
    for (int i = kLength; --i >= 0; /* Pré-décrémenté */)
    {
      final int xDst = xSrc + KING_MOVES[i];
      final int yDst = ySrc + KING_MOVES[(i + 2) % kLength];
      if ((xDst >= 0) && (yDst >= 0) && (xDst < FILE_COUNT) && (yDst < RANK_COUNT))
      {
        final Piece p = _pieces[xDst][yDst];
        if ((p == null) || (p.isWhite() != pBlanc))
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
      }
    }
    if ((nb > 0) && (xSrc == 4))
    {
      if (canCastleShort(pBlanc) && (_pieces[5][ySrc] == null) && (_pieces[6][ySrc] == null))
      {
        final Piece t = _pieces[FILE_COUNT - 1][ySrc];
        if ((t != null) && (t.getType() == ROOK) && (t.isWhite() == pBlanc))
        {
          tmp[nb++] = Square.valueOf(6, ySrc);
        }
      }
      if (canCastleLong(pBlanc) && (_pieces[3][ySrc] == null) && (_pieces[2][ySrc] == null)
          && (_pieces[1][ySrc] == null))
      {
        final Piece t = _pieces[0][ySrc];
        if ((t != null) && (t.getType() == ROOK) && (t.isWhite() == pBlanc))
        {
          tmp[nb++] = Square.valueOf(2, ySrc);
        }
      }
    }

    final Square [] res = new Square [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type cavalier.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getKnightTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tmp = new Square [ 8 ];
    int nb = 0;

    final int oX = pOrigine.getFile();
    final int oY = pOrigine.getRank();
    final int kLength = KNIGHT_MOVES.length;
    for (int i = kLength; --i >= 0; /* Pré-décrémenté */)
    {
      final int xDst = oX + KNIGHT_MOVES[i];
      final int yDst = oY + KNIGHT_MOVES[(i + 2) % kLength];
      if ((xDst >= 0) && (yDst >= 0) && (xDst < FILE_COUNT) && (yDst < RANK_COUNT))
      {
        final Piece p = _pieces[xDst][yDst];
        if ((p == null) || (p.isWhite() != pBlanc))
        {
          tmp[nb++] = Square.valueOf(xDst, yDst);
        }
      }
    }

    final Square [] res = new Square [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi la liste des cases pouvant être atteintes par un mouvement de type pion.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getPawnTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tmp = new Square [ 4 ];
    int nb = 0;

    final int ySrc = pOrigine.getRank();
    if (pBlanc)
    {
      if (ySrc < RANK_COUNT - 1)
      {
        final int xSrc = pOrigine.getFile();
        // Mouvement de 1...
        if (_pieces[xSrc][ySrc + 1] == null)
        {
          tmp[nb++] = Square.valueOf(xSrc, ySrc + 1);
          // Mouvement initial de 2
          if ((ySrc == 1) && (_pieces[xSrc][3] == null))
          {
            tmp[nb++] = Square.valueOf(xSrc, 3);
          }
        }
        if (xSrc > 0)
        {
          // Prise à gauche (y compris en passant)...
          final Square cDest = Square.valueOf(xSrc - 1, ySrc + 1);
          final Piece pDest = _pieces[xSrc - 1][ySrc + 1];
          if (((pDest != null) && (!pDest.isWhite())) || (cDest == getEnPassant()))
          {
            tmp[nb++] = cDest;
          }
        }
        if (xSrc < FILE_COUNT - 1)
        {
          // Prise à droite (y compris en passant)...
          final Square cDest = Square.valueOf(xSrc + 1, ySrc + 1);
          final Piece pDest = _pieces[xSrc + 1][ySrc + 1];
          if (((pDest != null) && (!pDest.isWhite())) || (cDest == getEnPassant()))
          {
            tmp[nb++] = cDest;
          }
        }
      }
    }
    else
    {
      if (ySrc > 0)
      {
        final int xSrc = pOrigine.getFile();
        // Mouvement de 1...
        if (_pieces[xSrc][ySrc - 1] == null)
        {
          tmp[nb++] = Square.valueOf(xSrc, ySrc - 1);
          // Mouvement initial de 2
          if ((ySrc == RANK_COUNT - 2) && (_pieces[xSrc][RANK_COUNT - 4] == null))
          {
            tmp[nb++] = Square.valueOf(xSrc, RANK_COUNT - 4);
          }
        }
        if (xSrc > 0)
        {
          // Prise à gauche (y compris en passant)...
          final Square cDest = Square.valueOf(xSrc - 1, ySrc - 1);
          final Piece pDest = _pieces[xSrc - 1][ySrc - 1];
          if (((pDest != null) && pDest.isWhite()) || (cDest == getEnPassant()))
          {
            tmp[nb++] = cDest;
          }
        }
        if (xSrc < FILE_COUNT - 1)
        {
          // Prise à droite (y compris en passant)...
          final Square cDest = Square.valueOf(xSrc + 1, ySrc - 1);
          final Piece pDest = _pieces[xSrc + 1][ySrc - 1];
          if (((pDest != null) && pDest.isWhite()) || (cDest == getEnPassant()))
          {
            tmp[nb++] = cDest;
          }
        }
      }
    }

    final Square [] res = new Square [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi l'éventuelle pièce présente sur la case indiquée.
   * 
   * @param pCase Case à tester.
   * @return Pièce présente sur la case (ou null si aucune).
   */
  public Piece getPieceAt(final Square pCase)
  {
    assert pCase != null;

    return getPieceAt(pCase.getFile(), pCase.getRank());
  }

  /**
   * Renvoi l'éventuelle pièce présente sur la case dont les coordonnées sont indiquées.
   * 
   * @param pColonne Colonne de la case à tester (de 0 à 7).
   * @param pLigne Ligne de la case à tester (de 0 à 7).
   * @return Pièce présente sur la case (ou null).
   */
  public Piece getPieceAt(final int pColonne, final int pLigne)
  {
    assert (pColonne >= 0) && (pColonne < FILE_COUNT);
    assert (pLigne >= 0) && (pLigne < RANK_COUNT);

    return _pieces[pColonne][pLigne];
  }

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "dame" d'une certaine couleur
   * (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Mis à vrai pour rechercher pour les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getQueenTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tour = getRookTargets(pOrigine, pBlanc);
    final int tl = tour.length;
    final Square [] fou = getBishopTargets(pOrigine, pBlanc);
    final int fl = fou.length;

    final Square [] res = new Square [ tl + fl ];
    System.arraycopy(tour, 0, res, 0, tl);
    System.arraycopy(fou, 0, res, tl, fl);

    return res;
  }

  /**
   * Renvoi toutes les cases cibles possibles d'un mouvement de type "tour" d'une certaine couleur
   * (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @param pBlanc Mis à vrai pour rechercher pour les blancs.
   * @return Liste des cases cibles (y compris celles conduisant à un échec).
   */
  public Square [] getRookTargets(final Square pOrigine, final boolean pBlanc)
  {
    assert pOrigine != null;

    final Square [] tmp = new Square [ 14 ];
    int nb = 0;

    final int xSrc = pOrigine.getFile();
    final int ySrc = pOrigine.getRank();

    // Mouvements / prise vers la gauche...
    for (int x = xSrc - 1; x >= 0; x--)
    {
      final Piece p = _pieces[x][ySrc];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(x, ySrc);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(x, ySrc);
        }
        break;
      }
    }

    // Mouvements / prise vers la droite...
    for (int x = xSrc + 1; x < FILE_COUNT; x++)
    {
      final Piece p = _pieces[x][ySrc];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(x, ySrc);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(x, ySrc);
        }
        break;
      }
    }

    // Mouvements / prise vers le haut...
    for (int y = ySrc + 1; y < RANK_COUNT; y++)
    {
      final Piece p = _pieces[xSrc][y];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xSrc, y);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xSrc, y);
        }
        break;
      }
    }

    // Mouvements / prise vers le bas...
    for (int y = ySrc - 1; y >= 0; y--)
    {
      final Piece p = _pieces[xSrc][y];
      if (p == null)
      {
        tmp[nb++] = Square.valueOf(xSrc, y);
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          tmp[nb++] = Square.valueOf(xSrc, y);
        }
        break;
      }
    }

    final Square [] res = new Square [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi tous les mouvements valides pour une couleur.
   * 
   * @param pTrait Positionné à "true" pour indiquer une recherche pour les blancs.
   * @return Liste des mouvements valides.
   */
  public Move [] getValidMoves(final boolean pTrait)
  {
    final Move [] tmp = new Move [ 164 ];
    int nb = 0;

    for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
    {
      for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
      {
        final Piece p = _pieces[x][y];
        if ((p != null) && (p.isWhite() == pTrait))
        {
          final Square orig = Square.valueOf(x, y);
          for (final Square dst : getValidTargets(orig))
          {
            final Piece prise;
            if ((p.getType() != PAWN) || (dst != getEnPassant()))
            {
              prise = _pieces[dst.getFile()][dst.getRank()];
            }
            else
            {
              if (pTrait)
              {
                prise = _pieces[dst.getFile()][dst.getRank() - 1];
              }
              else
              {
                prise = _pieces[dst.getFile()][dst.getRank() + 1];
              }
            }
            tmp[nb++] = new Move(p, orig, dst, prise);
          }
        }
      }
    }

    final Move [] res = new Move [ nb ];
    System.arraycopy(tmp, 0, res, 0, nb);

    return res;
  }

  /**
   * Renvoi toutes les cases cibles des mouvements valides à partir d'une case.
   * 
   * @param pOrigine Case à l'origine du mouvement.
   * @return Liste des cases cibles.
   */
  public Square [] getValidTargets(final Square pOrigine)
  {
    assert pOrigine != null;

    final Piece piece = _pieces[pOrigine.getFile()][pOrigine.getRank()];
    if (piece != null)
    {
      final Square [] total = getAllTargets(pOrigine);
      final int tLength = total.length;
      final Square [] tmp = new Square [ tLength ];
      int nb = 0;
      final boolean trait = piece.isWhite();
      for (int t = tLength; --t >= 0; /* Pré-décrémenté */)
      {
        final Square cible = total[t];
        final int xDst = cible.getFile();
        final int yDst = cible.getRank();
        final Piece prise = _pieces[xDst][yDst];
        if (!derive(new Move(piece, pOrigine, cible, prise), false).isInCheck(trait))
        {
          if ((piece.getType() == KING) && (pOrigine.getFile() == 4))
          {
            final int delta = 4 - cible.getFile();
            if ((delta == 2) || (delta == -2))
            {
              // Elimine le roque si le roi est en échec ou s'il le serait sur la case
              // intermédiaire...
              if (isInCheck(trait)
                  || derive(
                      new Move(piece, pOrigine, Square.valueOf(4 - (delta / 2), cible.getRank())),
                      false).isInCheck(trait))
              {
                continue;
              }
            }
          }

          tmp[nb++] = cible;
        }
      }

      final Square [] res = new Square [ nb ];
      System.arraycopy(tmp, 0, res, 0, nb);
      return res;
    }

    return new Square [ 0 ];
  }

  /**
   * Surcharge du calcul des clés de hachage, pour optimisation.
   * 
   * @return Clé de hachage.
   */
  @Override
  public synchronized int hashCode()
  {
    if (_hashCode == null)
    {
      int h = zobristRoot();
      for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
      {
        for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
        {
          final Piece p = _pieces[x][y];
          if (p != null)
          {
            h ^= ZOBRIST_PIECE_POSITION[p.ordinal()][x + y * FILE_COUNT];
          }
        }
      }
      assert h == super.hashCode();

      _hashCode = Integer.valueOf(h);
    }

    return _hashCode.intValue();
  }

  /**
   * Indique si une case est attaquée par une couleur.
   * 
   * @param pCible Case cible.
   * @param pCouleur Positionné à "true" pour tester l'attaque par les blancs.
   * @return Vrai si la case est attaquée.
   */
  public boolean isAttacked(final Square pCible, final boolean pCouleur)
  {
    assert pCible != null;

    final int xSrc = pCible.getFile();
    final int ySrc = pCible.getRank();

    Piece p = null;
    int x = xSrc - 1;
    // Gauche
    while ((x >= 0) && (p == null))
    {
      p = _pieces[x--][ySrc];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == ROOK) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    x = xSrc + 1;
    // Droite
    while ((x < FILE_COUNT) && (p == null))
    {
      p = _pieces[x++][ySrc];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == ROOK) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    int y = ySrc - 1;
    // Bas
    while ((y >= 0) && (p == null))
    {
      p = _pieces[xSrc][y--];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == ROOK) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    y = ySrc + 1;
    // Haut
    while ((y < RANK_COUNT) && (p == null))
    {
      p = _pieces[xSrc][y++];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == ROOK) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    x = xSrc - 1;
    y = ySrc - 1;
    // Bas / Gauche
    while ((x >= 0) && (y >= 0) && (p == null))
    {
      p = _pieces[x--][y--];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == BISHOP) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    x = xSrc - 1;
    y = ySrc + 1;
    // Haut / Gauche
    while ((x >= 0) && (y < RANK_COUNT) && (p == null))
    {
      p = _pieces[x--][y++];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == BISHOP) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    x = xSrc + 1;
    y = ySrc + 1;
    // Haut / Droit
    while ((x < FILE_COUNT) && (y < RANK_COUNT) && (p == null))
    {
      p = _pieces[x++][y++];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == BISHOP) || (t == QUEEN))
      {
        return true;
      }
    }

    p = null;
    x = xSrc + 1;
    y = ySrc - 1;
    // Bas / Droit
    while ((x < FILE_COUNT) && (y >= 0) && (p == null))
    {
      p = _pieces[x++][y--];
    }
    if ((p != null) && (p.isWhite() == pCouleur))
    {
      final PieceType t = p.getType();
      if ((t == BISHOP) || (t == QUEEN))
      {
        return true;
      }
    }

    // Cavalier
    int kLength = KNIGHT_MOVES.length;
    for (int i = kLength; --i >= 0; /* Pré-décrémenté */)
    {
      x = xSrc + KNIGHT_MOVES[i];
      y = ySrc + KNIGHT_MOVES[(i + 2) % kLength];
      if ((x >= 0) && (y >= 0) && (x < FILE_COUNT) && (y < RANK_COUNT))
      {
        p = _pieces[x][y];
        if ((p != null) && (p.isWhite() == pCouleur) && (p.getType() == KNIGHT))
        {
          return true;
        }
      }
    }

    // Roi
    kLength = KING_MOVES.length;
    for (int i = kLength; --i >= 0; /* Pré-décrémenté */)
    {
      x = xSrc + KING_MOVES[i];
      y = ySrc + KING_MOVES[(i + 2) % kLength];
      if ((x >= 0) && (y >= 0) && (x < FILE_COUNT) && (y < RANK_COUNT))
      {
        p = _pieces[x][y];
        if ((p != null) && (p.isWhite() == pCouleur) && (p.getType() == KING))
        {
          return true;
        }
      }
    }

    // Pions...
    if (pCouleur)
    {
      if (ySrc > 1)
      {
        if (((xSrc > 0) && (_pieces[xSrc - 1][ySrc - 1] == WHITE_PAWN))
            || ((xSrc < FILE_COUNT - 1) && (_pieces[xSrc + 1][ySrc - 1] == WHITE_PAWN)))
        {
          return true;
        }
      }
    }
    else
    {
      if (ySrc < RANK_COUNT - 2)
      {
        if (((xSrc > 0) && (_pieces[xSrc - 1][ySrc + 1] == BLACK_PAWN))
            || ((xSrc < FILE_COUNT - 1) && (_pieces[xSrc + 1][ySrc + 1] == BLACK_PAWN)))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Indique si le roi d'une couleur est en échec.
   * 
   * @param pCouleur Positionné à "true" pour tester l'échec sur les blancs, à "false" sinon.
   * @return Vrai si le roi est en échec.
   */
  public boolean isInCheck(final boolean pCouleur)
  {
    final Square posRoi = getKingSquare(pCouleur);

    for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
    {
      for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
      {
        final Piece p = _pieces[x][y];
        if ((p != null) && (p.isWhite() != pCouleur))
        {
          final Square test = Square.valueOf(x, y);
          if (test != posRoi)
          {
            for (final Square s : getAllTargets(test))
            {
              if (s == posRoi)
              {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }
}
