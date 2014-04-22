package me.desht.chesscraft.chess.player;

import chesspresso.Chess;
import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.chess.ChessGameManager;
import me.desht.chesscraft.chess.TimeControl;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.expector.ExpectDrawResponse;
import me.desht.chesscraft.expector.ExpectSwapResponse;
import me.desht.chesscraft.expector.ExpectUndoResponse;
import me.desht.chesscraft.expector.ExpectYesNoResponse;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MiscUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HumanChessPlayer extends ChessPlayer {
	private final UUID uuid;
	private final String oldStyleName;

	public HumanChessPlayer(String id, String name, ChessGame game, int colour) {
		super(id, name, game, colour);
		if (MiscUtil.looksLikeUUID(id)) {
			uuid = UUID.fromString(id);
			oldStyleName = null;
		} else {
			// not a UUID - then *hopefully* this is a Bukkit player name
			// which can be migrated to a UUID
			ChessGameManager.getManager().needToDoUUIDMigration(game);
			oldStyleName = id;
			uuid = null;
		}
	}

	public Player getBukkitPlayer() {
		return uuid == null ? null : Bukkit.getPlayer(uuid);
	}

	/**
	 * Get the old-style (player.getName()) player name.  This will only be non-null after an old
	 * save game file has been loaded and before a UUID migration operation has been carried out
	 * and is only intended to be used by the migration process.
	 *
	 * @return the old-style player name
	 */
	public String getOldStyleName() {
		return oldStyleName;
	}

	@Override
	public void promptForFirstMove() {
		alert(Messages.getString("Game.started", ChessUtils.getDisplayColour(getColour()), ChessUtils.getWandDescription()));
	}

	@Override
	public void promptForNextMove() {
		Player p = getBukkitPlayer();
		if (p == null)
			return;

		alert(Messages.getString("Game.playerPlayedMove",
		                         ChessUtils.getDisplayColour(getOtherColour()),
		                         getGame().getPosition().getLastMove().getSAN()));

		if (getGame().getPosition().isCheck()) {
			playEffect("check");
			alert(Messages.getString("Game.check"));
		}
	}

	@Override
	public void alert(String message) {
		Player p = getBukkitPlayer();
		if (p != null) {
			MiscUtil.alertMessage(p, Messages.getString("Game.alertPrefix", getGame().getName()) + message);
		}
	}

	@Override
	public void statusMessage(String message) {
		Player p = getBukkitPlayer();
		if (p != null) {
			MiscUtil.statusMessage(p, message);
		}
	}

	@Override
	public void replayMoves() {
		// nothing to do here
	}

	@Override
	public void cleanup() {
		// nothing to do here
	}

	@Override
	public void validateAffordability(String error) {
		if (error == null) error = "Game.cantAffordToJoin";
		double stake = getGame().getStake();
		Player player = getBukkitPlayer();
		if (ChessCraft.economy != null && player == null || !ChessCraft.economy.has(player.getName(), stake)) {
			throw new ChessException(Messages.getString(error, ChessUtils.formatStakeStr(stake)));
		}
	}

	@Override
	public void validateInvited(String error) {
		UUID invited = getGame().getInvited();
		if (!invited.equals(ChessGame.OPEN_INVITATION) && !invited.equals(uuid)) {
			throw new ChessException(Messages.getString(error));
		}
	}

	@Override
	public boolean isHuman() {
		return true;
	}

	@Override
	public void withdrawFunds(double amount) {
		ChessCraft.economy.withdrawPlayer(getDisplayName(), amount);
		alert(Messages.getString("Game.paidStake", ChessUtils.formatStakeStr(amount)));
	}

	@Override
	public void depositFunds(double amount) {
		ChessCraft.economy.depositPlayer(getDisplayName(), amount);
	}

	@Override
	public void summonToGame() {
		Player p = getBukkitPlayer();
		if (p != null) {
			getGame().getView().summonPlayer(p);
		}
	}

	@Override
	public void cancelOffers() {
		Player p = getBukkitPlayer();

		if (p != null) {
			// making a move after a draw/swap/undo offer has been made is equivalent to declining the offer
			ExpectYesNoResponse.handleYesNoResponse(p, false);
		}
	}

	@Override
	public double getPayoutMultiplier() {
		return 2.0;
	}

	@Override
	public void drawOffered() {
		ChessPlayer other = getGame().getPlayer(Chess.otherPlayer(getColour()));
		ChessCraft.getInstance().responseHandler.expect(getBukkitPlayer(), new ExpectDrawResponse(getGame(), getColour()));
		alert(Messages.getString("ChessCommandExecutor.drawOfferedOther", other.getDisplayName()));
		alert(Messages.getString("ChessCommandExecutor.typeYesOrNo"));
	}

	@Override
	public void swapOffered() {
		ChessPlayer other = getGame().getPlayer(Chess.otherPlayer(getColour()));
		ChessCraft.getInstance().responseHandler.expect(getBukkitPlayer(), new ExpectSwapResponse(getGame(), getColour()));
		alert(Messages.getString("ChessCommandExecutor.swapOfferedOther", other.getDisplayName()));
		alert(Messages.getString("ChessCommandExecutor.typeYesOrNo"));
	}

	@Override
	public void undoOffered() {
		ChessPlayer other = getGame().getPlayer(Chess.otherPlayer(getColour()));
		ChessCraft.getInstance().responseHandler.expect(getBukkitPlayer(), new ExpectUndoResponse(getGame(), getColour()));
		alert(Messages.getString("ChessCommandExecutor.undoOfferedOther", other.getDisplayName()));
		alert(Messages.getString("ChessCommandExecutor.typeYesOrNo"));
	}

	@Override
	public void undoLastMove() {
		// do nothing here
	}

	@Override
	public void checkPendingAction() {
		// do nothing here
	}

	@Override
	public void playEffect(String effect) {
		Player p = getBukkitPlayer();
		if (p != null) {
			ChessCraft.getInstance().getFX().playEffect(p.getLocation(), effect);
		}
	}

	@Override
	public void notifyTimeControl(TimeControl timeControl) {
		if (timeControl.isNewPhase()) {
			alert(Messages.getString("Game.newTimeControlPhase", timeControl.phaseString()));
		} else if (getGame().getPosition().getPlyNumber() <= 2) {
			alert(Messages.getString("ChessCommandExecutor.gameDetail.timeControlType", getGame().getTimeControl(getColour()).getSpec()));
		}
	}

}
