package me.desht.chesscraft.chess.ai;

import org.bukkit.configuration.ConfigurationSection;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import fr.free.jchecs.ai.Engine;
import fr.free.jchecs.ai.EngineFactory;
import fr.free.jchecs.core.Game;
import fr.free.jchecs.core.Move;
import fr.free.jchecs.core.MoveGenerator;
import fr.free.jchecs.core.Player;
import fr.free.jchecs.core.Square;

/**
 * @author des
 * 
 */
public class JChecsAI extends AbstractAI {

	private final Game jChecsGame;

	public JChecsAI(String name, ChessGame chessCraftGame, Boolean isWhite, ConfigurationSection params) {
		super(name, chessCraftGame, isWhite, params);

		jChecsGame = initGame();
	}
	
	/**
	 * Initialise the jChecs Game object.
	 * 
	 * @return
	 */
	private Game initGame() {
		Game jChecsGame = new Game();

		Player human = jChecsGame.getPlayer(!isWhite());
		human.setName(Messages.getString("ChessAI.human"));
		human.setEngine(null);

		Player ai = jChecsGame.getPlayer(isWhite());
		ai.setName(Messages.getString("ChessAI.computer"));
		String engine = "jChecs." + params.getString("engine", "MiniMax");
		Engine moteur = EngineFactory.newInstance(engine);
		if (moteur == null) {
			throw new ChessException("unknown jChecs engine: " + engine);
		}
		int searchDepth = params.getInt("depth", 1);
		moteur.setSearchDepthLimit(searchDepth);
		moteur.setOpeningsEnabled(ChessCraft.getInstance().getConfig().getBoolean("ai.use_opening_book", false));
		ai.setEngine(moteur);

		return jChecsGame;
	}

	/* (non-Javadoc)
	 * @see me.desht.chesscraft.chess.ai.AbstractAI#shutdown()
	 */
	@Override
	public void shutdown() {
		jChecsGame.getPlayer(isWhite()).setEngine(null);
	}

	/* (non-Javadoc)
	 * @see me.desht.chesscraft.chess.ai.AbstractAI#run()
	 */
	@Override
	public void run() {
		try {
			final MoveGenerator plateau = jChecsGame.getBoard();
			final Engine engine = jChecsGame.getPlayer(isWhite()).getEngine();
			final fr.free.jchecs.core.Move m = engine.getMoveFor(plateau);
			aiHasMoved(m.getFrom().getIndex(), m.getTo().getIndex());
		} catch (Exception e) {
			aiHasFailed(e);
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.chesscraft.chess.ai.AbstractAI#undoLastMove()
	 */
	@Override
	public void undoLastMove() {
		jChecsGame.goPrevious();
	}

	/* (non-Javadoc)
	 * @see me.desht.chesscraft.chess.ai.AbstractAI#movePiece(int, int)
	 */
	@Override
	protected void movePiece(int fromSqi, int toSqi) {
		// conveniently, Chesspresso & jChecs use the same row/column/sqi conventions
		Square from = Square.valueOf(fromSqi);
		Square to = Square.valueOf(toSqi);

		Move m = new Move(jChecsGame.getBoard().getPieceAt(from), from, to);
		jChecsGame.moveFromCurrent(m);
	}
}
