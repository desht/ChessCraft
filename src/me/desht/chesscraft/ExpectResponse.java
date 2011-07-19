package me.desht.chesscraft;

import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.exceptions.ChessException;

import org.bukkit.entity.Player;

class ExpectResponse {
	enum ExpectAction {
		BoardCreation, DrawResponse, SwapResponse
	}

	private final Map<String, ExpectData> exp = new HashMap<String, ExpectData>();

	ExpectResponse() {
	}

	void expectingResponse(Player p, ExpectAction action, ExpectData data) {
		expectingResponse(p, action, data, null);
	}

	void expectingResponse(Player p, ExpectAction action, ExpectData data, String expectee) {
		if (expectee != null) {
			exp.put(genKey(expectee, action), data);
		} else {
			exp.put(genKey(p, action), data);
		}
		data.setAction(action);
	}

	boolean isExpecting(Player p, ExpectAction action) {
		return exp.containsKey(genKey(p, action));
	}

	void handleAction(Player p, ExpectAction action) throws ChessException {
		exp.get(genKey(p, action)).doResponse(p);
		cancelAction(p, action);
	}

	void cancelAction(Player p, ExpectAction action) {
		exp.remove(genKey(p, action));
	}

	ExpectData getAction(Player p, ExpectAction action) {
		return exp.get(genKey(p, action));
	}

	private String genKey(Player p, ExpectAction action) {
		return p.getName() + ":" + action.toString();
	}

	private String genKey(String name, ExpectAction action) {
		return name + ":" + action.toString();
	}
}
