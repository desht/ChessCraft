package me.desht.chesscraft.commands;

import java.util.LinkedList;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessAI;
import me.desht.chesscraft.chess.ChessAI.AI_Def;
import me.desht.chesscraft.chess.ChessGame;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.results.Results;
import me.desht.chesscraft.results.ScoreRecord;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.MinecraftChatStr;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.entity.Player;

import chesspresso.Chess;

public class ListCommand extends AbstractCommand {

	public ListCommand() {
		super("chess l", 1, 3);
		setUsage(new String[] {
				"/chess list board",
				"/chess list game",
				"/chess list ai",
				"/chess list top [<n>] [ladder|league]",
		});
	}

	@Override
	public boolean execute(ChessCraft plugin, Player player, String[] args) throws ChessException {
		if (args[0].startsWith("g")) { // game //$NON-NLS-1$
			if (args.length > 1) {
				ChessGame.getGame(args[1]).showGameDetail(player);
			} else {
				listGames(player);
			}
		} else if (args[0].startsWith("b")) { // board //$NON-NLS-1$
			if (args.length > 1) {
				BoardView.getBoardView(args[1]).showBoardDetail(player);
			} else {
				listBoards(player);
			}
		} else if (args[0].startsWith("a")) { // ai //$NON-NLS-1$
			listAIs(player);
		} else if (args[0].startsWith("t")) { // top scores //$NON-NLS-1$
			listScores(player, args);
		} else {
			MiscUtil.errorMessage(player, "Usage: /chess list board"); //$NON-NLS-1$
			MiscUtil.errorMessage(player, "       /chess list game"); //$NON-NLS-1$
			MiscUtil.errorMessage(player, "       /chess list ai"); //$NON-NLS-1$
			MiscUtil.errorMessage(player, "       /chess list top [<n>] [ladder|league]"); //$NON-NLS-1$
		}
		
		return true;
	}

	void listGames(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.list.game");
		
		if (ChessGame.listGames().isEmpty()) {
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.noCurrentGames")); //$NON-NLS-1$
			return;
		}

		MessagePager pager = MessagePager.getPager(player).clear();
		for (ChessGame game : ChessGame.listGames(true)) {
			String name = game.getName();
			String curGameMarker = "  "; //$NON-NLS-1$
			if (player != null) {
				curGameMarker = game == ChessGame.getCurrentGame(player) ? "+ " : "  "; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String curMoveW = game.getPosition().getToPlay() == Chess.WHITE ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String curMoveB = game.getPosition().getToPlay() == Chess.BLACK ? "&4*&-" : ""; //$NON-NLS-1$ //$NON-NLS-2$
			String white = game.getPlayerWhite().isEmpty() ? "?" : game.getPlayerWhite(); //$NON-NLS-1$
			String black = game.getPlayerBlack().isEmpty() ? "?" : game.getPlayerBlack(); //$NON-NLS-1$
			StringBuilder info = new StringBuilder(": &f" + curMoveW + white + " (W) v " + curMoveB + black + " (B) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			info.append("&e[").append(game.getState()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (game.getInvited().length() > 0) {
				info.append(Messages.getString("ChessCommandExecutor.invited", game.getInvited())); //$NON-NLS-1$
			}
			pager.add(curGameMarker + name + info);
		}
		pager.showPage();
	}

	void listBoards(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.list.board");
		
		if (BoardView.listBoardViews().isEmpty()) {
			MiscUtil.statusMessage(player, Messages.getString("ChessCommandExecutor.noBoards")); //$NON-NLS-1$
			return;
		}

		MessagePager pager = MessagePager.getPager(player).clear();
		for (BoardView bv : BoardView.listBoardViews(true)) {
			String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("ChessCommandExecutor.noGame"); //$NON-NLS-1$
			pager.add(Messages.getString("ChessCommandExecutor.boardList", bv.getName(), ChessUtils.formatLoc(bv.getA1Square()), //$NON-NLS-1$
			                             bv.getBoardStyleName(), gameName));
		}
		pager.showPage();
	}

	void listAIs(Player player) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.list.ai");
		
		MessagePager pager = MessagePager.getPager(player).clear();
		LinkedList<String> lines = new LinkedList<String>();
		for (AI_Def ai : ChessAI.listAIs(true)) {
			StringBuilder sb = new StringBuilder(Messages.getString("ChessCommandExecutor.AIList", //$NON-NLS-1$
			                                                        ai.getName(), ai.getEngine(), ai.getSearchDepth()));
			if (ChessCraft.economy != null) {
				sb.append(player != null ? "<l>" : ", "); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(Messages.getString("ChessCommandExecutor.AIpayout", (int) (ai.getPayoutMultiplier() * 100))); //$NON-NLS-1$
			}

			if (ai.getComment() != null && player != null && ((lines.size() + 1) % MessagePager.getPageSize()) == 0) {
				lines.add(""); // ensure description and comment are on the same page  $NON-NLS-1$
			}
			lines.add(sb.toString());
			if (ai.getComment() != null) {
				lines.add("  &2 - " + ai.getComment()); //$NON-NLS-1$
			}
		}
		lines = MinecraftChatStr.alignTags(lines, true);
		pager.add(lines);
		pager.showPage();
	}

	void listScores(Player player, String[] args) throws ChessException {
		PermissionUtils.requirePerms(player, "chesscraft.commands.list.top");
		
		String viewName = "ladder";
		int n = 5;
		if (args.length > 1) {
			try {
				n = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new ChessException(Messages.getString("ChessCommandExecutor.invalidNumeric", args[1]));
			}
		}
		if (args.length > 2) {
			viewName = args[2];
		}
		
		MessagePager pager = MessagePager.getPager(player).clear();
		int row = 1;
		for (ScoreRecord sr : Results.getResultsHandler().getView(viewName).getScores(n)) {
			pager.add(Messages.getString("ChessCommandExecutor.scoreRecord", row, sr.getPlayer(), sr.getScore()));
			row++;
		}
		pager.showPage();
	}
}
