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

import java.util.Random;

/**
 * Squelette de l'implémentation d'une classe représentant un état de la partie.
 * 
 * @author David Cotton
 */
abstract class AbstractBoard implements Board
{
  /** Constantes de pièce / position pour le calcul de clés de hachage "Zobrist". */
  protected static final int [][] ZOBRIST_PIECE_POSITION;

  /** Constante de prise en passant pour le calcul de clés de hachage "Zobrist". */
  protected static final int [] ZOBRIST_EN_PASSANT;

  /** Constante de petit roque pour les noirs pour le calcul de clés de hachage "Zobrist". */
  protected static final int ZOBRIST_BLACK_CASTLE_LONG;

  /** Constante de grand roque pour les noirs pour le calcul de clés de hachage "Zobrist". */
  protected static final int ZOBRIST_BLACK_CASTLE_SHORT;

  /** Constante de trait aux blancs pour le calcul de clés de hachage "Zobrist". */
  protected static final int ZOBRIST_WHITE_ACTIVE;

  /** Constante de petit roque pour les blancs pour le calcul de clés de hachage "Zobrist". */
  protected static final int ZOBRIST_WHITE_CASTLE_LONG;

  /** Constante de grand roque pour les blancs pour le calcul de clés de hachage "Zobrist". */
  protected static final int ZOBRIST_WHITE_CASTLE_SHORT;

  static
  {
    final Random rnd = new Random(123456789L);
    final int nbPieces = Piece.values().length;
    ZOBRIST_PIECE_POSITION = new int [ nbPieces ] [ FILE_COUNT * RANK_COUNT ];
    for (int i = nbPieces; --i >= 0; /* Pré-décrémenté */)
    {
      for (int j = FILE_COUNT * RANK_COUNT; --j >= 0; /* Pré-décrémenté */)
      {
        ZOBRIST_PIECE_POSITION[i][j] = rnd.nextInt();
      }
    }
    ZOBRIST_EN_PASSANT = new int [ FILE_COUNT ];
    for (int i = FILE_COUNT; --i >= 0; /* Pré-décrémenté */)
    {
      ZOBRIST_EN_PASSANT[i] = rnd.nextInt();
    }
    ZOBRIST_BLACK_CASTLE_LONG = rnd.nextInt();
    ZOBRIST_BLACK_CASTLE_SHORT = rnd.nextInt();
    ZOBRIST_WHITE_ACTIVE = rnd.nextInt();
    ZOBRIST_WHITE_CASTLE_LONG = rnd.nextInt();
    ZOBRIST_WHITE_CASTLE_SHORT = rnd.nextInt();
  }

  /** Drapeau indiquant le droit de roquer côté roi (petit roque) pour les noirs. */
  private boolean _blackCastleShort = true;

  /** Drapeau indiquant le droit de roquer côté reine (grand roque) pour les noirs. */
  private boolean _blackCastleLong = true;

  /** Case cible de l'éventuelle 'une prise "en passant" disponible (peut être à null). */
  private Square _enPassant;

  /** Numéro du coup. */
  private int _fullmoveNumber = 1;

  /** Compteur des demi-coups depuis la dernière prise ou le dernier mouvement de pion. */
  private int _halfmoveCount;

  /** Drapeau positionné à vrai si le trait est aux blancs. */
  private boolean _whiteActive = true;

  /** Drapeau indiquant le droit de roquer côté roi (petit roque) pour les blancs. */
  private boolean _whiteCastleShort = true;

  /** Drapeau indiquant le droit de roquer côté reine (grand roque) pour les blancs. */
  private boolean _whiteCastleLong = true;

  /**
   * Crée une nouvelle instance.
   */
  protected AbstractBoard()
  {
    // Rien de spécifique...
  }

  /**
   * Crée une nouvelle instance, initialisée à partir de l'état reçu.
   * 
   * @param pEtat Instance initiale.
   */
  protected AbstractBoard(final Board pEtat)
  {
    assert pEtat != null;

    _blackCastleShort = pEtat.canCastleShort(false);
    _blackCastleLong = pEtat.canCastleLong(false);
    _enPassant = pEtat.getEnPassant();
    _fullmoveNumber = pEtat.getFullmoveNumber();
    _halfmoveCount = pEtat.getHalfmoveCount();
    _whiteActive = pEtat.isWhiteActive();
    _whiteCastleShort = pEtat.canCastleShort(true);
    _whiteCastleLong = pEtat.canCastleLong(true);
  }

  /**
   * Crée une nouvelle instance, copiée à partir de l'instance reçue.
   * 
   * @param pEtat Instance à copier.
   */
  protected AbstractBoard(final AbstractBoard pEtat)
  {
    assert pEtat != null;

    _blackCastleShort = pEtat._blackCastleShort;
    _blackCastleLong = pEtat._blackCastleLong;
    _enPassant = pEtat._enPassant;
    _fullmoveNumber = pEtat._fullmoveNumber;
    _halfmoveCount = pEtat._halfmoveCount;
    _whiteActive = pEtat._whiteActive;
    _whiteCastleShort = pEtat._whiteCastleShort;
    _whiteCastleLong = pEtat._whiteCastleLong;
  }

  /**
   * Renvoi l'état du droit de roquer côté roi (petit roque) pour une couleur.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return Etat du droit de roquer côté roi pour la couleur.
   */
  public final boolean canCastleLong(final boolean pBlanc)
  {
    if (pBlanc)
    {
      return _whiteCastleLong;
    }

    return _blackCastleLong;
  }

  /**
   * Renvoi l'état du droit de roquer côté reine (grand roque) pour une couleur.
   * 
   * @param pBlanc Positionné à "true" pour obtenir l'état des blancs.
   * @return Etat du droit de roquer côté reine pour la couleur.
   */
  public final boolean canCastleShort(final boolean pBlanc)
  {
    if (pBlanc)
    {
      return _whiteCastleShort;
    }

    return _blackCastleShort;
  }

  /**
   * Fonction de comparaison entre échiquiers, pour permettre un tri.
   * 
   * @param pEchiquier Echiquier avec lequel comparer.
   * @return -1 si l'échiquier est plus "petit", 0 s'ils sont égaux, 1 s'il est plus "grand".
   * @see Comparable#compareTo(Object)
   */
  public final int compareTo(final Board pEchiquier)
  {
    int res = 0;
    for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
    {
      for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
      {
        final Piece p1 = getPieceAt(x, y);
        final Piece p2 = pEchiquier.getPieceAt(x, y);
        if ((p1 != null) || (p2 != null))
        {
          if (p1 == null)
          {
            return -1;
          }
          else if (p2 == null)
          {
            return 1;
          }
          else
          {
            res = p1.compareTo(p2);
            if (res != 0)
            {
              return res;
            }
          }
        }
      }
    }

    final Square ep = pEchiquier.getEnPassant();
    if ((_enPassant != null) || (ep != null))
    {
      if (_enPassant == null)
      {
        return -1;
      }
      else if (ep == null)
      {
        return 1;
      }
      else
      {
        res = _enPassant.compareTo(ep);
      }
    }
    if (res == 0)
    {
      res = Boolean.valueOf(_whiteActive).compareTo(Boolean.valueOf(pEchiquier.isWhiteActive()));
    }
    if (res == 0)
    {
      res =
          Boolean.valueOf(_blackCastleLong).compareTo(
              Boolean.valueOf(pEchiquier.canCastleLong(false)));
    }
    if (res == 0)
    {
      res =
          Boolean.valueOf(_blackCastleShort).compareTo(
              Boolean.valueOf(pEchiquier.canCastleShort(false)));
    }
    if (res == 0)
    {
      res =
          Boolean.valueOf(_whiteCastleLong).compareTo(
              Boolean.valueOf(pEchiquier.canCastleLong(true)));
    }
    if (res == 0)
    {
      res =
          Boolean.valueOf(_whiteCastleShort).compareTo(
              Boolean.valueOf(pEchiquier.canCastleShort(true)));
    }

    return res;
  }

  /**
   * Teste l'égalité entre deux descriptions de partie.
   * <p>
   * Attention : le nombre de demi-coups depuis la dernière prise ainsi que le numéro du tour ne
   * sont pas pris en compte dans la comparaison.
   * </p>
   * <p>
   * Pour des raisons de performance, il est préférable que les implémentations concrètes de cette
   * classe prévoient une version spécialisée du test d'égalité. De plus, pour conserver
   * l'interopérabilité, les implémentations doivent rester compatibles sur ce test.
   * </p>
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

    if (!(pObjet instanceof Board))
    {
      return false;
    }

    if (hashCode() != pObjet.hashCode())
    {
      return false;
    }

    final Board o = (Board) pObjet;

    for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
    {
      for (int x = FILE_COUNT; --x >= 0; /* Pré-décrémenté */)
      {
        if (getPieceAt(x, y) != o.getPieceAt(x, y))
        {
          return false;
        }
      }
    }

    return equalsInternal(o);
  }

  /**
   * Teste l'égalité sur les données internes de cette classe.
   * 
   * @param pEtat Etat avec lequel comparer.
   * @return Vrai si les données internes sont égales.
   */
  protected final boolean equalsInternal(final Board pEtat)
  {
    assert pEtat != null;

    boolean res = true;

    if (_whiteActive != pEtat.isWhiteActive())
    {
      res = false;
    }
    else if (_enPassant != pEtat.getEnPassant())
    {
      res = false;
    }
    else if (_blackCastleLong != pEtat.canCastleLong(false))
    {
      res = false;
    }
    else if (_blackCastleShort != pEtat.canCastleShort(false))
    {
      res = false;
    }
    else if (_whiteCastleLong != pEtat.canCastleLong(true))
    {
      res = false;
    }
    else if (_whiteCastleShort != pEtat.canCastleShort(true))
    {
      res = false;
    }

    return res;
  }

  /**
   * Renvoi l'éventuelle case cible de la prise "en passant" en cours.
   * 
   * @return Case cible de la price "en passant" (peut être à null).
   */
  public final Square getEnPassant()
  {
    return _enPassant;
  }

  /**
   * Renvoi le numéro du coup.
   * 
   * @return Numéro de coup (> 0).
   */
  public final int getFullmoveNumber()
  {
    assert _fullmoveNumber > 0;
    return _fullmoveNumber;
  }

  /**
   * Renvoi la valeur du compteur de demi-coups depuis la dernière prise ou le dernier mouvement de
   * pion.
   * 
   * @return Nombre de demi-coups (>= 0).
   */
  public final int getHalfmoveCount()
  {
    assert _halfmoveCount >= 0;
    return _halfmoveCount;
  }

  /**
   * Implémentation par défaut du calcul des clés de hachage, suivant la méthode "Zobrist".
   * <p>
   * Les implémentations concrètes devraient surcharger cette méthode pour des raisons de
   * performance. Attention : les résultats produits par les différentes implémentations doivent
   * rester compatibles si l'on souhaite l'interopérabilité.
   * </p>
   * 
   * @return Clé de hachage.
   */
  @Override
  public int hashCode()
  {
    int res = zobristRoot();
    for (final Square s : Square.values())
    {
      final Piece p = getPieceAt(s);
      if (p != null)
      {
        res ^= ZOBRIST_PIECE_POSITION[p.ordinal()][s.getIndex()];
      }
    }

    return res;
  }

  /**
   * Indique si le trait est aux blancs.
   * 
   * @return "true" si le trait est aux blancs, "false" s'il est aux noirs.
   */
  public final boolean isWhiteActive()
  {
    return _whiteActive;
  }

  /**
   * Alimente l'état du droit de roquer côté roi (petit roque) pour une couleur.
   * <p>
   * Potentiellement dangeureux : attention à ne pas casser le contrat qui veut que les
   * implémentations se comportent comme des objets immuables.
   * </p>
   * 
   * @param pBlanc Positionné à "true" pour alimenter l'état des blancs.
   * @param pEtat Etat du droit de roquer côté roi pour la couleur.
   */
  protected void setCastleLong(final boolean pBlanc, final boolean pEtat)
  {
    if (pBlanc)
    {
      _whiteCastleLong = pEtat;
    }
    else
    {
      _blackCastleLong = pEtat;
    }
  }

  /**
   * Alimente l'état du droit de roquer côté reine (grand roque) pour une couleur.
   * <p>
   * Potentiellement dangeureux : attention à ne pas casser le contrat qui veut que les
   * implémentations se comportent comme des objets immuables.
   * </p>
   * 
   * @param pBlanc Positionné à "true" pour alimenter l'état des blancs.
   * @param pEtat Etat du droit de roquer côté reine pour la couleur.
   */
  protected void setCastleShort(final boolean pBlanc, final boolean pEtat)
  {
    if (pBlanc)
    {
      _whiteCastleShort = pEtat;
    }
    else
    {
      _blackCastleShort = pEtat;
    }
  }

  /**
   * Alimente l'éventuelle case cible de la prise "en passant".
   * <p>
   * Potentiellement dangeureux : attention à ne pas casser le contrat qui veut que les
   * implémentations se comportent comme des objets immuables.
   * </p>
   * 
   * @param pCase Case cible de la price "en passant" (peut être à null).
   */
  protected void setEnPassant(final Square pCase)
  {
    _enPassant = pCase;
  }

  /**
   * Alimente le numéro du coup.
   * <p>
   * Potentiellement dangeureux : attention à ne pas casser le contrat qui veut que les
   * implémentations se comportent comme des objets immuables.
   * </p>
   * 
   * @param pNumero Numéro de coup (> 0).
   */
  protected void setFullmoveNumber(final int pNumero)
  {
    assert pNumero > 0;
    _fullmoveNumber = pNumero;
  }

  /**
   * Alimente la valeur du compteur de demi-coups depuis la dernière prise ou le dernier mouvement
   * de pion.
   * <p>
   * Potentiellement dangeureux : attention à ne pas casser le contrat qui veut que les
   * implémentations se comportent comme des objets immuables.
   * </p>
   * 
   * @param pNombre Nombre de demi-coups (>= 0).
   */
  protected void setHalfmoveCount(final int pNombre)
  {
    assert pNombre >= 0;
    _halfmoveCount = pNombre;
  }

  /**
   * Alimente le drapeau du trait.
   * 
   * @param pTrait "true" si le trait va aux blancs, "false" s'il va aux noirs.
   */
  protected void setWhiteActive(final boolean pTrait)
  {
    _whiteActive = pTrait;
  }

  /**
   * Renvoi une chaine représentant le plateau.
   * 
   * @return Chaine représentant le plateau.
   */
  @Override
  public final String toString()
  {
    final StringBuilder res = new StringBuilder();

    for (int y = RANK_COUNT; --y >= 0; /* Pré-décrémenté */)
    {
      for (int x = 0; x < FILE_COUNT; x++)
      {
        final Piece p = getPieceAt(x, y);
        if (p == null)
        {
          res.append('+');
        }
        else
        {
          res.append(p.getFENLetter());
        }
      }
      res.append('\n');
    }

    return res.toString();
  }

  /**
   * Calcule le début de la clé de hachage "Zobrist".
   * 
   * @return Partie de la clé correspondant aux données internes de cette classe.
   */
  protected final int zobristRoot()
  {
    int res = 0;

    if (_blackCastleLong)
    {
      res ^= ZOBRIST_BLACK_CASTLE_LONG;
    }
    if (_blackCastleShort)
    {
      res ^= ZOBRIST_BLACK_CASTLE_SHORT;
    }
    if (_enPassant != null)
    {
      res ^= ZOBRIST_EN_PASSANT[_enPassant.getFile()];
    }
    if (_whiteActive)
    {
      res ^= ZOBRIST_WHITE_ACTIVE;
    }
    if (_whiteCastleLong)
    {
      res ^= ZOBRIST_WHITE_CASTLE_LONG;
    }
    if (_whiteCastleShort)
    {
      res ^= ZOBRIST_WHITE_CASTLE_SHORT;
    }

    return res;
  }
}
