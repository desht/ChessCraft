package me.desht.chesscraft;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class SignButton {
	private Location location;
	private boolean enabled;
	private String text;
	private MaterialWithData mat;
	private String name;
	
	private final static ChatColor enabledCol  = ChatColor.DARK_BLUE;
	private final static ChatColor disabledCol = ChatColor.DARK_GRAY;
	
	SignButton(String name, Location loc, String text, MaterialWithData mat, boolean enabled) {
		this.name = name;
		this.text = text;
		this.location = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		this.enabled = enabled;
		this.mat = mat;
		
		repaint();
	}

	Location getLocation() {
		return location;
	}

	boolean isEnabled() {
		return enabled;
	}

	String getText() {
		return text;
	}

	String getName() {
		return name;
	}

	static ChatColor getEnabledcol() {
		return enabledCol;
	}

	static ChatColor getDisabledcol() {
		return disabledCol;
	}

	void repaint() {
		Block block = location.getBlock();
		ChessCraft.setBlock(block, mat);
		String[] lines = text.split(";");
		if (block.getState() instanceof Sign) {
			Sign s = (Sign) block.getState();
			for (int i = 0; i < 4 && i < lines.length; i++) {
				if (lines[i].equals("="))
					continue;
				String col = enabled ? enabledCol.toString() : disabledCol.toString();
				s.setLine(i, ChessCraft.parseColourSpec(col + lines[i]));
			}
			s.update();
		}
	}

	void setText(String text) {
		this.text = text;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
