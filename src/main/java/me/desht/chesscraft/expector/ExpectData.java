package me.desht.chesscraft.expector;

import me.desht.chesscraft.enums.ExpectAction;
import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

public abstract class ExpectData {

	ExpectAction action;

	public ExpectData() {
	}

	public void doResponse(Player p) throws ChessException {
	}

	public void setAction(ExpectAction action) {
		this.action = action;
	}

	public ExpectAction getAction() {
		return action;
	}
}
