package me.desht.chesscraft.chess.pieces;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.ChessValidate;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import org.bukkit.configuration.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
	 * @param setName the set name to check
	 * @return true if the set if loaded
	 */
	public static boolean isLoaded(String setName) {
		return allChessSets.containsKey(setName);
	}

	/**
	 * Retrieve a chess set with the given name, loading it from file if necessary.
	 *
	 * @param setName name of the set to get
	 * @return the chess set
	 * @throws ChessException if the set could not be loaded for some reason
	 */
	public static ChessSet getChessSet(String setName) throws ChessException {
		setName = setName.toLowerCase();
		if (!isLoaded(setName) || needsReload(setName)) {
			return loadChessSet(setName);
		} else {
			return allChessSets.get(setName);
		}
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
		if (!f.exists()) {
			throw new ChessException("No such piece style '" + setName + "'");
		}
		try {
			Configuration c = MiscUtil.loadYamlUTF8(f);

			ChessSet set;
			if (!c.contains("type") || c.getString("type").equals("block")) {
				set = new BlockChessSet(c, DirectoryStructure.isCustom(f));
			} else if (c.getString("type").equals("entity")) {
				ChessValidate.isTrue(ChessCraft.getInstance().isCitizensEnabled(),
				                      "Entity chess sets are not available (Citizens 2 plugin must be installed)");
				set = new EntityChessSet(c, DirectoryStructure.isCustom(f));
			} else {
				throw new ChessException("Invalid chess set type '" + c.getString("type") + "' in " + f);
			}
			Debugger.getInstance().debug("loaded chess set '" + set.getName() + "' from " + f);
			if (!set.hasMovablePieces()) {
				// sets with movable pieces can't be cached, since each board will need its own copy of the set
				// (the set will be tracking the position of each piece)
				allChessSets.put(setName, set);
				setLoadTime.put(setName, System.currentTimeMillis());
			}

			return set;
		} catch (Exception e) {
//			e.printStackTrace();
			throw new ChessException("Can't load chess set '" + setName + "': " + e.getMessage());
		}
	}
}
