package fiji.plugin.trackmate.visualization.orthogonalview;

import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * Overlay class to have all Images positioned to the same location in the Image stack
 * @author Gby
 *
 */
public class PositionOverlay extends Roi{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
//	private static final boolean DEBUG = false;
	
	
	
	public static int radius = 4;
	
	/** The color mapping of the target collection. */
	protected Map<Spot, Color> targetColor;
	protected Spot editingSpot;
	protected final ImagePlus imp;
	protected final double[] calibration;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected Collection<Spot> spotSelection = new ArrayList<Spot>();
	protected Map<String, Object> displaySettings;
	protected final Model model;

	protected int focusX,focusY,focusZ;
	protected Direction direction;
		
	/*
	 * CONSTRUCTOR
	 */

	public PositionOverlay(final Model model, final ImagePlus imp, final Map<String, Object> displaySettings, Direction direction) {
		super(0, 0, imp);
		this.model = model;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration(imp);
		this.displaySettings = displaySettings;
		this.direction = direction;
	}

	/*
	 * METHODS
	 */

	public void setPosition(int x, int y, int t) {
		this.focusX = direction.x(x,y,t);
		this.focusY = direction.y(x,y,t);
		this.focusZ = direction.z(x,y,t);
		//imp.setPosition(focusZ+1);
		//imp.setPositionWithoutUpdate(1, focusZ+1, 1);
	}
	
	public void update() {
		imp.setPosition(focusZ+1);
	}
	
	
	

	@Override
	public void drawOverlay(Graphics g) {
		
		mag = getMagnification();

		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();

		g2d.setComposite(composite);
		g2d.setFont(LABEL_FONT);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		fm = g2d.getFontMetrics();
				
		// Draw Oval of specific Position
		g2d.drawOval(focusX-radius, focusY-radius, 2*radius, 2*radius);
		
		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
		
	}

	public boolean isImp(ImagePlus imp) {
		return this.imp.equals(imp);
	}
	
	
	/**
	 * Enum which transforms the original coordinates to those needed by the Overlay
	 * @author Gby
	 *
	 */
	public enum Direction {
		VERTICAL(1,3,2,"Vertical"), HORIZONTAL(3,2,1,"Horizontal"), ORIGINAL(1,2,3,"Original");
		
		private int x,y,z;
		protected String name;
		
		Direction(int x, int y, int z, String name) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.name= name;
		}
		
		public int x(int x, int y, int z) {
			switch (this.x) {
				case 1:
					return x;
				case 2:
					return y;
				case 3:
					return z;
				default:
					return x;
			}
		}
		public int y(int x, int y, int z) {
			switch (this.y) {
				case 1:
					return x;
				case 2:
					return y;
				case 3:
					return z;
				default:
					return y;
			}
		}
		public int z(int x, int y, int z) {
			switch (this.z) {
				case 1:
					return x;
				case 2:
					return y;
				case 3:
					return z;
				default:
					return z;
			}
		}
	}
}