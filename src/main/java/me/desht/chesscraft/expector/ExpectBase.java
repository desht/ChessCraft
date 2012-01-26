package me.desht.chesscraft.expector;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public abstract class ExpectBase {

	public abstract void doResponse(Player p) throws ChessException;
}
