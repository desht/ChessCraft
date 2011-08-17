package me.desht.chesscraft.results;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.results.ResultEntry;

/**
 * 
 * Implements a sort-of-ELO ladder.  Similar algorithm for adjusting player scores.
 * 
 * @author des
 *
 */
public class Ladder extends ResultViewBase {

	private final int INITIAL_POS = 1000;

	/**
	 * Create a new Ladder object.
	 */
	public Ladder(Results handler) {
		super(handler, "ladder");
	}

	/**
	 * Add one result to the ladder.
	 * 
	 * @param re	The ResultEntry to add
	 */
	@Override
	public void addResult(ResultEntry re) {
		applyRatingChange(re);
	}

	private void applyRatingChange(ResultEntry re) {
		int ratingW = getScore(re.getPlayerWhite());
		int ratingB = getScore(re.getPlayerBlack());

		float prob = getProbability(Math.abs(ratingW - ratingB));
		float probW = ratingW > ratingB ? prob : 1 - prob;
		float probB = 1 - probW;

//		System.out.println(String.format("add result: %s %s %s %.2f %.2f", re.playerWhite, re.playerBlack, re.pgnResult, probW, probB));
		if (re.getPgnResult().equals("1-0")) {
			setScore(re.getPlayerWhite(), ratingW + Math.round(getKfactor(ratingW) * (1 - probW)), true);
			setScore(re.getPlayerBlack(), ratingB + Math.round(getKfactor(ratingB) * (0 - probB)), true);
		} else if (re.getPgnResult().equals("0-1")) {
			setScore(re.getPlayerWhite(), ratingW + Math.round(getKfactor(ratingW) * (0 - probW)), true);
			setScore(re.getPlayerBlack(), ratingB + Math.round(getKfactor(ratingB) * (1 - probB)), true);
		} else {
			setScore(re.getPlayerWhite(), ratingW + Math.round(getKfactor(ratingW) * (0.5f - probW)), true);
			setScore(re.getPlayerBlack(), ratingB + Math.round(getKfactor(ratingB) * (0.5f - probB)), true);
		}
	}

	/**
	 * Calculate win probability based on ratings difference.
	 * Taken from FIDE handbook, section 8.0.
	 * 
	 * @param diff	Ratings difference
	 * @return		Win probability for higher-rated player
	 */
	private float getProbability(int diff) {
		if (diff >= 0 && diff <= 3) {
			return 0.50f;
		} else if (diff >= 4 && diff <= 10) {
			return 0.51f;
		} else if (diff >= 11 && diff <= 17) {
			return 0.52f;
		} else if (diff >= 18 && diff <= 25) {
			return 0.53f;
		} else if (diff >= 26 && diff <= 32) {
			return 0.54f;
		} else if (diff >= 33 && diff <= 39) {
			return 0.55f;
		} else if (diff >= 40 && diff <= 46) {
			return 0.56f;
		} else if (diff >= 47 && diff <= 53) {
			return 0.57f;
		} else if (diff >= 54 && diff <= 61) {
			return 0.58f;
		} else if (diff >= 62 && diff <= 68) {
			return 0.59f;
		} else if (diff >= 69 && diff <= 76) {
			return 0.60f;
		} else if (diff >= 77 && diff <= 83) {
			return 0.61f;
		} else if (diff >= 84 && diff <= 91) {
			return 0.62f;
		} else if (diff >= 92 && diff <= 98) {
			return 0.63f;
		} else if (diff >= 99 && diff <= 106) {
			return 0.64f;
		} else if (diff >= 107 && diff <= 113) {
			return 0.65f;
		} else if (diff >= 114 && diff <= 121) {
			return 0.66f;
		} else if (diff >= 122 && diff <= 129) {
			return 0.67f;
		} else if (diff >= 130 && diff <= 137) {
			return 0.68f;
		} else if (diff >= 138 && diff <= 145) {
			return 0.69f;
		} else if (diff >= 146 && diff <= 153) {
			return 0.70f;
		} else if (diff >= 154 && diff <= 162) {
			return 0.71f;
		} else if (diff >= 163 && diff <= 170) {
			return 0.72f;
		} else if (diff >= 171 && diff <= 179) {
			return 0.73f;
		} else if (diff >= 180 && diff <= 188) {
			return 0.74f;
		} else if (diff >= 189 && diff <= 197) {
			return 0.75f;
		} else if (diff >= 198 && diff <= 206) {
			return 0.76f;
		} else if (diff >= 207 && diff <= 215) {
			return 0.77f;
		} else if (diff >= 216 && diff <= 225) {
			return 0.78f;
		} else if (diff >= 226 && diff <= 235) {
			return 0.79f;
		} else if (diff >= 236 && diff <= 245) {
			return 0.80f;
		} else if (diff >= 246 && diff <= 256) {
			return 0.81f;
		} else if (diff >= 257 && diff <= 267) {
			return 0.82f;
		} else if (diff >= 268 && diff <= 278) {
			return 0.83f;
		} else if (diff >= 279 && diff <= 290) {
			return 0.84f;
		} else if (diff >= 291 && diff <= 302) {
			return 0.85f;
		} else if (diff >= 303 && diff <= 315) {
			return 0.86f;
		} else if (diff >= 316 && diff <= 328) {
			return 0.87f;
		} else if (diff >= 329 && diff <= 344) {
			return 0.88f;
		} else if (diff >= 345 && diff <= 357) {
			return 0.89f;
		} else if (diff >= 358 && diff <= 374) {
			return 0.90f;
		} else if (diff >= 375 && diff <= 391) {
			return 0.91f;
		} else if (diff >= 392 && diff <= 411) {
			return 0.92f;
		} else if (diff >= 412 && diff <= 432) {
			return 0.93f;
		} else if (diff >= 433 && diff <= 456) {
			return 0.94f;
		} else if (diff >= 457 && diff <= 484) {
			return 0.95f;
		} else if (diff >= 485 && diff <= 517) {
			return 0.96f;
		} else if (diff >= 518 && diff <= 559) {
			return 0.97f;
		} else if (diff >= 560 && diff <= 619) {
			return 0.98f;
		} else if (diff >= 620 && diff <= 735) {
			return 0.99f;
		} else {
			return 1.0f;
		}
	}

	@Override
	int getInitialScore() {
		return ChessConfig.getConfiguration().getInt("ladder.initial_position", INITIAL_POS);
	}

	private int getKfactor(int rating) {
		if (rating < 2000) {
			return 32;
		} else if (rating < 2400) {
			return 16;
		} else {
			return 8;
		}
	}
}