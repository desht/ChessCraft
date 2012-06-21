package me.desht.chesscraft.controlpanel;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.PersistableLocation;

public abstract class AbstractSignButton {
	private final static ChatColor enabledColour = ChatColor.DARK_BLUE;
	private final static ChatColor disabledColour = ChatColor.DARK_GRAY;
	
	private final PersistableLocation loc;
	private final String permissionNode;
	private final ControlPanel panel;
	
	private String[] label;
	
	public AbstractSignButton(ControlPanel panel, String labelKey, String permissionNode, int x, int y) {
		this.label = labelKey == null ? getCustomText() : getTranslation(labelKey);
		this.permissionNode = permissionNode;
		this.panel = panel;
		this.loc = getSignLocation(x, y);
	}
	
	public String[] getLabel() {
		return label;
	}

	public void setLabel(String[] label) {
		this.label = label;
	}

	public ControlPanel getPanel() {
		return panel;
	}

	public BoardView getView() {
		return getPanel().getView();
	}
	
	public ChessGame getGame() {
		return getView().getGame();
	}
	
	public void repaint() {
		Block block = loc.getBlock();
		
		MaterialWithData.get("wall_sign:" + getSignDirection()).applyToBlock(block);
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			boolean isEnabled = isEnabled();
			for (int i = 0; i < 4 && i < label.length; ++i) {
				if (label[i].equals("=")) {
					continue;	// an '=' means leave the line as it is
				}
				String col = isEnabled ? enabledColour.toString() : disabledColour.toString();
				if (label[i].matches("^&[0-9a-f]")) {
					col = "";
				}
				if (!isEnabled) {
					label[i] = label[i].replaceFirst("^&[0-9a-f]", "");
				}
				s.setLine(i, MiscUtil.parseColourSpec(col + label[i]));
			}
			s.update();
		} else {
			LogUtils.warning("sign button isn't a sign! block: " + block);
		}
	}
	
	public void onClicked(PlayerInteractEvent event) {
		if (!isEnabled()) return;
		
		if (!permissionNode.isEmpty()) PermissionUtils.requirePerms(event.getPlayer(), "chesscraft.commands." + permissionNode);
		
		execute(event);
	}
	
	public abstract void execute(PlayerInteractEvent event);
	
	public abstract boolean isEnabled();
	
	protected String[] getCustomText() {
		return null;
	}
	
	protected String[] getTranslation(String labelKey) {
		String label = Messages.getString("ControlPanel." + labelKey);
		
		return label.split(";", 4);
	}

	private byte getSignDirection() {
		switch (panel.getView().getRotation()) {
		case NORTH:
			return 4;
		case EAST:
			return 2;
		case SOUTH:
			return 5;
		case WEST:
			return 3;
		default:
			return 0;
		}
	}
	
	private PersistableLocation getSignLocation(int x, int y) {
		BoardRotation signDir = panel.getView().getRotation();
		Cuboid panelBlocks = panel.getPanelBlocks();
		
		int realX = signDir.getX();
		int realY = panelBlocks.getLowerNE().getBlockY() + y;
		int realZ = signDir.getZ();

		switch(signDir){
		case NORTH:
			realX += panelBlocks.getLowerX();
			realZ += panelBlocks.getLowerZ() + x;
			break;
		case EAST:
			realX += panelBlocks.getUpperX() - x;
			realZ += panelBlocks.getLowerZ();
			break;
		case SOUTH:
			realX += panelBlocks.getLowerX();
			realZ += panelBlocks.getUpperZ() - x;
			break;
		case WEST:
			realX += panelBlocks.getLowerX() + x;
			realZ += panelBlocks.getLowerZ();
			break;
		}
		return new PersistableLocation(panelBlocks.getWorld(), realX, realY, realZ);
	}
}
