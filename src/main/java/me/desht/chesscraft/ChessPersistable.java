package me.desht.chesscraft;

import java.io.File;

/**
 * @author des
 * Note lack of serialize() method - that is specified by ConfigurationSerializable
 */
public interface ChessPersistable {
	public String getName();			// for determining save file names
	public File getSaveDirectory();		// directory where save files are placed
}
