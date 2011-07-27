package me.desht.chesscraft;

import me.desht.chesscraft.regions.Cuboid;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

import chesspresso.Chess;
import chesspresso.position.PositionListener;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.blocks.ChessStone;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.HighlightStyle;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;

public class BoardView implements PositionListener {

    private static final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
    private static final String styleDir = ChessConfig.getDirectory() + File.separator + "board_styles";
    private ChessCraft plugin;
    private String name;
    private Game game;
    private Location a1Square;
    private Location origin;
    // if highlight_last_move, what squares are highlighted
    private int fromSquare = -1, toSquare = -1;
    private int frameWidth;
    private int squareSize;
    private int height;
    private MaterialWithData blackSquareMat;
    private MaterialWithData whiteSquareMat;
    private MaterialWithData frameMat;
    private MaterialWithData controlPanelMat;
    private MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
    private HighlightStyle highlightStyle;
    private MaterialWithData enclosureMat;
    private String boardStyle, pieceStyle;
    private Boolean isLit;
    private Map<Integer, ChessStone> stones;
    private byte lastLevel;
    private ControlPanel controlPanel;

    public BoardView(String bName, ChessCraft plugin, Location where, String bStyle, String pStyle) throws ChessException {
        _init(bName, plugin, where, bStyle, pStyle, false);
    }
    
    public BoardView(String bName, ChessCraft plugin, Location where, String bStyle, String pStyle, boolean onlyTesting) throws ChessException {
        _init(bName, plugin, where, bStyle, pStyle, onlyTesting);
    }

    private void _init(String bName, ChessCraft plugin, Location where, String bStyle, String pStyle, boolean onlyTesting) throws ChessException  {
    	this.plugin = plugin;
        boardStyle = bStyle;
        pieceStyle = pStyle;

        name = bName;
        if (BoardView.checkBoardView(name))
        	throw new ChessException("A board with this name already exists.");
        
        game = null; // indicates board not used by any game yet
        if (boardStyle == null) {
            boardStyle = "Standard";
        }
        loadStyle(boardStyle);
        origin = where;
        a1Square = calcBaseSquare(where);
        if (!onlyTesting)
        	validateIntersections();
        stones = createStones(pieceStyle);
        validateBoardParams();
        lastLevel = -1;
        
        if (!onlyTesting) { 
        	controlPanel = new ControlPanel(plugin, this);
        }
    }
    
    /**
     * Overall sanity checking on board/set parameters
     * @throws ChessException if anything about the board & pieces are bad
     */
    private void validateBoardParams() throws ChessException {
        if (squareSize < 2) {
            throw new ChessException("Board's square size is too small (minimum 2)!");
        }
        if (height < 3) {
            throw new ChessException("Board does not have enough vertical space (minimum 3)!");
        }
        if (frameWidth < 2) {
            throw new ChessException("Frame width is too narrow (minimum 2)");
        }
        if (a1Square.getBlockY() + height >= 127) {
            throw new ChessException("Board altitude is too high - roof would be above top of world");
        }

        int maxH = -1, maxV = -1;
        for (Entry<Integer, ChessStone> entry : stones.entrySet()) {
            maxH = Math.max(maxH, entry.getValue().getSizeX());
            maxH = Math.max(maxH, entry.getValue().getSizeZ());
            maxV = Math.max(maxV, entry.getValue().getSizeY());
        }
        if (maxH > squareSize) {
            throw new ChessException("Set '" + pieceStyle + "' is too wide for this board!");
        }
        if (maxV > height) {
            throw new ChessException("Set '" + pieceStyle + "' is too tall for this board!");
        }
    }

    /**
     * Ensure this board doesn't intersect any other boards
     * @throws ChessException if an intersection would occur
     */
    private void validateIntersections() throws ChessException {
        Cuboid bounds = getBounds();
        bounds.outset(Direction.Horizontal, getFrameWidth() - 1);
        bounds.expand(Direction.Up, getHeight() + 1);

        for (BoardView bv : BoardView.listBoardViews()) {
            if (bv.getA1Square().getWorld() != getA1Square().getWorld()) {
                continue;
            }
            for (Location l : bounds.corners()) {
                if (bv.getOuterBounds().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                    throw new ChessException("Board would intersect existing board " + bv.getName());
                }
            }
        }
    }

    public Map<String, Object> freeze() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("name", name);
        result.put("game", game == null ? "" : game.getName());
        result.put("pieceStyle", pieceStyle);
        result.put("boardStyle", boardStyle);
        result.put("origin", ChessPersistence.makeBlockList(origin));

        return result;
    }

    public String getName() {
        return name;
    }

    public String getBoardStyle() {
        return boardStyle;
    }

    public String getPieceStyle() {
        return pieceStyle;
    }

    public Game getGame() {
        return game;
    }

    public Location getA1Square() {
        return a1Square;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getSquareSize() {
        return squareSize;
    }

    public int getHeight() {
        return height;
    }

    public Boolean getIsLit() {
        return isLit;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public MaterialWithData getBlackSquareMat() {
        return blackSquareMat;
    }

    public MaterialWithData getWhiteSquareMat() {
        return whiteSquareMat;
    }

    public MaterialWithData getFrameMat() {
        return frameMat;
    }

    public MaterialWithData getControlPanelMat() {
        return controlPanelMat == null ? frameMat : controlPanelMat;
    }

    public MaterialWithData getEnclosureMat() {
        return enclosureMat;
    }

    @SuppressWarnings("unchecked")
    public final void loadStyle(String style) throws ChessException {
        Yaml yaml = new Yaml();

        File f = new File(styleDir, style + ".yml");
        try {
            Map<String, Object> styleMap = (Map<String, Object>) yaml.load(new FileInputStream(f));

            squareSize = (Integer) styleMap.get("square_size");
            frameWidth = (Integer) styleMap.get("frame_width");
            height = (Integer) styleMap.get("height");
            isLit = (Boolean) styleMap.get("lit");
            if (pieceStyle == null) {
                pieceStyle = (String) styleMap.get("piece_style");
            }

            blackSquareMat = new MaterialWithData((String) styleMap.get("black_square"));
            whiteSquareMat = new MaterialWithData((String) styleMap.get("white_square"));
            frameMat = new MaterialWithData((String) styleMap.get("frame"));
            if (styleMap.get("panel") != null) {
                controlPanelMat = new MaterialWithData((String) styleMap.get("panel"));
            }
            if (styleMap.get("highlight") != null) {
                highlightMat = new MaterialWithData((String) styleMap.get("highlight"));
            } else {
                highlightMat = new MaterialWithData(89);
            }

            if (styleMap.get("highlight_white_square") != null) {
                highlightWhiteSquareMat =
                        new MaterialWithData((String) styleMap.get("highlight_white_square"));
            } else {
                highlightWhiteSquareMat = null;
            }

            if (styleMap.get("highlight_black_square") != null) {
                highlightBlackSquareMat =
                        new MaterialWithData((String) styleMap.get("highlight_black_square"));
            } else {
                highlightBlackSquareMat = null;
            }

            if (styleMap.get("highlight_style") != null) {
                String hs = (String) styleMap.get("highlight_style");
                try {
                    highlightStyle = HighlightStyle.getStyle(hs);
                } catch (IllegalArgumentException e) {
                    ChessCraft.log(Level.WARNING, "unknown highlight_style definition '" + hs + "' when loading " + getName());
                    highlightStyle = HighlightStyle.CORNERS;
                }
            } else {
                highlightStyle = HighlightStyle.CORNERS;
            }

            enclosureMat = new MaterialWithData((String) styleMap.get("enclosure"));
        } catch (Exception e) {
            //e.printStackTrace();
            ChessCraft.log(Level.SEVERE, "can't load board style " + style, e);
            throw new ChessException("Board style '" + style + "' is not available.");
        }
    }

    /**
     * Given a board origin (the block at the center of the A1 square),
     * calculate the southwest corner of the A1 square <br>
     * (which is also the southwest corner of the whole board)
     * @param where the center of the square to be A1
     * @return southwest corner of the square
     */
    private Location calcBaseSquare(Location where) {
        int xOff = squareSize / 2;
        int zOff = squareSize / 2;
        return new Location(where.getWorld(), where.getBlockX() + xOff, where.getBlockY(), where.getBlockZ() + zOff);
    }

    private Map<Integer, ChessStone> createStones(String pieceStyle) throws ChessException {
        if (!plugin.library.isChessSetLoaded(pieceStyle)) {
            plugin.library.loadChessSet(pieceStyle);
        }
        Map<Integer, ChessStone> result = new HashMap<Integer, ChessStone>();

        for (int stone = Chess.MIN_STONE; stone <= Chess.MAX_STONE; ++stone) {
            if (stone != Chess.NO_STONE) {
                result.put(stone, plugin.library.getStone(pieceStyle, stone));
            }
        }
        return result;
    }

    /**
     * paint whole board
     * (board, frame, enclosure, control panel, lighting)
     */
    public void paintAll() {
        wipe();
        paintEnclosure();
        paintBoard();
        paintFrame();
        controlPanel.repaint();
        if (fromSquare >= 0 || toSquare >= 0) {
            highlightSquares(fromSquare, toSquare);
        } else {
            doLighting(true); // force a lighting update
        }
    }

    private void paintEnclosure() {
        Cuboid bounds = getBounds();
        int fw = frameWidth - 1;
        int x1 = bounds.getLowerNE().getBlockX() - fw;
        int z1 = bounds.getLowerNE().getBlockZ() - fw;
        int x2 = bounds.getUpperSW().getBlockX() + fw;
        int z2 = bounds.getUpperSW().getBlockZ() + fw;
        // (x1,z1) & (x2,z2) are now the outermost corners of the frame
        int y1 = a1Square.getBlockY() + 1;
        int y2 = a1Square.getBlockY() + 1 + height;
        if (y2 > 127) {
            y2 = 127;
        }
        World w = a1Square.getWorld();

        Cuboid walls[] = {new Cuboid(new Location(w, x1, y1, z2), new Location(w, x2, y2, z2)), // west
            new Cuboid(new Location(w, x1, y1, z1), new Location(w, x2, y2, z1)), // east
            new Cuboid(new Location(w, x1, y1, z1), new Location(w, x1, y2, z2)), // north
            new Cuboid(new Location(w, x2, y1, z1), new Location(w, x2, y2, z2)), // south
            new Cuboid(new Location(w, x1, y2, z1), new Location(w, x2, y2, z2)), // roof
        };
        for (Cuboid wall : walls) {
            for (Location l : wall) {
                enclosureMat.applyToBlock(w.getBlockAt(l));
            }
        }
    }

    private void paintFrame() {
        Cuboid bounds = getBounds();

        World w = a1Square.getWorld();
        int y = a1Square.getBlockY();
        int fw = frameWidth - 1;
        int x1 = bounds.getLowerNE().getBlockX();
        int z1 = bounds.getLowerNE().getBlockZ();
        int x2 = bounds.getUpperSW().getBlockX();
        int z2 = bounds.getUpperSW().getBlockZ();

        Cuboid[] frameParts = {new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x2 + fw, y, z1)), // east
            new Cuboid(new Location(w, x1 - fw, y, z2), new Location(w, x2 + fw, y, z2 + fw)), // west
            new Cuboid(new Location(w, x1 - fw, y, z1 - fw), new Location(w, x1, y, z2 + fw)), // north
            new Cuboid(new Location(w, x2, y, z1 - fw), new Location(w, x2 + fw, y, z2 + fw)), // south
        };
        for (Cuboid part : frameParts) {
            for (Location l : part) {
                frameMat.applyToBlock(w.getBlockAt(l));
            }
        }
    }

    private void paintBoard() {
        for (int i = 0; i < Chess.NUM_OF_SQUARES; ++i) {
            paintSquareAt(i);
            int stone = game != null ? game.getPosition().getStone(i) : Chess.NO_STONE;
            paintStoneAt(i, stone);
        }
    }

    private void paintStoneAt(int sqi, int stone) {
        int col = Chess.sqiToCol(sqi);
        int row = Chess.sqiToRow(sqi);
        Location l = rowColToWorld(row, col, 0, 0);

        World w = a1Square.getWorld();
        if (stone == Chess.NO_STONE) {

            // first remove blocks that might pop off & leave a drop
            Block b = null;
            for (int x = 0; x < squareSize; ++x) {
                for (int y = 1; y <= height; ++y) {
                    for (int z = 0; z < squareSize; ++z) {
                        b = w.getBlockAt(l.getBlockX() - x, l.getBlockY() + y, l.getBlockZ() - z);
                        if (BlockType.shouldPlaceLast(b.getTypeId())) {
                            b.setTypeId(0);
                        }
                    }
                }
            }
            for (int x = 0; x < squareSize; ++x) {
                for (int y = 1; y <= height; ++y) {
                    for (int z = 0; z < squareSize; ++z) {
                        w.getBlockAt(l.getBlockX() - x, l.getBlockY() + y, l.getBlockZ() - z).setTypeId(0);
                    }
                }
            }
        } else {
            MaterialWithData air = new MaterialWithData(0, (byte) 0);
            ChessStone cStone = stones.get(stone);
            int xOff = (squareSize - cStone.getSizeX()) / 2;
            int zOff = (squareSize - cStone.getSizeZ()) / 2;
            boolean secondPassNeeded = false;
            for (int x = 0; x < cStone.getSizeX(); ++x) {
                for (int y = 0; y < height; ++y) {
                    for (int z = 0; z < cStone.getSizeZ(); ++z) {
                        MaterialWithData mat = y >= cStone.getSizeY() ? air : cStone.getMaterial(x, y, z);
                        if (!BlockType.shouldPlaceLast(mat.getMaterial())) {
                            mat.applyToBlock(w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1,
                                    (l.getBlockZ() - zOff) - z));
                        } else {
                            secondPassNeeded = true;
                        }
                    }
                }
            }
            if (secondPassNeeded) {
                for (int x = 0; x < cStone.getSizeX(); ++x) {
                    for (int y = 0; y < height; ++y) {
                        for (int z = 0; z < cStone.getSizeZ(); ++z) {
                            MaterialWithData mat = y >= cStone.getSizeY() ? air : cStone.getMaterial(x, y, z);
                            if (BlockType.shouldPlaceLast(mat.getMaterial())) {
                                mat.applyToBlock(w.getBlockAt((l.getBlockX() - xOff) - x, l.getBlockY() + y + 1,
                                        (l.getBlockZ() - zOff) - z));
                            }
                        }
                    }
                }
            }
        }
    }

    private void paintSquareAt(int sqi) {
        paintSquareAt(sqi, false);
    }

    private void paintSquareAt(int sqi, boolean highlight) {
        int col = Chess.sqiToCol(sqi);
        int row = Chess.sqiToRow(sqi);
        if (!(row >= 0 && row < 8 && col >= 0 && col < 8)) {
            return;
        }
        Location locNE = rowColToWorldNE(row, col);
        MaterialWithData m = new MaterialWithData(Chess.isWhiteSquare(sqi) ? whiteSquareMat : blackSquareMat);
        Cuboid square = new Cuboid(locNE, locNE);
        square.expand(Direction.South, squareSize - 1);
        square.expand(Direction.West, squareSize - 1);

        for (Location loc : square) {
            m.applyToBlock(loc.getBlock());
        }
        if (highlight) {
            switch (highlightStyle) {
                case EDGES:
                    for (Location loc : square.walls()) {
                        m = Chess.isWhiteSquare(sqi) ? highlightWhiteSquareMat : highlightBlackSquareMat;
                        (m == null ? highlightMat : m).applyToBlock(loc.getBlock());
                    }
                    break;
                case CORNERS:
                    for (Location loc : square.corners()) {
                        m = Chess.isWhiteSquare(sqi) ? highlightWhiteSquareMat : highlightBlackSquareMat;
                        (m == null ? highlightMat : m).applyToBlock(loc.getBlock());
                    }
                    break;
                case CHECKERED:
                case CHEQUERED:
                    for (Location loc : square) {
                        if ((loc.getBlockX() - loc.getBlockZ()) % 2 == 0) {
                            highlightMat.applyToBlock(loc.getBlock());
                        }
                    }
                    break;
            }
        }
    }

    public void doLighting() {
        doLighting(false);
    }

    public void doLighting(boolean force) {
        if (!isLit) {
            return;
        }

//        byte level = getOuterBounds().getUpperSW().getBlock().getLightLevel();
//        byte level = getBounds().shift(Direction.Up, height/2)
//                .getUpperSW().getBlock().getLightLevel();
//        Player jas = plugin.getServer().getPlayer("jascotty2");
//        if(jas!=null && getName().contains("cave")){
//            Cuboid c = getBounds().shift(Direction.Up, 2).
//                inset(Direction.Horizontal, frameWidth + squareSize * 3)
//                .expand(Direction.Up, height / 2);
//            com.sk89q.worldedit.bukkit.selections.CuboidSelection s =
//                    new com.sk89q.worldedit.bukkit.selections.CuboidSelection(
//                    c.getUpperSW().getWorld(), c.getUpperSW(), c.getLowerNE());
//            plugin.getWorldEdit().setSelection(jas, s);
//        }
        byte level = getBounds().shift(Direction.Up, 2).
                inset(Direction.Horizontal, frameWidth + squareSize * 3).
                expand(Direction.Up, height / 2).
                averageLightLevel();

        if (!force && isBright(level) == isBright(lastLevel) && lastLevel >= 0) {
            return;
        }
        lastLevel = level;

        if (isBright(level)) {
            for (int sqi = 0; sqi < Chess.NUM_OF_SQUARES; ++sqi) {
                int col = Chess.sqiToCol(sqi);
                int row = Chess.sqiToRow(sqi);
                Location locNE = rowColToWorldNE(row, col);
                if (locNE.getBlock().getTypeId() == 89) {
                    (Chess.isWhiteSquare(sqi) ? whiteSquareMat : blackSquareMat).applyToBlock(locNE.getBlock());
                }
            }
            setFrameLights(frameMat);
        } else {
            for (int sqi = 0; sqi < Chess.NUM_OF_SQUARES; ++sqi) {
                // todo: this code could be better optimized
                int col = Chess.sqiToCol(sqi);
                int row = Chess.sqiToRow(sqi);
                Location locNE = rowColToWorldNE(row, col);
                locNE.getBlock().setTypeId(89);
            }
            setFrameLights(new MaterialWithData(89, (byte) -1));
        }
    }

    private void setFrameLights(MaterialWithData mat) {
        Location l = getBounds().getLowerNE();
        l.add(squareSize / 2 + 1, 0, 0);
        int boardSize = squareSize * 8 + 1;
        // east & west sides
        for (int i = 0; i < 8; ++i) {
            mat.applyToBlock(l.getBlock());
            l.add(0, 0, boardSize);
            mat.applyToBlock(l.getBlock());
            l.add(squareSize, 0, -boardSize);
        }
        // north & south sides
        l = getBounds().getLowerNE();
        l.add(0, 0, squareSize / 2 + 1);
        for (int i = 0; i < 8; ++i) {
            mat.applyToBlock(l.getBlock());
            l.add(boardSize, 0, 0);
            mat.applyToBlock(l.getBlock());
            l.add(-boardSize, 0, squareSize);
        }
    }

    private boolean isBright(byte level) {
        if (level < 12) {
            return false;
        } else {
            return true;
        }
    }

    public void highlightSquares(int from, int to) {
        if (highlightStyle == HighlightStyle.NONE) {
            return;
        }

        if (fromSquare >= 0 || toSquare >= 0) {
            if (highlightStyle == HighlightStyle.LINE) {
                drawHighlightLine(fromSquare, toSquare, false);
            } else {
                paintSquareAt(fromSquare);
                paintSquareAt(toSquare);
            }
        }
        fromSquare = from;
        toSquare = to;
        
        doLighting(true);

        if (highlightStyle == HighlightStyle.LINE) {
            drawHighlightLine(fromSquare, toSquare, true);
        } else {
            paintSquareAt(fromSquare, true);
            paintSquareAt(toSquare, true);
        }
    }

    /**
     * Use Bresenham's algorithm to draw line between two squares on the board
     * 
     * @param from	Square index of the first square
     * @param to	Square index of the second square
     * @param isHighlighting	True if drawing a highlight, false if erasing it
     */
    private void drawHighlightLine(int from, int to, boolean isHighlighting) {
        Location loc1 = rowColToWorldCenter(Chess.sqiToRow(from), Chess.sqiToCol(from));
        Location loc2 = rowColToWorldCenter(Chess.sqiToRow(to), Chess.sqiToCol(to));

        int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
        int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
        int sx = loc1.getBlockX() < loc2.getBlockX() ? 1 : -1;
        int sz = loc1.getBlockZ() < loc2.getBlockZ() ? 1 : -1;
        int err = dx - dz;

        while (loc1.getBlockX() != loc2.getBlockX() || loc1.getBlockZ() != loc2.getBlockZ()) {
            int sqi = getSquareAt(loc1);
            MaterialWithData m = isHighlighting ? highlightMat : (Chess.isWhiteSquare(sqi) ? whiteSquareMat : blackSquareMat);
            m.applyToBlock(loc1.getBlock());
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                loc1.add(sx, 0, 0);
            }
            if (e2 < dx) {
                err += dx;
                loc1.add(0, 0, sz);
            }
        }
    }

    /**
     * get the bounds of the board itself
     * @return the bounds of the chess board - the innermost ring of the frame
     */
    public Cuboid getBounds() {
        Location a1 = rowColToWorldSW(0, 0);
        Location h8 = rowColToWorldNE(7, 7);

        int x1 = h8.getBlockX(), z2 = h8.getBlockZ();
        int x2 = a1.getBlockX(), z1 = a1.getBlockZ();

        World w = a1Square.getWorld();
        int y = a1Square.getBlockY();
        return new Cuboid(new Location(w, x1, y, z1), new Location(w, x2, y, z2)).outset(Direction.Horizontal, 1);
    }

    public Cuboid getOuterBounds() {
        Cuboid res = getBounds();
        res.outset(Direction.Horizontal, getFrameWidth() - 1);
        res.expand(Direction.Up, getHeight() + 1);
        return res;
    }

    /**
     * given a Chess row & col, get the location in world coords
     * of that square's NE block (smallest X & Z)
     * @param row
     * @param col
     * @return
     */
    public Location rowColToWorldNE(int row, int col) {
        return rowColToWorld(row, col, squareSize - 1, squareSize - 1);
    }

    /**
     * given a Chess row & col, get the location in world coords
     * of that square's SW block (largest X & Z)
     * @param row
     * @param col
     * @return
     */
    public Location rowColToWorldSW(int row, int col) {
        return rowColToWorld(row, col, 0, 0);
    }

    public Location rowColToWorldCenter(int row, int col) {
        return rowColToWorld(row, col, squareSize / 2, squareSize / 2);
    }

    public Location rowColToWorld(int row, int col, int xOff, int zOff) {
        Location a1 = a1Square;
        xOff += row * squareSize;
        zOff += col * squareSize;
        return new Location(a1.getWorld(), a1.getX() - xOff, a1.getY(), a1.getZ() - zOff);
    }

    @Override
    public void castlesChanged(int castles) {
        // TODO Auto-generated method stub
    }

    @Override
    public void halfMoveClockChanged(int halfMoveClock) {
        controlPanel.updateHalfMoveClock(halfMoveClock);
    }

    @Override
    public void plyNumberChanged(int plyNumber) {
        controlPanel.updatePlyCount(plyNumber);
    }

    @Override
    public void sqiEPChanged(int sqiEP) {
        // TODO Auto-generated method stub
    }

    @Override
    public void squareChanged(int sqi, int stone) {
        paintStoneAt(sqi, stone);
    }

    @Override
    public void toPlayChanged(int toPlay) {
        MaterialWithData mat;
        if (toPlay == Chess.WHITE) {
            mat = getWhiteSquareMat();
        } else if (toPlay == Chess.BLACK) {
            mat = getBlackSquareMat();
        } else if (toPlay == Chess.NOBODY) {
            mat = getControlPanelMat();
        } else {
            return; // should never get here
        }
        controlPanel.updateToMoveIndicator(mat);
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
        Cuboid bounds = getBounds();
        bounds.inset(Direction.Horizontal, 1);
        bounds.shift(Direction.Up, minHeight);
        bounds.expand(Direction.Up, maxHeight - minHeight);
        return bounds.contains(loc);
    }

    /**
     * check if this is a part of the board floor
     * @param loc location to check
     * @return true if the location is part of the board itself
     */
    public boolean isOnBoard(Location loc) {
        return isOnBoard(loc, 0, 0);
    }

    /**
     * check if this is a space within the board bounds, and above the board
     * @param loc location to check
     * @return true if the location is above the board <br>
     * AND within the board's height range
     */
    public boolean isAboveBoard(Location loc) {
        return isOnBoard(loc, 1, height);
    }

    /**
     * check if this is somewhere within the board bounds
     * @param loc location to check
     * @return true if the location is *anywhere* within the board <br>
     * including frame & enclosure
     */
    public boolean isPartOfBoard(Location loc) {
        return getOuterBounds().contains(loc);
    }

    public boolean isControlPanel(Location loc) {
        return controlPanel.getPanelBlocks().contains(loc);
    }

    public int getSquareAt(Location loc) {
        if (!isOnBoard(loc, 0, height)) {
            return Chess.NO_SQUARE;
        }
        int row = (a1Square.getBlockX() - loc.getBlockX()) / squareSize;
        int col = (a1Square.getBlockZ() - loc.getBlockZ()) / squareSize;
        return Chess.coorToSqi(col, row);
    }

    public void delete() {
        delete(false, null);
    }

    public void delete(boolean deleteBlocks, Player p) {
        if (deleteBlocks) {
            restoreTerrain(p);
        }
        BoardView.removeBoardView(getName());
    }

    /**
     * delete blocks in bounds, but don't allow items to drop (paintings are not
     * blocks, and are not included...) also does not scan the faces of the
     * region for drops when the region is cleared
     */
    public void wipe() {
        Block b = null;
        // first remove blocks that might pop off & leave a drop
        for (Location l : getOuterBounds()) {
            b = l.getBlock();
            if (BlockType.shouldPlaceLast(b.getTypeId())) {
                b.setTypeId(0);
            }// also check if this is a container
            else if (BlockType.isContainerBlock(b.getTypeId())) {
                BlockState state = b.getState();
                if (state instanceof org.bukkit.block.ContainerBlock) {
                    org.bukkit.block.ContainerBlock chest = (org.bukkit.block.ContainerBlock) state;
                    Inventory inven = chest.getInventory();
                    inven.clear();
                }
            }
        }
        // now wipe all (remaining) blocks
        for (Location l : getOuterBounds()) {
            l.getBlock().setTypeId(0);
        }
    }

    public void restoreTerrain(Player player) {
        wipe();
        if (plugin.getWorldEdit() != null) {
            TerrainBackup.reload(plugin, player, this);
        }
    }

    public Location findSafeLocationOutside() {
        Location dest0 = getA1Square().clone();

        dest0.add(getFrameWidth() + 1, 0.0, getFrameWidth() + 1);
        Location dest1 = dest0.clone().add(0.0, 1.0, 0.0);

        while (dest0.getBlock().getTypeId() != 0 && dest1.getBlock().getTypeId() != 0) {
            dest0.add(0.0, 1.0, 0.0);
            dest1.add(0.0, 1.0, 0.0);
            if (dest1.getBlockY() > 127) {
                return null;
            }
        }
        return dest0;
    }

    /*------------------------------------------------------------------------------_*/
    public static void addBoardView(String name, BoardView view) {
        chessBoards.put(name, view);
    }
    
    public static void addBoardView(BoardView view) {
        chessBoards.put(view.name, view);
    }

    public static void removeBoardView(String name) {
        chessBoards.remove(name);
    }

    public static void removeAllBoardViews() {
        chessBoards.clear();
    }

    public static Boolean checkBoardView(String name) {
        return chessBoards.containsKey(name);
    }

    public static BoardView getBoardView(String name) throws ChessException {
        if (!chessBoards.containsKey(name)) {
            if (chessBoards.size() > 0) {
                // try "fuzzy" search
                String keys[] = chessBoards.keySet().toArray(new String[0]);
                String matches[] = ChessUtils.fuzzyMatch(name, keys, 3);
                
                if (matches.length == 1) {
                    return chessBoards.get(matches[0]);
                } else {
                    // partial-name search
                    int k = -1, c = 0;
                    name = name.toLowerCase();
                    for (int i = 0; i < keys.length; ++i) {
                        if (keys[i].toLowerCase().startsWith(name)) {
                            k = i;
                            ++c;
                        }
                    }
                    if (k >= 0 && c == 1) {
                        return chessBoards.get(keys[k]);
                    }
                }
            }
            throw new ChessException("No such board '" + name + "'");
        }
        return chessBoards.get(name);
    }

    public static List<BoardView> listBoardViews(boolean isSorted) {
        if (isSorted) {
            SortedSet<String> sorted = new TreeSet<String>(chessBoards.keySet());
            List<BoardView> res = new ArrayList<BoardView>();
            for (String name : sorted) {
                res.add(chessBoards.get(name));
            }
            return res;
        } else {
            return new ArrayList<BoardView>(chessBoards.values());
        }

    }

    public static List<BoardView> listBoardViews() {
        return listBoardViews(false);
    }

    public static BoardView getFreeBoard() throws ChessException {
        for (BoardView bv : listBoardViews()) {
            if (bv.getGame() == null) {
                return bv;
            }
        }
        throw new ChessException("There are no free boards to create a game on.");
    }

    /**
     * match if loc is any part of the board including the frame & enclosure
     * @param loc location to check
     * @return the boardview that matches, or null if none
     */
    public static BoardView partOfChessBoard(Location loc) {
        for (BoardView bv : listBoardViews()) {
            if (bv.isPartOfBoard(loc)) {
                return bv;
            }
        }
        return null;
    }

    /**
     * match if loc is above a board square but below the roof
     * @param loc location to check
     * @return the boardview that matches, or null if none
     */
    public static BoardView aboveChessBoard(Location loc) {
        for (BoardView bv : listBoardViews()) {
            if (bv.isAboveBoard(loc)) {
                return bv;
            }
        }
        return null;
    }

    /**
     * match if loc is part of a board square
     * @param loc location to check
     * @return the boardview that matches, or null if none
     */
    public static BoardView onChessBoard(Location loc) {
        for (BoardView bv : listBoardViews()) {
            if (bv.isOnBoard(loc)) {
                return bv;
            }
        }
        return null;
    }
}
