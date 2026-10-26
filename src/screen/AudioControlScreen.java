package screen;

import java.awt.event.KeyEvent;

import audio.AudioControls;
import audio.AudioManager;
import engine.Cooldown;
import engine.Core;

/**
 * Implements the audio settings screen.
 * Lets the player change the BGM volume, the SFX volume, and the mute state.
 */
public class AudioControlScreen extends Screen {

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;

	/** Menu option for the background music volume. */
	private static final int BGM_VOLUME_OPTION = 0;

	/** Menu option for the sound effect volume. */
	private static final int SFX_VOLUME_OPTION = 1;

	/** Menu option for the global mute state. */
	private static final int MUTE_OPTION = 2;

	/** Number of menu options. */
	private static final int OPTION_COUNT = 3;

	/** Amount the volume changes with one key press. */
	private static final int VOLUME_STEP = 5;

	/** Lowest volume. */
	private static final int MIN_VOLUME = 0;

	/** Highest volume. */
	private static final int MAX_VOLUME = 100;

	/** Volume option currently being dragged, or -1 if none. */
private int draggedVolumeOption = -1;

	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;

	/** Currently selected menu option. */
	private int selectedOption;

	/** Option under the mouse on the last frame, -1 if none. */
	private int lastHoveredOption = -1;

	/** Starts true so a click held from the previous screen is ignored. */
	private boolean wasMousePressed = true;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public AudioControlScreen(final int width, final int height, final int fps) {
		super(width, height, fps);

		// Returns to the main menu when closed.
		this.returnCode = 4;
		this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
		this.selectionCooldown.reset();

		// Defaults to the BGM volume.
		this.selectedOption = BGM_VOLUME_OPTION;
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

		draw();
		handleMouse();
		// Accept input only after the screen has been open for a moment and
		// the last input's cooldown has finished.
		if (this.selectionCooldown.checkFinished() && this.inputDelay.checkFinished()) {
			// Up and down move the selection between the options.
			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {
				previousOption();
				this.selectionCooldown.reset();
				AudioManager.playSFX("audio/sfx/menu_move.wav");
			}
			if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {
				nextOption();
				this.selectionCooldown.reset();
				AudioManager.playSFX("audio/sfx/menu_move.wav");
			}
			if (inputManager.isKeyDown(KeyEvent.VK_SPACE)
        	&& this.selectedOption == MUTE_OPTION) {
    		toggleMute();
    		this.selectionCooldown.reset();
			}
			// Left and right change the volume of the selected option only.
			if (this.selectedOption == BGM_VOLUME_OPTION) {
				if (inputManager.isKeyDown(KeyEvent.VK_LEFT)
						|| inputManager.isKeyDown(KeyEvent.VK_A)) {
					changeBGMVolume(-VOLUME_STEP);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/menu_move.wav");
				}
				if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
						|| inputManager.isKeyDown(KeyEvent.VK_D)) {
					changeBGMVolume(VOLUME_STEP);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/menu_move.wav");
				}
			}
			if (this.selectedOption == SFX_VOLUME_OPTION) {
				if (inputManager.isKeyDown(KeyEvent.VK_LEFT)
						|| inputManager.isKeyDown(KeyEvent.VK_A)) {
					changeSFXVolume(-VOLUME_STEP);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/menu_move.wav");
				}
				if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
						|| inputManager.isKeyDown(KeyEvent.VK_D)) {
					changeSFXVolume(VOLUME_STEP);
					this.selectionCooldown.reset();
					AudioManager.playSFX("audio/sfx/menu_move.wav");
				}
			}
			// M toggles the mute state, whichever option is selected.
			if (inputManager.isKeyDown(KeyEvent.VK_M)) {
				toggleMute();
				this.selectionCooldown.reset();
			}
			// Esc closes the screen.
			if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)){
				AudioManager.playSFX("audio/sfx/menu_back.wav");
				this.isRunning = false;
		}
		}
	}

	/**
	 * Selects the option under the mouse. Clicking the mute option toggles
	 * the mute state.
	 */

/**
 * Handles mouse selection, volume dragging, and mute clicks.
 */
private void handleMouse() {
    int mouseX = inputManager.getMouseX();
    int mouseY = inputManager.getMouseY();

    int hoveredOption = drawManager.getAudioSettingsOptionAt(
            this, OPTION_COUNT, mouseX, mouseY);

    int hoveredBar = drawManager.getAudioSettingsBarAt(
            this, mouseX, mouseY);

    // Select an option when the mouse moves onto it.
    if (hoveredOption != this.lastHoveredOption) {
        this.lastHoveredOption = hoveredOption;

        if (hoveredOption != -1
                && hoveredOption != this.selectedOption) {
            this.selectedOption = hoveredOption;
            AudioManager.playSFX("audio/sfx/menu_move.wav");
        }
    }

    boolean mousePressed = inputManager.isMousePressed();
    boolean newMouseClick = mousePressed && !this.wasMousePressed;
    this.wasMousePressed = mousePressed;

    if (newMouseClick && this.inputDelay.checkFinished()) {
        // Start dragging if the click begins on a volume bar.
        if (hoveredBar != -1) {
            this.selectedOption = hoveredBar;
            this.draggedVolumeOption = hoveredBar;
            this.selectionCooldown.reset();
        } else if (hoveredOption == MUTE_OPTION) {
            this.selectedOption = MUTE_OPTION;
            toggleMute();
            this.selectionCooldown.reset();
        }
    }

    // Update the volume continuously while dragging.
    if (mousePressed && this.draggedVolumeOption != -1
            && this.inputDelay.checkFinished()) {

        int volume = drawManager.getAudioSettingsVolumeFromMouseX(
                this, mouseX);

        if (this.draggedVolumeOption == BGM_VOLUME_OPTION) {
            setBGMVolumeFromMouse(volume);
        } else if (this.draggedVolumeOption == SFX_VOLUME_OPTION) {
            setSFXVolumeFromMouse(volume);
        }
    }

    // Stop dragging when the mouse button is released.
    if (!mousePressed) {
        this.draggedVolumeOption = -1;
    }
}


/**
 * Sets the BGM volume from mouse input.
 */
private void setBGMVolumeFromMouse(final int volume) {
    int clampedVolume = Math.max(MIN_VOLUME,
            Math.min(MAX_VOLUME, volume));

    if (AudioManager.getBGMVolume() != clampedVolume) {
        AudioManager.setBGMVolume(clampedVolume);
        AudioControls.saveBGMVolume();
    }
}

/**
 * Sets the SFX volume from mouse input.
 */
private void setSFXVolumeFromMouse(final int volume) {
    int clampedVolume = Math.max(MIN_VOLUME,
            Math.min(MAX_VOLUME, volume));

    if (AudioManager.getSFXVolume() != clampedVolume) {
        AudioManager.setSFXVolume(clampedVolume);
        AudioControls.saveSFXVolume();
    }
}


	/**
	 * Shifts the focus to the next menu option.
	 * Wraps around to the first option after the last one.
	 */
	private void nextOption() {
		this.selectedOption = (this.selectedOption + 1) % OPTION_COUNT;
	}

	/**
	 * Shifts the focus to the previous menu option.
	 * Wraps around to the last option before the first one.
	 */
	private void previousOption() {
		this.selectedOption = (this.selectedOption - 1 + OPTION_COUNT) % OPTION_COUNT;
	}

	/**
	 * Changes the background music volume.
	 * The result is kept between the lowest and the highest volume, and is
	 * saved so it is kept after the game exits.
	 *
	 * @param amount
	 *            Amount to add to the volume. Negative to lower it.
	 */
	private void changeBGMVolume(final int amount) {
		int volume = AudioManager.getBGMVolume() + amount;
		volume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
		AudioManager.setBGMVolume(volume);
		AudioControls.saveBGMVolume();
	}

	/**
	 * Changes the sound effect volume.
	 * The result is kept between the lowest and the highest volume, and is
	 * saved so it is kept after the game exits.
	 *
	 * @param amount
	 *            Amount to add to the volume. Negative to lower it.
	 */
	private void changeSFXVolume(final int amount) {
		int volume = AudioManager.getSFXVolume() + amount;
		volume = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
		AudioManager.setSFXVolume(volume);
		AudioControls.saveSFXVolume();
	}

	/**
	 * Turns the global mute on if it is off, and off if it is on.
	 * The volume levels are kept, so they come back when unmuted.
	 * The mute state is saved so it is kept after the game exits.
	 */
	private void toggleMute() {
		AudioManager.setMuted(!AudioManager.isMuted());
		AudioControls.saveMuted();
	}

	/**
	 * Draws the elements associated with the screen.
	 * The current settings are read from the AudioManager.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawAudioSettings(this, this.selectedOption,
				AudioManager.getBGMVolume(), AudioManager.getSFXVolume(),
				AudioManager.isMuted());

		drawManager.completeDrawing(this);
	}
}
