package screen;

/**
 * Pixel art sprites used to decorate the menus.
 * Each sprite is indexed [row][column]; draw it with
 * {@code DrawManager.drawSprite}.
 */
public final class MenuSprites {

	/** Crab invader, two animation frames (as in the classic game). */
	public static final boolean[][][] CRAB = {
		parse(
			"..#.....#..",
			"...#...#...",
			"..#######..",
			".##.###.##.",
			"###########",
			"#.#######.#",
			"#.#.....#.#",
			"...##.##..."),
		parse(
			"..#.....#..",
			"#..#...#..#",
			"#.#######.#",
			"###.###.###",
			"###########",
			".#########.",
			"..#.....#..",
			".#.......#.") };

	/** Octopus invader, two animation frames. */
	public static final boolean[][][] OCTOPUS = {
		parse(
			"....####....",
			".##########.",
			"############",
			"###..##..###",
			"############",
			"...##..##...",
			"..##.##.##..",
			"##........##"),
		parse(
			"....####....",
			".##########.",
			"############",
			"###..##..###",
			"############",
			"..###..###..",
			".##..##..##.",
			"..##....##..") };

	/** Star sprites: big sparkle, medium sparkle, small cross. */
	public static final boolean[][][] STARS = {
		parse(
			"....#....",
			"....#....",
			"...###...",
			"...###...",
			"#########",
			"...###...",
			"...###...",
			"....#....",
			"....#...."),
		parse(
			"...#...",
			"...#...",
			"..###..",
			"#######",
			"..###..",
			"...#...",
			"...#..."),
		parse(
			"..#..",
			"..#..",
			"#####",
			"..#..",
			"..#..") };

	/**
	 * Builds a sprite from rows of text, where '#' is a filled pixel.
	 *
	 * @param rows
	 *            Rows of the sprite.
	 * @return The sprite, indexed [row][column].
	 */
	private static boolean[][] parse(final String... rows) {
		boolean[][] sprite = new boolean[rows.length][];
		for (int i = 0; i < rows.length; i++) {
			sprite[i] = new boolean[rows[i].length()];
			for (int j = 0; j < rows[i].length(); j++)
				sprite[i][j] = rows[i].charAt(j) == '#';
		}
		return sprite;
	}

	/**
	 * Constructor, not called.
	 */
	private MenuSprites() {

	}
}