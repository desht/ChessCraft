package me.desht.chesscraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import me.desht.chesscraft.exceptions.ChessException;
import me.desht.dhutils.LogUtils;

public class DirectoryStructure {
	private static File pluginDir = new File("plugins", "ChessCraft"); //$NON-NLS-1$ //$NON-NLS-2$
	private static File pgnDir, boardStyleDir, pieceStyleDir, schematicsDir;
	private static File dataDir, gamePersistDir, boardPersistDir, languagesDir, resultsDir;
	private static final String pgnFoldername = "pgn"; //$NON-NLS-1$
	private static final String boardStyleFoldername = "board_styles"; //$NON-NLS-1$
	private static final String pieceStyleFoldername = "piece_styles"; //$NON-NLS-1$
	private static final String schematicsFoldername = "schematics"; //$NON-NLS-1$
	private static final String languageFoldername = "lang"; //$NON-NLS-1$
	private static final String datasaveFoldername = "data"; //$NON-NLS-1$
	private static final String gamesFoldername = "games"; //$NON-NLS-1$
	private static final String boardsFoldername = "boards"; //$NON-NLS-1$
	private static final String resultsFoldername = "results"; //$NON-NLS-1$
	private static File persistFile;
	private static final String persistFilename = "persist.yml"; //$NON-NLS-1$

	public static void setup() {
		pluginDir = ChessCraft.getInstance().getDataFolder();

		setupDirectoryStructure();
		extractResources();
	}

	public static File getPluginDirectory() {
		return pluginDir;
	}

	public static File getPGNDirectory() {
		return pgnDir;
	}

	public static File getBoardStyleDirectory() {
		return boardStyleDir;
	}

	public static File getPieceStyleDirectory() {
		return pieceStyleDir;
	}

	public static File getSchematicsDirectory() {
		return schematicsDir;
	}

	public static File getGamesPersistDirectory() {
		return gamePersistDir;
	}

	public static File getBoardPersistDirectory() {
		return boardPersistDir;
	}

	public static File getLanguagesDirectory() {
		return languagesDir;
	}

	public static File getPersistFile() {
		return persistFile;
	}

	public static File getResultsDir() {
		return resultsDir;
	}

	private static void setupDirectoryStructure() {
		// directories
		pgnDir = new File(pluginDir, pgnFoldername);
		boardStyleDir = new File(pluginDir, boardStyleFoldername);
		pieceStyleDir = new File(pluginDir, pieceStyleFoldername);
		dataDir = new File(pluginDir, datasaveFoldername);
		gamePersistDir = new File(dataDir, gamesFoldername);
		boardPersistDir = new File(dataDir, boardsFoldername);
		schematicsDir = new File(boardPersistDir, schematicsFoldername);
		languagesDir = new File(pluginDir, languageFoldername);
		resultsDir = new File(dataDir, resultsFoldername);

		// files
		persistFile = new File(dataDir, persistFilename);

		// [plugins]/ChessCraft
		createDir(pluginDir);
		// [plugins]/ChessCraft/pgn
		createDir(pgnDir);
		// [plugins]/ChessCraft/lang
		createDir(languagesDir);
		// [plugins]/ChessCraft/board_styles
		createDir(boardStyleDir);
		createDir(new File(boardStyleDir, "custom"));
		// [plugins]/ChessCraft/piece_styles
		createDir(pieceStyleDir);
		createDir(new File(pieceStyleDir, "custom"));
		// [plugins]/ChessCraft/data
		createDir(dataDir);
		// [plugins]/ChessCraft/data/games
		createDir(gamePersistDir);
		// [plugins]/ChessCraft/data/boards
		createDir(boardPersistDir);
		// [plugins]/ChessCraft/data/results
		createDir(resultsDir);

		// saved board schematics may need to be moved into their new location
		File oldSchematicsDir = new File(pluginDir, "schematics"); //$NON-NLS-1$
		if (oldSchematicsDir.isDirectory()) {
			if (!oldSchematicsDir.renameTo(schematicsDir)) {
				LogUtils.warning("Can't move " + oldSchematicsDir + " to " + schematicsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// [plugins]/ChessCraft/data/boards/schematics
			createDir(schematicsDir);
		}
	}

	public static File getJarFile() {
		return new File("plugins", "ChessCraft.jar");
	}

	private static void extractResources() {
		extractResource("/AI_settings.yml", pluginDir); //$NON-NLS-1$
		extractResource("/timecontrols.yml", pluginDir); //$NON-NLS-1$

		extractResource("/datafiles/board_styles/standard.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/open.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/sandwood.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/large.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/small.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/huge.yml", boardStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/board_styles/yazpanda.yml", boardStyleDir); //$NON-NLS-1$

		extractResource("/datafiles/piece_styles/standard.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/twist.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/sandwood.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/large.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/small.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/huge.yml", pieceStyleDir); //$NON-NLS-1$
		extractResource("/datafiles/piece_styles/yazpanda.yml", pieceStyleDir); //$NON-NLS-1$

		// message resources no longer extracted here - this is now done by Messages.loadMessages()
	}

	private static void createDir(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			LogUtils.warning("Can't make directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	static void extractResource(String from, File toDir) {
		extractResource(from, toDir, false);
	}

	static void extractResource(String from, File to, boolean force) {
		File of = to;
		if (to.isDirectory()) {
			String fname = new File(from).getName();
			of = new File(to, fname);
		} else if (!of.isFile()) {
			LogUtils.warning("not a file: " + of);
			return;
		}

		LogUtils.fine("extractResource: file=" + of +
		                      ", file-last-mod=" + of.lastModified() +
		                      ", file-exists=" + of.exists() +
		                      ", jar-last-mod=" +  getJarFile().lastModified() +
		                      ", force=" + force);

		// if the file exists and is newer than the JAR, then we'll leave it alone
		if (of.exists() && of.lastModified() > getJarFile().lastModified() && !force) {
			return;
		}

		LogUtils.fine("extractResource: " + from + " -> " + of);

		OutputStream out = null;
		try {
			// Got to jump through hoops to ensure we can still pull messages from a JAR
			// file after it's been reloaded...
			URL res = ChessCraft.class.getResource(from);
			if (res == null) {
				LogUtils.warning("can't find " + from + " in plugin JAR file"); //$NON-NLS-1$
				return;
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			InputStream in = resConn.getInputStream();

			if (in == null) {
				LogUtils.warning("can't get input stream from " + res); //$NON-NLS-1$
			} else {
				out = new FileOutputStream(of);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (Exception ex) { //IOException
				// ChessCraft.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Find a YAML resource file in the given directory.  Look first in the custom/ subdirectory
	 * and then in the directory itself.
	 * 
	 * @param dir
	 * @param filename
	 * @param saving
	 * @return
	 * @throws ChessException
	 */
	public static File getResourceFileForLoad(File dir, String filename) throws ChessException {
		// try the lower-cased form first, if that fails try the exact filename
		File f = new File(dir, "custom" + File.separator + filename.toLowerCase() + ".yml");
		if (!f.exists()) {
			f = new File(dir, "custom" + File.separator + filename + ".yml");
		}
		if (!f.exists()) {
			f = new File(dir, filename.toLowerCase() + ".yml");
			if (!f.exists()) {
				throw new ChessException("resource file '" + f + "' is not readable");
			}
		}
		return f;
	}

	/** 
	 * Find a YAML resource in the custom/ subdirectory of the given directory.
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 * @throws ChessException
	 */
	public static File getResourceFileForSave(File dir, String filename) throws ChessException {
		File f = new File(dir, "custom" + File.separator + filename.toLowerCase() + ".yml");
		return f;
	}

}
