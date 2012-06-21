package me.desht.chesscraft.controlpanel;

import org.bukkit.event.player.PlayerInteractEvent;

public class BoardInfoButton extends AbstractSignButton {
	
	public BoardInfoButton(ControlPanel panel) {
		super(panel, "boardInfoBtn", "list.board", 0, 2);
	}
	
	@Override
	public void execute(PlayerInteractEvent event) {
		getPanel().getView().showBoardDetail(event.getPlayer());
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
