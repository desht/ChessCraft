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
import static fr.free.jchecs.core.Piece.BLACK_PAWN;
import static fr.free.jchecs.core.Piece.BLACK_QUEEN;
import static fr.free.jchecs.core.Piece.WHITE_PAWN;
import static fr.free.jchecs.core.Piece.WHITE_QUEEN;
import static fr.free.jchecs.core.PieceType.BISHOP;
import static fr.free.jchecs.core.PieceType.KING;
import static fr.free.jchecs.core.PieceType.KNIGHT;
import static fr.free.jchecs.core.PieceType.PAWN;
import static fr.free.jchecs.core.PieceType.QUEEN;
import static fr.free.jchecs.core.PieceType.ROOK;

/**
 * Représentation d'un état de la partie basée sur un tableau bordé à une dimension pour stocker les
 * positions.
 * <p>
 * Cette représentation est plus performante que la représentation naturelle avec un tableau à deux
 * dimensions, tout en restant moins complexe que les BitBoards.
 * </p>
 * 
 * @author David Cotton
 */
final class MailboxBoard extends AbstractMoveGenerator
{
  /** Identifiant de la classe pour la sérialisation. */
  private static final long serialVersionUID = -756989397935622605L;

  /** Mailbox pour tester les déplacements. */
  private static final int [] MAILBOX =
      { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1,
        2, 3, 4, 5, 6, 7, -1, -1, 8, 9, 10, 11, 12, 13, 14, 15, -1, -1, 16, 17, 18, 19, 20, 21, 22,
        23, -1, -1, 24, 25, 26, 27, 28, 29, 30, 31, -1, -1, 32, 33, 34, 35, 36, 37, 38, 39, -1, -1,
        40, 41, 42, 43, 44, 45, 46, 47, -1, -1, 48, 49, 50, 51, 52, 53, 54, 55, -1, -1, 56, 57, 58,
        59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, };

  /** Equivalences entre le plateau et la mailbox. */
  private static final int [] TO_MAILBOX =
      { 21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 33, 34, 35, 36, 37, 38, 41, 42, 43, 44, 45, 46, 47,
        48, 51, 52, 53, 54, 55, 56, 57, 58, 61, 62, 63, 64, 65, 66, 67, 68, 71, 72, 73, 74, 75, 76,
        77, 78, 81, 82, 83, 84, 85, 86, 87, 88, 91, 92, 93, 94, 95, 96, 97, 98, };

  /** Liste des modificateurs pour les mouvements d'un roi. */
  private static final int [] KING_MOVES = { -11, -10, -9, -1, 1, 9, 10, 11 };

  /** Liste des modificateurs pour les mouvements d'un cavalier dans la mailbox. */
  private static final int [] KNIGHT_MOVES = { -21, -19, -12, -8, 8, 12, 19, 21, };

  /** Liste de cases cibles vides. */
  private static final Square [] NO_SQUARE = new Square [ 0 ];

  /**
   * Buffer de travail pour optimiser la recherche des cibles de mouvements.
   * <p>
   * L'utilisation d'un buffer statique optimise les performances mais nécessite de faire très
   * attention à la synchronisation pour que la classe reste sûre vis-à-vis des threads.
   * </p>
   */
  // 27 est le nombre maximum de cases cibles pour une pièce (une dame, dans le meilleur des cas).
  private static final int [] SQUARES_BUFFER = new int [ 27 ];

  /** Indice du dernier élément valide dans les buffer de travail des cases. */
  private static int S_nbBufferedSquares;

  /** Description du plateau. */
  private final Piece [] _pieces = new Piece [ FILE_COUNT * RANK_COUNT ];

  /** Clé de hachage. */
  private int _hashCode;

  /**
   * Crée une nouvelle instance, initialisée à partir de l'état reçu en paramètre.
   * 
   * @param pEtat Instance initial.
   */
  MailboxBoard(final Board pEtat)
  {
    super(pEtat);

    for (final Square s : Square.values())
    {
      _pieces[s.getIndex()] = pEtat.getPieceAt(s);
    }
    _hashCode = super.hashCode();
  }

  /**
   * Crée une nouvelle instance, copie conforme de l'instance reçue.
   * 
   * @param pEtat Instance à copier.
   */
  private MailboxBoard(final MailboxBoard pEtat)
  {
    super(pEtat);

    System.arraycopy(pEtat._pieces, 0, _pieces, 0, FILE_COUNT * RANK_COUNT);
    _hashCode = pEtat._hashCode;
  }

  /**
   * Ajoute au buffer interne toutes les cases cibles des mouvements possibles (y compris ceux
   * mettant le roi en échec) pour la pièce contenue par une case.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   */
  private void addAllTargets(final int pOrigine)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final Piece piece = _pieces[pOrigine];
    if (piece != null)
    {
      final boolean trait = piece.isWhite();
      switch (piece.getType())
      {
        case BISHOP :
          addBishopTargets(pOrigine, trait);
          break;
        case KING :
          addKingTargets(pOrigine, trait);
          break;
        case KNIGHT :
          addKnightTargets(pOrigine, trait);
          break;
        case PAWN :
          addPawnTargets(pOrigine, trait);
          break;
        case QUEEN :
          addBishopTargets(pOrigine, trait);
          addRookTargets(pOrigine, trait);
          break;
        case ROOK :
          addRookTargets(pOrigine, trait);
          break;
        default :
          assert false;
      }
    }
  }

  /**
   * Ajoute au buffer interne toutes les cases cibles possibles d'un mouvement de type "fou" d'une
   * certaine couleur (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   * @param pBlanc Positionné à vrai si la recherche concerne les blancs.
   */
  private void addBishopTargets(final int pOrigine, final boolean pBlanc)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final int mbSrc = TO_MAILBOX[pOrigine];

    // Mouvements / prise vers le haut/gauche...
    int mbDst = mbSrc + 9;
    int dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst += 9;
      dst = MAILBOX[mbDst];
    }

    // Mouvements / prise vers le haut/droit...
    mbDst = mbSrc + 11;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst += 11;
      dst = MAILBOX[mbDst];
    }

    // Mouvements / prise vers le bas/gauche...
    mbDst = mbSrc - 11;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst -= 11;
      dst = MAILBOX[mbDst];
    }

    // Mouvements / prise vers le bas/droit...
    mbDst = mbSrc - 9;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst -= 9;
      dst = MAILBOX[mbDst];
    }
  }

  /**
   * Ajoute au buffer interne la liste des cases pouvant être atteintes par un mouvement de type
   * roi.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   */
  private void addKingTargets(final int pOrigine, final boolean pBlanc)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final int mbSrc = TO_MAILBOX[pOrigine];
    boolean testerRoque = false;
    for (final int km : KING_MOVES)
    {
      final int dst = MAILBOX[mbSrc + km];
      if (dst >= 0)
      {
        final Piece p = _pieces[dst];
        if ((p == null) || (p.isWhite() != pBlanc))
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
          testerRoque = true;
        }
      }
    }
    if (testerRoque && (Square.valueOf(pOrigine).getFile() == 4))
    {
      final int dst = MAILBOX[mbSrc];
      if (canCastleShort(pBlanc) && (_pieces[dst + 1] == null) && (_pieces[dst + 2] == null))
      {
        final Piece t = _pieces[dst + 3];
        if ((t != null) && (t.getType() == ROOK) && (t.isWhite() == pBlanc))
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst + 2;
        }
      }
      if (canCastleLong(pBlanc) && (_pieces[dst - 1] == null) && (_pieces[dst - 2] == null)
          && (_pieces[dst - 3] == null))
      {
        final Piece t = _pieces[dst - 4];
        if ((t != null) && (t.getType() == ROOK) && (t.isWhite() == pBlanc))
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst - 2;
        }
      }
    }
  }

  /**
   * Ajoute au buffer interne la liste des cases pouvant être atteintes par un mouvement de type
   * cavalier.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   */
  private void addKnightTargets(final int pOrigine, final boolean pBlanc)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final int mbSrc = TO_MAILBOX[pOrigine];
    for (final int km : KNIGHT_MOVES)
    {
      final int dst = MAILBOX[mbSrc + km];
      if (dst >= 0)
      {
        final Piece p = _pieces[dst];
        if ((p == null) || (p.isWhite() != pBlanc))
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
      }
    }
  }

  /**
   * Ajoute au buffer interne la liste des cases pouvant être atteintes par un mouvement de type
   * pion.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   * @param pBlanc A vrai pour indiquer une recherche sur les blancs.
   */
  private void addPawnTargets(final int pOrigine, final boolean pBlanc)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final Square cSrc = Square.valueOf(pOrigine);
    final int ySrc = cSrc.getRank();
    if (pBlanc)
    {
      if (ySrc < RANK_COUNT - 1)
      {
        // Mouvement de 1...
        if (_pieces[pOrigine + FILE_COUNT] == null)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = pOrigine + FILE_COUNT;
          // Mouvement initial de 2
          if ((ySrc == 1) && (_pieces[pOrigine + FILE_COUNT * 2] == null))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = pOrigine + FILE_COUNT * 2;
          }
        }
        final int xSrc = cSrc.getFile();
        if (xSrc > 0)
        {
          // Prise à gauche (y compris en passant)...
          final int iDest = pOrigine - 1 + FILE_COUNT;
          final Piece pDest = _pieces[iDest];
          if (((pDest != null) && (!pDest.isWhite())) || (Square.valueOf(iDest) == getEnPassant()))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = iDest;
          }
        }
        if (xSrc < FILE_COUNT - 1)
        {
          // Prise à droite (y compris en passant)...
          final int iDest = pOrigine + 1 + FILE_COUNT;
          final Piece pDest = _pieces[iDest];
          if (((pDest != null) && (!pDest.isWhite())) || (Square.valueOf(iDest) == getEnPassant()))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = iDest;
          }
        }
      }
    }
    else
    {
      if (ySrc > 0)
      {
        // Mouvement de 1...
        if (_pieces[pOrigine - FILE_COUNT] == null)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = pOrigine - FILE_COUNT;
          // Mouvement initial de 2
          if ((ySrc == RANK_COUNT - 2) && (_pieces[pOrigine - FILE_COUNT * 2] == null))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = pOrigine - FILE_COUNT * 2;
          }
        }
        final int xSrc = cSrc.getFile();
        if (xSrc > 0)
        {
          // Prise à gauche (y compris en passant)...
          final int iDest = pOrigine - 1 - FILE_COUNT;
          final Piece pDest = _pieces[iDest];
          if (((pDest != null) && pDest.isWhite()) || (Square.valueOf(iDest) == getEnPassant()))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = iDest;
          }
        }
        if (xSrc < FILE_COUNT - 1)
        {
          // Prise à droite (y compris en passant)...
          final int iDest = pOrigine + 1 - FILE_COUNT;
          final Piece pDest = _pieces[iDest];
          if (((pDest != null) && pDest.isWhite()) || (Square.valueOf(iDest) == getEnPassant()))
          {
            SQUARES_BUFFER[S_nbBufferedSquares++] = iDest;
          }
        }
      }
    }
  }

  /**
   * Ajoute au buffer interne toutes les cases cibles possibles d'un mouvement de type "tour" d'une
   * certaine couleur (y compris ceux mettant le roi en échec) à partir d'une case.
   * 
   * @param pOrigine Indice de la case à l'origine du mouvement.
   * @param pBlanc Mis à vrai pour rechercher pour les blancs.
   */
  private void addRookTargets(final int pOrigine, final boolean pBlanc)
  {
    assert (pOrigine >= 0) && (pOrigine < FILE_COUNT * RANK_COUNT);

    final int mbSrc = TO_MAILBOX[pOrigine];

    // Mouvements / prise vers la gauche...
    int mbDst = mbSrc - 1;
    int dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      dst = MAILBOX[--mbDst];
    }

    // Mouvements / prise vers la droite...
    mbDst = mbSrc + 1;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      dst = MAILBOX[++mbDst];
    }

    // Mouvements / prise vers le haut...
    mbDst = mbSrc + 10;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst += 10;
      dst = MAILBOX[mbDst];
    }

    // Mouvements / prise vers le bas...
    mbDst = mbSrc - 10;
    dst = MAILBOX[mbDst];
    while (dst >= 0)
    {
      final Piece p = _pieces[dst];
      if (p == null)
      {
        SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
      }
      else
      {
        if (p.isWhite() != pBlanc)
        {
          SQUARES_BUFFER[S_nbBufferedSquares++] = dst;
        }
        break;
      }
      mbDst -= 10;
      dst = MAILBOX[mbDst];
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

    return new MailboxBoard(pEtat);
  }

  /**
   * Renvoi une nouvelle instance décrivant l'état du jeu après application d'un mouvement.
   * 
   * @param pMouvement Description de mouvement.
   * @param pSuivant Drapeau positionné si l'on souhaite que le trait soit modifié.
   * @return Instance dérivée.
   */
  public MoveGenerator derive(final Move pMouvement, final boolean pSuivant)
  {
    assert pMouvement != null;

    final MailboxBoard res = new MailboxBoard(this);

    // Ajuste les compteurs...
    if (pSuivant)
    {
      final boolean t = !isWhiteActive();
      res.setWhiteActive(t);
      res._hashCode ^= ZOBRIST_WHITE_ACTIVE;
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
    final int iSrc = src.getIndex();
    final int xSrc = src.getFile();
    assert res._pieces[iSrc] == piece;
    res._pieces[iSrc] = null;
    final int pieceOrdinal = piece.ordinal();
    res._hashCode ^= ZOBRIST_PIECE_POSITION[pieceOrdinal][iSrc];
    final Square dst = pMouvement.getTo();
    final int iDst = dst.getIndex();
    final int xDst = dst.getFile();
    final int yDst = dst.getRank();
    final Piece pieceDst = _pieces[iDst];
    if (pieceDst != null)
    {
      res._hashCode ^= ZOBRIST_PIECE_POSITION[pieceDst.ordinal()][iDst];
    }
    res._pieces[iDst] = piece;
    res._hashCode ^= ZOBRIST_PIECE_POSITION[pieceOrdinal][iDst];
    // ... éxécute un mouvement spécifique de type "roque" et gère le suivi des rois ...
    if (typePiece == KING)
    {
      res.setKingSquare(trait, dst);
      if (xSrc == 4)
      {
        if (xDst == 2)
        {
          // ... côté reine...
          final int i = yDst * FILE_COUNT;
          final Piece tour = res._pieces[i];
          assert tour != null;
          assert tour.getType() == ROOK;
          res._pieces[i] = null;
          final int tourOrdinal = tour.ordinal();
          res._hashCode ^= ZOBRIST_PIECE_POSITION[tourOrdinal][i];
          res._pieces[i + 3] = tour;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[tourOrdinal][i + 3];
          res.setCastled(trait, true);
        }
        else if (xDst == 6)
        {
          // ... côté roi...
          final int i = FILE_COUNT - 1 + yDst * FILE_COUNT;
          final Piece tour = res._pieces[i];
          assert tour != null;
          assert tour.getType() == ROOK;
          res._pieces[i] = null;
          final int tourOrdinal = tour.ordinal();
          res._hashCode ^= ZOBRIST_PIECE_POSITION[tourOrdinal][i];
          res._pieces[i - 2] = tour;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[tourOrdinal][i - 2];
          res.setCastled(trait, true);
        }
      }
    }
    // Tient compte des interdictions de roquer que le mouvement peut provoquer...
    if (canCastleShort(trait))
    {
      if ((typePiece == KING) || ((typePiece == ROOK) && (xSrc == FILE_COUNT - 1)))
      {
        res.setCastleShort(trait, false);
        if (trait)
        {
          res._hashCode ^= ZOBRIST_WHITE_CASTLE_SHORT;
        }
        else
        {
          res._hashCode ^= ZOBRIST_BLACK_CASTLE_SHORT;
        }
      }
    }
    if (canCastleLong(trait))
    {
      if ((typePiece == KING) || ((typePiece == ROOK) && (xSrc == 0)))
      {
        res.setCastleLong(trait, false);
        if (trait)
        {
          res._hashCode ^= ZOBRIST_WHITE_CASTLE_LONG;
        }
        else
        {
          res._hashCode ^= ZOBRIST_BLACK_CASTLE_LONG;
        }
      }
    }
    // Détecte si une prise "en passant" doit être effectuée ou signalée et gère la promotion...
    final Square epOrig = getEnPassant();
    res.setEnPassant(null);
    if (typePiece == PAWN)
    {
      final int ySrc = src.getRank();
      // En profite pour aussi gérer le compteur de demis coups...
      if (pSuivant)
      {
        res.setHalfmoveCount(0);
      }
      if (trait)
      {
        assert yDst > ySrc;
        if (yDst == RANK_COUNT - 1)
        {
          res._pieces[iDst] = WHITE_QUEEN;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[pieceOrdinal][iDst];
          res._hashCode ^= ZOBRIST_PIECE_POSITION[WHITE_QUEEN.ordinal()][iDst];
        }
        else if ((ySrc == 1) && (yDst == 3))
        {
          res.setEnPassant(Square.valueOf(xDst, 2));
        }
        else if (dst == epOrig)
        {
          final int epDst = iDst - FILE_COUNT;
          res._pieces[epDst] = null;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[_pieces[epDst].ordinal()][epDst];
        }
      }
      else
      {
        assert yDst < ySrc;
        if (yDst == 0)
        {
          res._pieces[iDst] = BLACK_QUEEN;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[pieceOrdinal][iDst];
          res._hashCode ^= ZOBRIST_PIECE_POSITION[BLACK_QUEEN.ordinal()][iDst];
        }
        else if ((ySrc == RANK_COUNT - 2) && (yDst == RANK_COUNT - 4))
        {
          res.setEnPassant(Square.valueOf(xDst, RANK_COUNT - 3));
        }
        else if (dst == epOrig)
        {
          final int epDst = iDst + FILE_COUNT;
          res._pieces[epDst] = null;
          res._hashCode ^= ZOBRIST_PIECE_POSITION[_pieces[epDst].ordinal()][epDst];
        }
      }
    }

    final Square epFinal = res.getEnPassant();
    if ((epOrig != null) && ((epFinal == null) || (!epOrig.equals(epFinal))))
    {
      res._hashCode ^= ZOBRIST_EN_PASSANT[epOrig.getFile()];
    }
    if ((epFinal != null) && ((epOrig == null) || (!epFinal.equals(epOrig))))
    {
      res._hashCode ^= ZOBRIST_EN_PASSANT[epFinal.getFile()];
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

    if (pObjet instanceof MailboxBoard)
    {
      if (hashCode() != pObjet.hashCode())
      {
        return false;
      }

      final MailboxBoard o = (MailboxBoard) pObjet;

      for (int i = 0; i < RANK_COUNT * FILE_COUNT; i++)
      {
        if (_pieces[i] != o._pieces[i])
        {
          return false;
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

    final int idx = pOrigine.getIndex();
    final Piece piece = _pieces[idx];
    if (piece != null)
    {
      final Square [] res;

      synchronized (SQUARES_BUFFER)
      {
        S_nbBufferedSquares = 0;

        addAllTargets(idx);

        res = new Square [ S_nbBufferedSquares ];
        for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
        {
          res[t] = Square.valueOf(SQUARES_BUFFER[t]);
        }
      }

      return res;
    }

    return NO_SQUARE;
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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      addBishopTargets(pOrigine.getIndex(), pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      addKingTargets(pOrigine.getIndex(), pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      addKnightTargets(pOrigine.getIndex(), pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      addPawnTargets(pOrigine.getIndex(), pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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

    return _pieces[pCase.getIndex()];
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

    return _pieces[pColonne + pLigne * FILE_COUNT];
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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      final int idx = pOrigine.getIndex();
      addBishopTargets(idx, pBlanc);
      addRookTargets(idx, pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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

    final Square [] res;

    synchronized (SQUARES_BUFFER)
    {
      S_nbBufferedSquares = 0;

      addRookTargets(pOrigine.getIndex(), pBlanc);

      res = new Square [ S_nbBufferedSquares ];
      for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
      {
        res[t] = Square.valueOf(SQUARES_BUFFER[t]);
      }
    }

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
    Move [] tmp = new Move [ 45 ];
    int nb = 0;
    int lTmp = tmp.length;
    for (int i = FILE_COUNT * RANK_COUNT; --i >= 0; /* Pré-décrémenté */)
    {
      final Piece p = _pieces[i];
      if ((p != null) && (p.isWhite() == pTrait))
      {
        final Square orig = Square.valueOf(i);
        for (final Square dst : getValidTargets(orig))
        {
          final Piece prise;
          if ((p.getType() != PAWN) || (dst != getEnPassant()))
          {
            prise = _pieces[dst.getIndex()];
          }
          else
          {
            if (pTrait)
            {
              prise = _pieces[dst.getIndex() - FILE_COUNT];
            }
            else
            {
              prise = _pieces[dst.getIndex() + FILE_COUNT];
            }
          }
          tmp[nb++] = new Move(p, orig, dst, prise);
          if (nb >= lTmp)
          {
            final Move [] extension = new Move [ lTmp + 15 ];
            System.arraycopy(tmp, 0, extension, 0, lTmp);
            tmp = extension;
            lTmp = tmp.length;
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

    final int iSrc = pOrigine.getIndex();
    final Piece piece = _pieces[iSrc];
    if (piece != null)
    {
      synchronized (SQUARES_BUFFER)
      {
        S_nbBufferedSquares = 0;
        addAllTargets(iSrc);
        int nbFinal = S_nbBufferedSquares;
        final boolean trait = piece.isWhite();
        for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
        {
          final int idxCible = SQUARES_BUFFER[t];
          final Square cible = Square.valueOf(idxCible);
          final Piece prise = _pieces[idxCible];
          if (derive(new Move(piece, pOrigine, cible, prise), false).isInCheck(trait))
          {
            SQUARES_BUFFER[t] = -1;
            nbFinal--;
          }
          else if ((piece.getType() == KING) && (pOrigine.getFile() == 4))
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
                SQUARES_BUFFER[t] = -1;
                nbFinal--;
              }
            }
          }
        }
        assert (nbFinal >= 0) && (nbFinal <= S_nbBufferedSquares);

        if (nbFinal == 0)
        {
          return NO_SQUARE;
        }

        final Square [] res = new Square [ nbFinal ];
        for (int t = S_nbBufferedSquares; --t >= 0; /* Pré-décrémenté */)
        {
          final int idx = SQUARES_BUFFER[t];
          if (idx >= 0)
          {
            res[--nbFinal] = Square.valueOf(idx);
          }
        }

        return res;
      }
    }

    return NO_SQUARE;
  }

  /**
   * Surcharge du calcul des clés de hachage, pour optimisation.
   * 
   * @return Clé de hachage.
   */
  @Override
  public int hashCode()
  {
    assert _hashCode == super.hashCode();
    return _hashCode;
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

    final int mbSrc = TO_MAILBOX[pCible.getIndex()];

    Piece p = null;
    int mbDst = mbSrc - 1;
    int dst = MAILBOX[mbDst];
    // Gauche
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      dst = MAILBOX[--mbDst];
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
    mbDst = mbSrc + 1;
    dst = MAILBOX[mbDst];
    // Droite
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      dst = MAILBOX[++mbDst];
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
    mbDst = mbSrc - 10;
    dst = MAILBOX[mbDst];
    // Bas
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst -= 10;
      dst = MAILBOX[mbDst];
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
    mbDst = mbSrc + 10;
    dst = MAILBOX[mbDst];
    // Haut
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst += 10;
      dst = MAILBOX[mbDst];
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
    mbDst = mbSrc - 11;
    dst = MAILBOX[mbDst];
    // Bas / Gauche
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst -= 11;
      dst = MAILBOX[mbDst];
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
    mbDst = mbSrc + 9;
    dst = MAILBOX[mbDst];
    // Haut / Gauche
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst += 9;
      dst = MAILBOX[mbDst];
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
    mbDst = mbSrc + 11;
    dst = MAILBOX[mbDst];
    // Haut / Droit
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst += 11;
      dst = MAILBOX[mbDst];
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
    mbDst = mbSrc - 9;
    dst = MAILBOX[mbDst];
    // Bas / Droit
    while ((dst >= 0) && (p == null))
    {
      p = _pieces[dst];
      mbDst -= 9;
      dst = MAILBOX[mbDst];
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
    for (final int km : KNIGHT_MOVES)
    {
      dst = MAILBOX[mbSrc + km];
      if (dst >= 0)
      {
        p = _pieces[dst];
        if ((p != null) && (p.isWhite() == pCouleur) && (p.getType() == KNIGHT))
        {
          return true;
        }
      }
    }

    // Roi
    for (final int km : KING_MOVES)
    {
      dst = MAILBOX[mbSrc + km];
      if (dst >= 0)
      {
        p = _pieces[dst];
        if ((p != null) && (p.isWhite() == pCouleur) && (p.getType() == KING))
        {
          return true;
        }
      }
    }

    // Pions...
    if (pCouleur)
    {
      final int ySrc = pCible.getRank();
      if (ySrc > 1)
      {
        final int xSrc = pCible.getFile();
        if (((xSrc > 0) && (_pieces[MAILBOX[mbSrc - 11]] == WHITE_PAWN))
            || ((xSrc < FILE_COUNT - 1) && (_pieces[MAILBOX[mbSrc - 9]] == WHITE_PAWN)))
        {
          return true;
        }
      }
    }
    else
    {
      final int ySrc = pCible.getRank();
      if (ySrc < RANK_COUNT - 2)
      {
        final int xSrc = pCible.getFile();
        if (((xSrc > 0) && (_pieces[MAILBOX[mbSrc + 9]] == BLACK_PAWN))
            || ((xSrc < FILE_COUNT - 1) && (_pieces[MAILBOX[mbSrc + 11]] == BLACK_PAWN)))
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
    return isAttacked(getKingSquare(pCouleur), !pCouleur);
  }
}
