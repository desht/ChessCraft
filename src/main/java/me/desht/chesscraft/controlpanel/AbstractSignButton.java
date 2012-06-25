package me.desht.chesscraft.controlpanel;

import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.dhutils.PermissionUtils;

public abstract class AbstractSignButton extends AbstractSignLabel {

	private final String permissionNode;
	
	public AbstractSignButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y) {
		super(panel, labelKey, x, y);
		
		this.permissionNode = permissionNode;
	}

	public void onClicked(PlayerInteractEvent event) {
		if (!isEnabled() || !isReactive()) return;
		
		if (permissionNode != null) PermissionUtils.requirePerms(event.getPlayer(), "chesscraft.commands." + permissionNode);
		
		execute(event);
	}
	
	@Override
	public boolean isReactive() {
		return true;
	}
	
	public abstract void execute(PlayerInteractEvent event);
	
}
