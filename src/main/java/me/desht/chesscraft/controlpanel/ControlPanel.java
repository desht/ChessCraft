package me.desht.chesscraft.controlpanel;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.chess.TimeControlDefs;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;

import chesspresso.Chess;

import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.dhutils.PersistableLocation;
import me.desht.chesscraft.enums.BoardRotation;

public class ControlPanel {

	public static final int PANEL_WIDTH = 8;

	private final BoardView view;
	private final BoardRotation boardDir, signDir;
	private final Cuboid panelBlocks;
	private final Cuboid toMoveIndicator;
	private final PlyCountLabel plyCountLabel;
	private final HalfmoveClockLabel halfMoveClockLabel;
	private final ClockLabel[] clockLabels;
	private final Map<PersistableLocation, AbstractSignButton> buttonLocs;
	private final Map<String,AbstractSignButton> buttonNames;

	public ControlPanel(BoardView view) {
		this.view = view;
		boardDir = view.getRotation();
		signDir = boardDir.getRight();

		buttonLocs = new HashMap<PersistableLocation, AbstractSignButton>();
		buttonNames = new HashMap<String, AbstractSignButton>();
		
		panelBlocks = getPanelPosition();
		
		createSignButtons();
		
		plyCountLabel = new PlyCountLabel(this);
		halfMoveClockLabel = new HalfmoveClockLabel(this);
		clockLabels = new ClockLabel[2];
		clockLabels[Chess.WHITE] = new ClockLabel(this, Chess.WHITE);
		clockLabels[Chess.BLACK] = new ClockLabel(this, Chess.BLACK);

		toMoveIndicator = panelBlocks.inset(Direction.Vertical, 1).
				expand(boardDir.getDirection(), -((PANEL_WIDTH - 2) / 2)).
				expand(boardDir.getDirection().opposite(), -((PANEL_WIDTH - 2) / 2));
	}

	private void createSignButtons() {
		createSignButton(new BlackNoButton(this));
		createSignButton(new BlackYesButton(this));
		createSignButton(new BoardInfoButton(this));
		createSignButton(new CreateGameButton(this));
		createSignButton(new GameInfoButton(this));
		createSignButton(new InviteAnyoneButton(this));
		createSignButton(new InvitePlayerButton(this));
		createSignButton(new OfferDrawButton(this));
		createSignButton(new PromoteBlackButton(this));
		createSignButton(new PromoteWhiteButton(this));
		createSignButton(new ResignButton(this));
		createSignButton(new StakeButton(this));
		createSignButton(new StartButton(this));
		createSignButton(new TeleportButton(this));
		createSignButton(new TimeControlButton(this));
		createSignButton(new WhiteNoButton(this));
		createSignButton(new WhiteYesButton(this));
	}
	
	private void createSignButton(AbstractSignButton button) {
		buttonLocs.put(button.getLocation(), button);
		buttonNames.put(button.getClass().getSimpleName(), button);
	}

	public BoardView getView() {
		return view;
	}

	public TimeControlDefs getTcDefs() {
		return getSignButton(TimeControlButton.class).getTcDefs();
	}
	
	public <T extends AbstractSignButton> T getSignButton(Class<T> type) {
		return type.cast(buttonLocs.get(type.getSimpleName()));
	}
	
	/**
	 * Get a teleport-in location for this control panel.  Player will be standing in front of the
	 * control panel, facing it.
	 * 
	 * @return	The teleport-in location
	 */
	public Location getTeleportLocation() {
		double xOff = (panelBlocks.getUpperX() - panelBlocks.getLowerX()) / 2.0 + 0.5 + signDir.getXadjustment() * 3.5;
		double zOff = (panelBlocks.getUpperZ() - panelBlocks.getLowerZ()) / 2.0 + 0.5 + signDir.getZadjustment() * 3.5;
	
		return new Location(panelBlocks.getWorld(),
		                    panelBlocks.getLowerX() + xOff,
		                    panelBlocks.getLowerY(),
		                    panelBlocks.getLowerZ() + zOff,
		                    (signDir.getYaw() + 180.0f) % 360,
		                    0.0f);
	}

	public Cuboid getPanelBlocks() {
		return panelBlocks;
	}

	public void repaint() {
		panelBlocks.setFast(view.getControlPanelMaterial());
		panelBlocks.forceLightLevel(view.getChessBoard().getBoardStyle().getLightLevel());

		repaintSignButtons();
		repaintClocks();
		halfMoveClockLabel.repaint();
		plyCountLabel.repaint();
	}

	public void repaintClocks() {
		ChessGame game = view.getGame();
		updateClock(Chess.WHITE, game == null ? null : game.getTcWhite());
		updateClock(Chess.BLACK, game == null ? null : game.getTcBlack());
	}

	public void repaintSignButtons() {
		for (AbstractSignButton btn : buttonLocs.values()) {
			btn.repaint();
		}
	}
	
	public void signClicked(PlayerInteractEvent event) throws ChessException {
		AbstractSignButton btn = buttonLocs.get(new PersistableLocation(event.getClickedBlock().getLocation()));
		if (btn != null) {
			btn.onClicked(event);
		}
	}
	
	public void updateToMoveIndicator(MaterialWithData mat) {
		toMoveIndicator.set(mat);
	}

	public void updatePlyCount(int plyNumber) {
		plyCountLabel.setCount(plyNumber);
		plyCountLabel.repaint();
	}

	public void updateHalfMoveClock(int halfMoveClock) {
		halfMoveClockLabel.setCount(halfMoveClock);
		halfMoveClockLabel.repaint();
	}

	public void updateClock(int colour, TimeControl tc) {
		clockLabels[colour].setTimeControl(tc);
		clockLabels[colour].repaint();
	}

	/**
	 * Calculate the control panel position based on the boardview's position
	 * and rotation.
	 * 
	 * @return	a Cuboid representing the panel position
	 */
	private Cuboid getPanelPosition() {
		BoardRotation dir = view.getRotation();
		BoardRotation dirLeft = dir.getLeft();
		Location a1 = view.getA1Square();

		int panelOffset = 4 * view.getSquareSize() - PANEL_WIDTH / 2;
		int frameOffset = (int) Math.ceil((view.getFrameWidth() + .5) / 2);
		
		// for the control panel edge, move <panelOffset> blocks in the board's direction, then
		// <frameOffset> blocks to the left of that.
		int x = a1.getBlockX() + dir.getXadjustment(panelOffset) + dirLeft.getXadjustment(frameOffset);
		int y = a1.getBlockY() + 1;
		int z = a1.getBlockZ() + dir.getZadjustment(panelOffset) + dirLeft.getZadjustment(frameOffset);
		// then expand the cuboid in the board's direction by the panel's desired width
		Cuboid panel = new Cuboid(new Location(a1.getWorld(), x, y, z));
		return panel.expand(dir.getDirection(), PANEL_WIDTH - 1).expand(Direction.Up, 2);
	}

}
