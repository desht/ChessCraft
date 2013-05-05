package me.desht.chesscraft.chess.pieces;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.configuration.Configuration;

/**
 * @author desht
 *
 * Factory class.  Loads chess sets from file, returns a set of the appropriate type.
 */
public class ChessSetFactory {

	// map of all known chess sets keyed by set name
	private static final Map<String, ChessSet> allChessSets = new HashMap<String, ChessSet>();

	// note when the set was loaded so we can reload if the set file is updated
	private static final Map<String,Long> setLoadTime = new HashMap<String, Long>();

	/**
	 * Check if the given set is loaded.
	 * 
	 * @param setName
	 * @return
	 */
	public static boolean isLoaded(String setName) {
		return allChessSets.containsKey(setName);
	}

	/**
	 * Retrieve a chess set with the given name, loading it from file if necessary.
	 * 
	 * @param setName
	 * @return
	 * @throws ChessException
	 */
	public static ChessSet getChessSet(String setName) throws ChessException {
		setName = setName.toLowerCase();
		if (!isLoaded(setName) || needsReload(setName)) {
			loadChessSet(setName);
		}
		return allChessSets.get(setName);
	}

	private static boolean needsReload(String setName) throws ChessException {
		if (!setLoadTime.containsKey(setName)) {
			return true;
		}
		File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getPieceStyleDirectory(), setName);
		return f.lastModified() > setLoadTime.get(setName);
	}

	private static ChessSet loadChessSet(String setName) throws ChessException {
		File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getPieceStyleDirectory(), setName);		
		try {
			Configuration c = MiscUtil.loadYamlUTF8(f);

			BlockChessSet set = new BlockChessSet(c, DirectoryStructure.isCustom(f));
			LogUtils.fine("loaded chess set '" + set.getName() + "' from " + f);

			allChessSets.put(setName, set);
			setLoadTime.put(setName, System.currentTimeMillis());

			return set;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ChessException("can't load chess set from [" + f + "]: " + e.getMessage());
		}
	}
}
