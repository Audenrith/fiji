package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class RegionOverlay extends Roi {

	private static final long serialVersionUID = 1L;
	private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
	private static final boolean DEBUG = false;
	
	private static final Color NORMAL_COLOR = new Color(100, 100, 255);
	private static final Color SELECTED_COLOR = new Color(255, 100, 100);
	private static final Color START_COLOR = new Color(255, 255, 0);

	protected Point selectedPoint;
	protected Point startPoint;
	protected final double[] calibration;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected List<List<Point>> completeRegions = new ArrayList<List<Point>>();
	protected List<Point> createRegion = new ArrayList<Point>();
	protected int radius;
	
	/*
	 * CONSTRUCTOR
	 */

	public RegionOverlay(final List<List<Point>> allRegions, final ImagePlus imp, int radius) {
		super(0, 0, imp);
		this.completeRegions = allRegions;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration(imp);
		this.radius = radius;
	}

	/*
	 * METHODS
	 */


	@Override
	public void drawOverlay(Graphics g) {
		
		if (completeRegions==null && createRegion == null) {
			return;
		}
		
		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();

		double mag = this.getMagnification();
		
		g2d.setComposite(composite);
		g2d.setFont(LABEL_FONT);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		fm = g2d.getFontMetrics();

		g2d.setStroke(new BasicStroke(1.0f));
		
		if (completeRegions!=null) {
			for (List<Point> region : completeRegions) {
				if (region.contains(selectedPoint)) {
					// Deal with selected Region
					g2d.setColor(SELECTED_COLOR);
					
					Point startpoint = region.get(0);
					Point present;
					Point next;
									
					for (int i=0; i<region.size()-1; i++) {
						present = region.get(i);
						next = region.get(i+1);
						g2d.drawLine((int)(present.x*mag), (int)(present.y*mag), (int)(next.x*mag), (int)(next.y*mag));
						if (present.equals(selectedPoint)) {
							g2d.drawOval((int)((present.x-radius)*mag), (int)((present.y-radius)*mag), 2*(int)(radius*mag), 2*(int)(radius*mag));
						}
					}
					present = region.get(region.size()-1);
				//	g2d.drawLine(present.x, present.y, startpoint.x, startpoint.y);
					g2d.drawLine((int)(present.x*mag), (int)(present.y*mag), (int)(startpoint.x*mag), (int)(startpoint.y*mag));
					if (present.equals(selectedPoint)) {
						//g2d.drawOval(present.x-radius, present.y-radius, 2*radius, 2*radius);
						g2d.drawOval((int)((present.x-radius)*mag), (int)((present.y-radius)*mag), 2*(int)(radius*mag), 2*(int)(radius*mag));
					}
					
				} else {
					// Deal with normal Points
					g2d.setColor(NORMAL_COLOR);
					
					Point startpoint = region.get(0);
					Point present;
					Point next;
									
					for (int i=0; i<region.size()-1; i++) {
						present = region.get(i);
						next = region.get(i+1);
						//g2d.drawLine(present.x, present.y, next.x, next.y);
						g2d.drawLine((int)(present.x*mag), (int)(present.y*mag), (int)(next.x*mag), (int)(next.y*mag));
					}
					present = region.get(region.size()-1);
					//g2d.drawLine(present.x, present.y, startpoint.x, startpoint.y);
					g2d.drawLine((int)(present.x*mag), (int)(present.y*mag), (int)(startpoint.x*mag), (int)(startpoint.y*mag));
					
				}
					
				
				
			}
		}
		if (createRegion != null) {
			// Deal with created Region
			
			g2d.setColor(START_COLOR);
			
			Point present;
			Point next;
							
			for (int i=0; i<createRegion.size()-1; i++) {
				present = createRegion.get(i);
				next = createRegion.get(i+1);
				//g2d.drawOval(present.x-radius, present.y-radius, 2*radius, 2*radius);
				//g2d.drawLine(present.x, present.y, next.x, next.y);
				g2d.drawOval((int)((present.x-radius)*mag), (int)((present.y-radius)*mag), 2*(int)(radius*mag), 2*(int)(radius*mag));
				g2d.drawLine((int)(present.x*mag), (int)(present.y*mag), (int)(next.x*mag), (int)(next.y*mag));
			}
			if (createRegion.size()>0) {
				present = createRegion.get(createRegion.size()-1);
				//g2d.drawOval(present.x-radius, present.y-radius, 2*radius, 2*radius);
				g2d.drawOval((int)((present.x-radius)*mag), (int)((present.y-radius)*mag), 2*(int)(radius*mag), 2*(int)(radius*mag));
			}
				
		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite(originalComposite);
		g2d.setStroke(originalStroke);
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
	}
	
	

	public void setRegions(List<List<Point>> completeRegions) {
		this.completeRegions = completeRegions;
	}

	public Point getSelectedPoint() {
		return selectedPoint;
	}

	public void setSelectedPoint(Point point) {
		selectedPoint = point;
	}


	public void setCreateRegion(List<Point> createRegion) {
		this.createRegion = createRegion;
	}
	
	public double getMagnification() {
		return super.getMagnification();
	}

}