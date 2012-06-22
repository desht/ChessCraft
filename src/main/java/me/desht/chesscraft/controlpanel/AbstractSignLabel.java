package me.desht.chesscraft.controlpanel;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;

public abstract class AbstractSignLabel {

	public final static String ENABLED_COLOUR = ChatColor.DARK_BLUE.toString();
	public final static String DISABLED_COLOUR = ChatColor.DARK_GRAY.toString();
	public final static String INDICATOR_COLOUR = ChatColor.DARK_RED.toString();
	
	private final ControlPanel panel;
	private final PersistableLocation loc;
	private final String labelKey;
	
	public AbstractSignLabel(ControlPanel panel, String labelKey, int x, int y) {
		this.labelKey = labelKey;
		this.panel = panel;
		this.loc = getSignLocation(x, y);
	}

	public abstract boolean isEnabled();

	public PersistableLocation getLocation() {
		return loc;
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
	
	public String[] getLabel() {
		String[] text = getCustomSignText();
		if (text != null) return text;
		
		if (labelKey == null) {
			LogUtils.warning("Unknown label key: " + labelKey);
			return new String[] { "???" };
		} else {
			return getSignText();
		}
	}

	protected String[] getSignText() {
		String[] res = new String[] { "", "", "", "" };
		
		String label = Messages.getString("ControlPanel." + labelKey);
		int i = 0;
		for (String line : label.split(";", 4)) {
			res[i++] = line;
		}
		
		return res;
	}
	
	protected String[] getCustomSignText() {
		return null;
	}
	
	public void repaint() {
		Block block = loc.getBlock();
		
		byte data = getSignDirection();
		if (block.getType() != Material.WALL_SIGN || block.getData() != data) {
			MaterialWithData.get("wall_sign:" + data).applyToBlock(block);
		}
		
		if (block.getState() instanceof Sign) {
			Sign sign = (Sign) block.getState();
			boolean isEnabled = isEnabled();
			String[] label = getLabel();
			for (int i = 0; i < 4 && i < label.length; ++i) {
				if (label[i].equals("=")) continue;				// '=' means leave the line as it is
				
				String col = isEnabled ? ENABLED_COLOUR : DISABLED_COLOUR;
				if (label[i].matches("^&[0-9a-f]")) col = "";	// avoid redundant colour codes
				
				if (!isEnabled) label[i] = label[i].replaceFirst("^&[0-9a-f]", "");	// disabled labels are entirely monochromatic
				
				sign.setLine(i, MiscUtil.parseColourSpec(col + label[i]));
			}
			sign.update();
		} else {
			LogUtils.warning("sign button isn't a sign! block: " + block);
		}
	}
	
	protected PersistableLocation getSignLocation(int x, int y) {
		BoardRotation signDir = panel.getView().getRotation();
		Cuboid panelBlocks = panel.getPanelBlocks();
		
		int realX = signDir.getX();
		int realY = panelBlocks.getLowerNE().getBlockY() + y;
		int realZ = signDir.getZ();

		switch (signDir){
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
}
