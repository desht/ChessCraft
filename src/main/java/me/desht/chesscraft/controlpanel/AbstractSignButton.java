package me.desht.chesscraft.controlpanel;

import me.desht.dhutils.PermissionUtils;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class AbstractSignButton extends AbstractSignLabel {

	private final String permissionNode;

	public AbstractSignButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y) {
		super(panel, labelKey, x, y);

		this.permissionNode = permissionNode;
	}

	/**
	 * Called when the sign is clicked by the player.  Any ChessException thrown by this method (and the abstract execute()
	 * method that it calls) will ultimately be caught and reported to the player by the PlayerInteractEvent event handler.
	 *
	 * @param event	The player interaction event as caught by the plugin's event handler
	 */
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
