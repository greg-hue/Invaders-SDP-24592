package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Random;
import audio.AudioManager;

import engine.Cooldown;
import engine.Core;

/**
 * Implements the title screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class TitleScreen extends Screen {

	// Variable to track which menu option is selected (0 = first choice)
	private int selectedMenuOption = 0;

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;

	/** Number of twinkling stars in the background. */
	private static final int STAR_COUNT = 45;
	/** Number of dim invaders floating in the background. */
	private static final int GHOST_COUNT = 5;
	/** Frames between two animation frames of the invaders. */
	private static final int INVADER_ANIMATION_FRAMES = 30;

	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;
	/** If the mouse button was pressed on the previous frame. */
	private boolean wasMousePressed = false;

	/** Frame counter, drives every background animation. */
	private int frameCount;
	/** Background stars. */
	private final Star[] stars = new Star[STAR_COUNT];
	/** Dim invaders floating in the background. */
	private final Ghost[] ghosts = new Ghost[GHOST_COUNT];

	/** A twinkling star of the background. */
	private static final class Star {
		int x, y, type, phase, period;
	}

	/** A dim invader floating in the background. */
	private static final class Ghost {
		int x, y, speed, type, phase;
	}

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width
	 *               Screen width.
	 * @param height
	 *               Screen height.
	 * @param fps
	 *               Frames per second, frame rate at which the game is run.
	 */
	public TitleScreen(final int width, final int height, final int fps) {
		super(width, height, fps);

		// Defaults to play.
		this.returnCode = 2;
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();

		initBackground(width, height);

		AudioManager.playBGM("audio/bgm/music_menu.wav");
	}

	/**
	 * Places the stars and the floating invaders. A fixed seed keeps the
	 * background identical each time the title screen opens.
	 */
	private void initBackground(final int width, final int height) {
		Random random = new Random(24592);
		for (int i = 0; i < stars.length; i++) {
			Star star = new Star();
			star.x = random.nextInt(width - 10);
			star.y = random.nextInt(height - 10);
			// Mostly small stars, a few big ones.
			int roll = random.nextInt(10);
			star.type = roll < 6 ? 2 : (roll < 9 ? 1 : 0);
			star.period = 90 + random.nextInt(120);
			star.phase = random.nextInt(star.period);
			stars[i] = star;
		}
		for (int i = 0; i < ghosts.length; i++) {
			Ghost ghost = new Ghost();
			ghost.x = random.nextInt(width);
			ghost.y = 150 + random.nextInt(Math.max(1, height - 200));
			ghost.speed = 1 + random.nextInt(2);
			ghost.type = random.nextInt(2);
			ghost.phase = random.nextInt(INVADER_ANIMATION_FRAMES * 2);
			ghosts[i] = ghost;
		}
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();
		// Get mouse position relative to the game drawing area.
		int mouseX = inputManager.getMouseX();
		int mouseY = inputManager.getMouseY();

		int hoveredOption = drawManager.getMenuOptionAt(this, mouseX, mouseY);

		// Update selection when hovering over a different option.
		if (hoveredOption != -1 && hoveredOption != this.returnCode) {
			this.returnCode = hoveredOption;
			AudioManager.playSFX("audio/sfx/menu_move.wav");
		}

		boolean mousePressed = inputManager.isMousePressed();
		boolean newMouseClick = mousePressed && !wasMousePressed;

		wasMousePressed = mousePressed;

		if (newMouseClick
				&& hoveredOption != -1
				&& this.inputDelay.checkFinished()) {

			this.returnCode = hoveredOption;
			AudioManager.playSFX("audio/sfx/menu_select.wav");
			this.isRunning = false;
		}

		this.frameCount++;
		draw();
		if (this.selectionCooldown.checkFinished()
				&& this.inputDelay.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {
				previousMenuItem();
				this.selectionCooldown.reset();
				AudioManager.playSFX("audio/sfx/menu_move.wav");
			}
			if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {
				nextMenuItem();
				this.selectionCooldown.reset();
				AudioManager.playSFX("audio/sfx/menu_move.wav");
			}
			if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
				AudioManager.playSFX("audio/sfx/menu_select.wav");
				this.isRunning = false;
			}
			// Shortcut to the infinite mode, until it gets a menu entry.
			if (inputManager.isKeyDown(KeyEvent.VK_I)) {
				AudioManager.playSFX("audio/sfx/menu_select.wav");
				this.returnCode = 8;
				this.isRunning = false;
			}
		}
	}

	/**
	 * Shifts the focus to the next menu item.
	 */
	private void nextMenuItem() {
		// Order: Play (2) -> High scores (3) -> Settings (4) -> Exit (0).
		if (this.returnCode == 4)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 2;
		else
			this.returnCode++;
	}

	/**
	 * Shifts the focus to the previous menu item.
	 */
	private void previousMenuItem() {
		if (this.returnCode == 0)
			this.returnCode = 4;
		else if (this.returnCode == 2)
			this.returnCode = 0;
		else
			this.returnCode--;
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawBackground();
		drawManager.drawTitle(this);
		drawManager.drawMenu(this, this.returnCode);
		drawManager.drawMenuIcons(this, this.returnCode, this.frameCount);

		drawManager.completeDrawing(this);
	}

	/**
	 * Draws the animated background: twinkling stars, dim invaders floating
	 * across the screen, and a bright invader formation above the title.
	 */
	private void drawBackground() {
		// Twinkling stars, brightness goes up and down in a triangle wave.
		for (Star star : stars) {
			int half = star.period / 2;
			int t = (frameCount + star.phase) % star.period;
			int wave = t < half ? t : star.period - t;
			int level = Math.min(255, 50 + 205 * wave / half);
			drawManager.drawSprite(MenuSprites.STARS[star.type], star.x, star.y, 2,
					new Color(level, level, level));
		}

		// Dim invaders drifting from left to right, behind the menu.
		for (Ghost ghost : ghosts) {
			int x = (ghost.x + frameCount * ghost.speed / 2) % (getWidth() + 60) - 40;
			boolean[][][] kind = ghost.type == 0 ? MenuSprites.CRAB : MenuSprites.OCTOPUS;
			int step = ((frameCount + ghost.phase) / INVADER_ANIMATION_FRAMES) % 2;
			drawManager.drawSprite(kind[step], x, ghost.y, 2,
					new Color(30, 70, 30));
		}

		// Bright formation above the title: one on top, two below.
		int sway = (int) (Math.sin(frameCount / 50.0) * 14);
		int step = (frameCount / INVADER_ANIMATION_FRAMES) % 2;
		int centerX = getWidth() / 2;
		drawManager.drawSprite(MenuSprites.OCTOPUS[step], centerX - 18 + sway, 20, 3,
				Color.WHITE);
		drawManager.drawSprite(MenuSprites.CRAB[step], centerX - 85 + sway, 72, 3,
				Color.WHITE);
		drawManager.drawSprite(MenuSprites.CRAB[step], centerX + 52 + sway, 72, 3,
				Color.WHITE);
	}

}