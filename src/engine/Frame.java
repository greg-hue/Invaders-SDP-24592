package engine;

import java.awt.Insets;

import javax.swing.JFrame;

import screen.Screen;

/**
 * Implements a frame to show screens on.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
@SuppressWarnings("serial")
public class Frame extends JFrame {

	/** Frame width. */
	private int width;
	/** Frame height. */
	private int height;
	/** Screen currently shown. */
	private Screen currentScreen;

	/**
	 * Initializes the new frame.
	 * 
	 * @param width
	 *               Frame width.
	 * @param height
	 *               Frame height.
	 */
	public Frame(final int width, final int height) {
		setSize(width, height);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setLocationRelativeTo(null);
		setVisible(true);

		Insets insets = getInsets();
		this.width = width - insets.left - insets.right;
		this.height = height - insets.top + insets.bottom;
		setTitle("Invaders");

		addKeyListener(Core.getInputManager());
		getContentPane().addMouseListener(Core.getInputManager());
		getContentPane().addMouseMotionListener(Core.getInputManager());
	}

	/** Exact client dimensions for the presentation, including its footer. */
	public final void setPresentationSize(final int contentWidth, final int contentHeight) {
		java.awt.Rectangle usable = java.awt.GraphicsEnvironment
				.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Insets borders = getInsets();
		getContentPane().setPreferredSize(new java.awt.Dimension(
				Math.min(contentWidth, usable.width - borders.left - borders.right),
				Math.min(contentHeight, usable.height - borders.top - borders.bottom)));
		pack();
		this.width = getContentPane().getWidth();
		this.height = getContentPane().getHeight();
		setLocationRelativeTo(null);
	}

	/**
	 * Sets current screen.
	 * 
	 * @param screen
	 *               Screen to show.
	 * @return Return code of the finished screen.
	 */
	public final int setScreen(final Screen screen) {
		currentScreen = screen;
		currentScreen.initialize();
		return currentScreen.run();
	}

	/**
	 * Getter for frame width.
	 * 
	 * @return Frame width.
	 */
	public final int getWidth() {
		return this.width;
	}

	/**
	 * Getter for frame height.
	 * 
	 * @return Frame height.
	 */

	public final int getHeight() {
		return this.height;
	}
}
