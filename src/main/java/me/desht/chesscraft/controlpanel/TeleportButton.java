package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.chess.BoardView;

import org.bukkit.event.player.PlayerInteractEvent;

public class TeleportButton extends AbstractSignButton {

	public TeleportButton(ControlPanel panel) {
		super(panel, "teleportOutBtn", "teleport", 4, 0);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		if (ChessCraft.getInstance().getConfig().getBoolean("teleporting")) {
			BoardView.teleportOut(event.getPlayer());
		}
	}

	@Override
	public boolean isEnabled() {
		return ChessCraft.getInstance().getConfig().getBoolean("teleporting");
	}

}
