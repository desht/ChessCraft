package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.enums.BoardRotation;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public abstract class ChessSet implements Comparable<ChessSet>{
	private final String name;
	private final String comment;
	private final boolean isCustom;
	private int maxWidth;
	private int maxHeight;
	
	public ChessSet(Configuration c, boolean isCustom) {
		ChessPersistence.requireSection(c, "name");
		name = c.getString("name");
		comment = c.getString("comment", "");
		this.isCustom = isCustom;
	}
	
	protected ChessSet(String name, String comment) {
		this.name = name;
		this.comment = comment;
		this.isCustom = true;
	}

	public abstract ChessStone getStone(int stone, BoardRotation direction);
	
	public abstract void save(String newName);
	
	protected abstract String getHeaderText();
	
	protected abstract String getType();

	/**
	 * Get this chess set's name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the comment for this chess set.
	 * 
	 * @return
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Get the width (X or Z) of the widest piece in the set
	 *
	 * @return
	 */
	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Get the height of the tallest piece in the set
	 * 
	 * @return
	 */
	public int getMaxHeight() {
		return maxHeight;
	}

	public boolean isCustom() {
		return isCustom;
	}
	
	protected void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}
	
	protected void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	protected YamlConfiguration getYamlConfig() {
		YamlConfiguration conf = new YamlConfiguration();
		
		conf.set("name", name);
		conf.set("comment", comment);
		conf.options().header(getHeaderText());
		
		return conf;
	}

	@Override
	public int compareTo(ChessSet o) {
		return getName().compareTo(o.getName());
	}
}
