package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.enums.GameState;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public abstract class AbstractSignLabel {

	private final static String ENABLED_COLOUR = ChatColor.DARK_BLUE.toString();
	private final static String DISABLED_COLOUR = ChatColor.DARK_GRAY.toString();
	private final static String INDICATOR_COLOUR = ChatColor.DARK_RED.toString();
	private static final String NONREACTIVE_COLOUR = "";

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

	public boolean isReactive() {
		return false;
	}

	public boolean gameInState(GameState state) {
		return getGame() != null && getGame().getState() == state;
	}

	public String getLabelColour() {
		if (!isEnabled()) return DISABLED_COLOUR;
		if (!isReactive()) return NONREACTIVE_COLOUR;
		return ENABLED_COLOUR;
	}

	public String getIndicatorColour() {
		if (!isEnabled()) return DISABLED_COLOUR;
		return INDICATOR_COLOUR;
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

	/**
	 * Override this in subclasses if the button/label needs some special
	 * processing to display its text.
	 *
	 * @return
	 */
	protected String[] getCustomSignText() {
		return null;
	}

	public void repaint() {
		Block block = loc.getBlock();

		LogUtils.finer("about to repaint sign: " + block);
		byte data = getSignDirection();
		if (block.getType() != Material.WALL_SIGN || block.getData() != data) {
			MaterialWithData.get("wall_sign:" + data).applyToBlock(block);
		}

		Sign sign = (Sign) block.getState();
		String[] label = getLabel();

		String col = getLabelColour();

		for (int i = 0; i < 4 && i < label.length; ++i) {
			if (label[i].equals("="))
				continue;				// '=' means leave the line as it is
			if (label[i].startsWith("\u00a7")) {
				sign.setLine(i, label[i]);
			} else {
				sign.setLine(i, col + label[i]);
			}
		}
		sign.update();
	}

	private PersistableLocation getSignLocation(int x, int y) {
		BoardRotation boardRot = panel.getView().getRotation();
		BoardRotation signRot = boardRot.getRight();
		Cuboid panelBlocks = panel.getPanelBlocks();

		Cuboid face = panelBlocks.getFace(boardRot.getDirection().opposite());
		int blockX = face.getLowerX() + x * boardRot.getXadjustment() + signRot.getXadjustment();
		int blockY = face.getLowerY() + y;
		int blockZ = face.getLowerZ() + x * boardRot.getZadjustment() + signRot.getZadjustment();

		return new PersistableLocation(panelBlocks.getWorld(), blockX, blockY, blockZ);
	}

	private byte getSignDirection() {
		switch (panel.getView().getRotation().getRight()) {
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
