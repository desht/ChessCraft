package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.io.IOException;

import me.desht.chesscraft.ChessPersistence;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.chess.ChessBoard;
import me.desht.chesscraft.enums.BoardRotation;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import chesspresso.position.Position;

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

	protected abstract String getHeaderText();
	protected abstract String getType();

	public abstract ChessStone getStone(int stone, BoardRotation direction);
	public abstract ChessStone getStoneAt(int sqi);
	public abstract boolean canRide();
	public abstract boolean hasMovablePieces();
	public abstract void movePiece(int fromSqi, int toSqi, int captureSqi, Location to, int promoteStone);
	public abstract void syncToPosition(Position pos, ChessBoard board);

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

	protected abstract void addSaveData(Configuration c);

	/**
	 * Save this chess set to a file with the new name.
	 *
	 * @param newName name of the new chess set
	 * @throws ChessException if there is a problem writing the file
	 */
	public void save(String newName) {
		File f = DirectoryStructure.getResourceFileForSave(DirectoryStructure.getPieceStyleDirectory(), ChessPersistence.makeSafeFileName(newName));

		YamlConfiguration conf = new YamlConfiguration();

		conf.options().header(getHeaderText());
		conf.set("name", name);
		conf.set("comment", comment);
		conf.set("type", getType());

		addSaveData(conf);

		try {
			conf.save(f);
			LogUtils.fine("saved " + getType() + " chess set '" + getName() + "' to " + f);
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}

	protected YamlConfiguration getYamlConfig() {
		YamlConfiguration conf = new YamlConfiguration();

		conf.options().header(getHeaderText());
		conf.set("name", name);
		conf.set("comment", comment);
		conf.set("type", getType());

		return conf;
	}

	@Override
	public int compareTo(ChessSet o) {
		return getName().compareTo(o.getName());
	}

}
