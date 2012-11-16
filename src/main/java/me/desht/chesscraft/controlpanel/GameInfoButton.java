package me.desht.chesscraft.controlpanel;

import java.util.List;

import me.desht.chesscraft.chess.ChessGame;
import me.desht.dhutils.MessagePager;

import org.bukkit.event.player.PlayerInteractEvent;

public class GameInfoButton extends AbstractSignButton {

	public GameInfoButton(ControlPanel panel) {
		super(panel, "gameInfoBtn", "list.game", 0, 1);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		ChessGame game = getGame();
		if (game != null) {
			MessagePager pager = MessagePager.getPager(event.getPlayer()).clear();
			List<String> l = game.getGameDetail();
			pager.add(l);
			pager.showPage();
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

}
