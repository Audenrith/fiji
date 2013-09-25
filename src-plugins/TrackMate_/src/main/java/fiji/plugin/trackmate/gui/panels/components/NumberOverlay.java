package fiji.plugin.trackmate.gui.panels.components;

import ij.ImagePlus;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.descriptors.RegionSelectDescriptor;
import fiji.util.gui.OverlayedImageCanvas.Overlay;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack window.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010 - 2011
 */
public class NumberOverlay implements Overlay {

	/** The color mapping of the target collection. */
	protected final ImagePlus imp;
	protected Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	protected FontMetrics fm;
	protected final LogPanel logger;

	private Regions regions;
	
	/*
	 * CONSTRUCTOR
	 */

	public NumberOverlay(final LogPanel logger, final ImagePlus imp){//, final Map<String, Object> displaySettings) {
		
		this.logger = logger;
		this.imp = imp;
		
		computeNumberPoints();		
	}

	/*
	 * METHODS
	 */

	public void update(Map<String, List<Integer>> regionNumbers) {
		
		for (int j=0; j<RegionSelectDescriptor.REGION_STRINGS.length; j++) {
			List<Integer> targets = regionNumbers.get(RegionSelectDescriptor.REGION_STRINGS[j]);
			
			if (targets!=null) {
				for (int i=0; i<targets.size(); i++) {
					regions.reindexRegion(targets.get(i), RegionSelectDescriptor.REGION_INDEXES[j]);
				}
			}
		}
				
	}
	
	
	@Override
	public void paint(Graphics g, int xcorner, int ycorner, double magnification) {
		
		final Graphics2D g2d = (Graphics2D)g;
		// Save graphic device original settings
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();
		
		regions.drawRegionNumbers(g2d, xcorner, ycorner, magnification);
		
		// Restore graphic device original settings
		g2d.setColor(originalColor);
		g2d.setFont(originalFont);
		
	}
	
	public void computeNumberPoints() {
		// Compute Region Numbers and averages
		int[] pixels = (int[])imp.getImageStack().getPixels(1);
		int width = imp.getImageStack().getWidth();
		int height = imp.getImageStack().getHeight();	
				
		regions = new Regions(4,4, width, height);		
		
		int i=0;
		for (int col = 0; col < width; col++) {
			for (int row = 0; row<height; row++) {
				regions.addPoint(pixels[i], row, col);
				i++;
			}
		}	
		
	}
	
	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}
	
	private class Regions {
		
		private List<Integer> regionNumbers = new ArrayList<Integer>();
		private List<String> regionIndex = new ArrayList<String>();
		private List<Integer[]> xSums = new ArrayList<Integer[]>();
		private List<Integer[]> ySums = new ArrayList<Integer[]>();
		private List<Integer[]> pointNumber = new ArrayList<Integer[]>();
		private int xParts, yParts;		
		private int width, height;
		
		
		public Regions(int xParts, int yParts, int width, int height) {
			this.xParts = xParts;
			this.yParts = yParts;
			Integer arr[] = new Integer[xParts*yParts];
			for (int i=0; i<arr.length; i++) {
				arr[i] = 0;
			}
			
			this.width = width;
			this.height = height;
			
			xSums.add(arr.clone());
			ySums.add(arr.clone());
			pointNumber.add(arr.clone());
			
			logger.getLogger().log(xSums.size()+" - "+ySums.size()+" - "+pointNumber.size()+"\n");
		}
		
		public void addPoint(int regionNumber, int x, int y) {
			
			int index = -1;
						
			if (this.regionNumbers.contains(regionNumber)) {
				// Get Index if present
				index = regionNumbers.indexOf(regionNumber);
			} else {
				// Else: Add new RegionNumber and refactor all other variables
				regionNumbers.add(regionNumber);
				index = regionNumbers.indexOf(regionNumber);
				Integer arr[] = new Integer[xParts*yParts];
				for (int i=0; i<arr.length; i++) {
					arr[i] = 0;
				}
				
				while (index>=xSums.size()) {
					
					xSums.add(arr.clone());
					ySums.add(arr.clone());
					pointNumber.add(arr.clone());				
				}
				while (index>=regionIndex.size()) {
					regionIndex.add("");
				}
				
				xSums.set(index, arr.clone());
				ySums.set(index, arr.clone());
				pointNumber.set(index, arr.clone());
			}
			
			
			if (index!=-1) {
				
				int partX = x*xParts/width;
				int partY = y*yParts/height;
				
				Integer[] xSum = xSums.get(index);
				xSum[partX*xParts+partY] +=x;
				xSums.set(index, xSum);
				
				Integer[] ySum = ySums.get(index); 
				ySum[partX*xParts+partY] +=y;
				ySums.set(index, ySum);
				
				Integer[] points = pointNumber.get(index); 
				points[partX*xParts+partY] ++;
				pointNumber.set(index, points);
								
			} else {
				throw new IllegalArgumentException("False RegionNumber");
			}
			
		}
		
		public void drawRegionNumbers(Graphics2D g2d, int xcorner, int ycorner, double magnification) {
			
			for (int index=0; index<regionNumbers.size(); index++) {
				
				Integer[] points = pointNumber.get(index);				
				int maxIndex = 0;
				int number = points[0];
				for (int i=1; i<points.length; i++) {
					if (points[i]>number) {
						number = points[i];
						maxIndex = i;
					}
				}

				Integer[] xSum = xSums.get(index); 
				Integer[] ySum = ySums.get(index); 
				
				int xPos = (int) (xSum[maxIndex]*magnification/number);
				int yPos = (int) (ySum[maxIndex]*magnification/number);
				
				g2d.setColor(Color.black);
				if (regionIndex.get(index)==null || regionIndex.get(index).equals("")) {
					g2d.fillRect(xPos-(int)(xcorner*magnification)-1, 
							yPos-(int)(ycorner*magnification)-((int)(9*magnification)), 
							(int)(8*(1+index/10)*magnification), (int)(9*magnification));					
				} else {
					g2d.fillRect(xPos-(int)(xcorner*magnification)-1, 
							yPos-(int)(ycorner*magnification)-(int)(9*magnification), 
							(int)((8*(1+index/10)+6)*magnification), (int)(9*magnification));
				}				
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, (int) (12*magnification)));
				g2d.drawString(regionNumbers.get(index)+regionIndex.get(index), 
						xPos-(int)(xcorner*magnification), yPos-(int)(ycorner*magnification));
			}
		}
		
		public void reindexRegion(int target, String targetIndex) {
			int index = -1;
			
			if (this.regionNumbers.contains(target)) {
				index = regionNumbers.indexOf(target);
				regionIndex.set(index, targetIndex);
			}
		}
		
				
	}
	


}