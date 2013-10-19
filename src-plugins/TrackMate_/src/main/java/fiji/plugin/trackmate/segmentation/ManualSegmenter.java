package fiji.plugin.trackmate.segmentation;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.StackWindow;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.util.gui.OverlayedImageCanvas;

public class ManualSegmenter implements Segmenter{
	
	
	public static final String SEGMENTER_KEY = "MANUAL_SEGMENTER";
	public static final String NAME = "Manual Segmenter";
	public static final String INFO_TEXT = "<html>" +
			"This Segmenter is a manual Segmenter. <br>" +
			"By clicking on the Segmentation-Window you can create a polygone <br>" +
			"which will be associated as a region." +
			"</html>";

	private final static String BASE_ERROR_MESSAGE = "MANUAL_SEGMENTER: ";
	private String errorMessage = "";
	
	/** Logger used to echo progress on tracking. */
	protected final Logger logger;
	
	/** Data model of the Segmentation */
	protected final Model model;
	
	/** Image */
	protected ImagePlus regionImage;
	protected ImagePlus image;
	
	/** StackWindow */
	private StackWindow window;
	private CreateRegionsTool editTool;
	private OverlayedImageCanvas canvas;
	List<List<Point>> regions;
	
	public ManualSegmenter(Model model, ImagePlus image, List<List<Point>> regions) {
		this.logger = model.getLogger();
		this.model = model;
		this.image = image;
		this.regions = regions;
	}

	/**
	 * Divides the Image in two parts. These parts should be segmented afterwards
	 * @param image to binarize
	 * @return binarized Image
	 */
	private ImagePlus createRegions() {
		ImageStack stack = image.getStack();
		
		int width = stack.getWidth();
		int height = stack.getHeight();

		int[][] pixel = new int[width][height];
		
		for (int col = 0; col<width; col++) {
			for (int row=0; row<height; row++) {
				pixel[col][row] = 0;
			}
		}
		
		List<List<Point>> regions = editTool.getAllRegions();
		
		if (regions!=null) {
			
			int color = 0;
			
			for (List<Point> region : regions) {
				// For every Region create the border
				color++;
				
				if (region.size()>1) {
				
					pixel = createRegionBorder(region, pixel, color);

				}
			}
			

			color = 0;
			
			for (List<Point> region : regions) {
				// Fill every region
				color++;
				
				if (region.size()>1) {
					Point p = findInsidePixel(region, pixel, color);
					
					if (p!=null) {
						pixel = colorRegion(p, pixel, color);						
					}
				}
			}
		}
		
		int[] stackPixel = new int[width*height];
		
		for (int col = 0; col<width; col++) {
			for (int row=0; row<height; row++) {
				stackPixel[col+row*width] = pixel[col][row];
			}
		}
		
		
		
		ImageStack segStack = new ImageStack(width,height);
		segStack.addSlice("regions", stackPixel);
		
		return new ImagePlus("RegionImage",segStack); 
		
	}
	
	private int[][] createRegionBorder(List<Point> region, int[][] pixel, int color) {
		
		int xstart=-1, ystart=-1, xend=-1, yend=-1, dx=0, dy=0; 
		double xdouble=0, ydouble = 0;
		try{
		for (int i=0; i<region.size(); i++) {
			// For every Point in every Region
			xstart = region.get(i).x;
			ystart = region.get(i).y;
			if (i<region.size()-1) {
				xend = region.get(i+1).x;
				yend = region.get(i+1).y;				
			} else {
				xend = region.get(0).x;
				yend = region.get(0).y;
			}
			
			
			// caseSwitching
			dx = xend-xstart;
			dy = yend-ystart;
			
			if (Math.abs(dx)>Math.abs(dy)) {
				int x = xstart;
				double y = ystart;
				
				int xadd = ((int)Math.signum(dx));
				double yadd = (double)dy/Math.abs(dx);
				
				while (x!=xend) {
					pixel[x][round(y)] = color;
					x += xadd;
					y += yadd;
					
					xdouble = x;
					ydouble = y;
				}
				
			} else {
				double x = xstart;
				int y = ystart;
				
				double xadd = (double)dx/Math.abs(dy);
				int yadd = ((int)Math.signum(dy));
				
				while (y!=yend) {
					pixel[round(x)][y] = color;
					x += xadd;
					y += yadd;
					
					xdouble = x;
					ydouble = y;
				}
			}
			
		}
		} catch (ArrayIndexOutOfBoundsException e){
			e.printStackTrace();
			System.out.println(xstart+"("+dx+"): "+xdouble+" - "+xend+" ; "+ystart+"("+dy+"): "+ydouble+" - "+yend);
		}
		return pixel;
	}
	
	private Point findInsidePixel(List<Point> region, int[][]pixel, int color) {
		
		int sumX = 0;
		int sumY = 0;
		
		for (Point point : region) {
			if (meetsNoColorPixel(point,pixel, 1,0,color) && !meetsNoColorPixel(point,pixel,-1,0,color)) {
				if (pixel[point.x-1][point.y] != color) {
					return new Point(point.x-1, point.y);
				}
			} else if (meetsNoColorPixel(point,pixel, 0,1,color) && !meetsNoColorPixel(point,pixel,-1,0,color)) {
				if (pixel[point.x][point.y-1] != color) {
					return new Point(point.x, point.y-1);
				}
			} else if (meetsNoColorPixel(point,pixel, -1,0,color) && !meetsNoColorPixel(point,pixel,-1,0,color)) {
				if (pixel[point.x+1][point.y] != color) {
					return new Point(point.x+1, point.y);
				}
			} else if (meetsNoColorPixel(point,pixel, 0,-1,color) && !meetsNoColorPixel(point,pixel,-1,0,color)) {
				if (pixel[point.x][point.y+1] != color) {
					return new Point(point.x, point.y+1);
				}
			}
			sumX+=point.x;
			sumY+=point.y;
		}
		
		sumX/=region.size();
		sumY/=region.size();
		
		Point center = new Point(sumX,sumY);
		
		if (!meetsNoColorPixel(center,pixel,1,0,color) &&
			!meetsNoColorPixel(center,pixel,-1,0,color) &&
			!meetsNoColorPixel(center,pixel,0,1,color) &&
			!meetsNoColorPixel(center,pixel,0,-1,color)) {
			return center;
		}
		
		return null;
	}
	
	
	/**
	 * Try to find a pixel with the specified color if you go from pixel p in direction (dx,dy)
	 * @return no pixel found
	 */
	private boolean meetsNoColorPixel(Point p, int[][] pixel, int dx, int dy, int color) {
		int x = p.x+dx;
		int y = p.y+dy;
		
		while(x>=0 && x<pixel.length && y>=0 && y<pixel[0].length) {
			
			if (pixel[x][y] == color) {
				return false;
			} else {
				x +=dx;
				y +=dy;
			}
			
		}
		
		return true;
		
	}
		
	private int round(double d) {
		if (Math.abs(d-(int)(d))<0.5) {
			return (int)(d);
		} else {
			return (int)(d+Math.signum(d));
		}
	}
	
	
	private int[][] colorRegion(Point p, int[][]pixel, int color) {
			
		pixel[p.x][p.y] = color;
		
		ArrayList<Point> testPoints = new ArrayList<Point>();
		testPoints.add(p);
		
		while (testPoints.size()>0) {
			Point point = testPoints.get(0); 
			
			int x = point.x;
			int y = point.y; 
			
			if (x-1>0 && pixel[x-1][y] != color && pixel[x-1][y] == 0) {
				testPoints.add(new Point(x-1,y));
				pixel[x-1][y] = color;
			} 
			if (x+1 < pixel.length && pixel[x+1][y] != color && pixel[x+1][y] == 0) {
				testPoints.add(new Point(x+1,y));
				pixel[x+1][y] = color;
			} 
			if (y-1 > 0 && pixel[x][y-1] != color && pixel[x][y-1] == 0) {
				testPoints.add(new Point(x,y-1));
				pixel[x][y-1] = color;
			}
			if (y+1 < pixel[0].length && pixel[x][y+1] != color && pixel[x][y+1] == 0) {
				testPoints.add(new Point(x,y+1));
				pixel[x][y+1] = color;
			}
			
			testPoints.remove(0);
		}
				
		return pixel;
	}
	
	@Override
	public boolean checkInput() {
		return model!=null;
	}



	@Override
	public boolean process() {
		
		Roi roi = image.getRoi();
		if (roi!= null) {
			image.killRoi();
		}
		
		Overlay overlay = image.getOverlay();
		if (overlay == null) {
			overlay = new Overlay();
			image.setOverlay(overlay);
		}
		overlay.clear();
		if (roi != null) {
			image.getOverlay().add(roi);
		}
		
		if (null != image) {
			image.updateAndDraw();
		}
		
		
		image.setOpenAsHyperStack(true);
		if (!image.isVisible()) {
			image.show();
		}

		image.setOpenAsHyperStack(true);
		
		// TODO IS THIS NECCESSARY?
		/* 
		editTool = CreateRegionsTool.getInstance();
		if (!SpotEditTool.isLaunched()) {
			editTool.run("");
			editTool.imageUpdated(image); 
		} else {
			editTool.imageUpdated(image);
		}*/

		
		
		canvas = new OverlayedImageCanvas(image);
		canvas.addKeyListener(editTool);
		canvas.addMouseListener(editTool);
		canvas.addMouseMotionListener(editTool);
		canvas.addMouseWheelListener(editTool);
		
		RegionOverlay ro = new RegionOverlay(regions, image, editTool.pointClickRadius);
		
		
		window = new StackWindow(image, canvas);
		window.setTitle("Manual Segmentation");
		window.setVisible(true);
		window.setSize(image.getWidth()+200,image.getHeight()+200);
		
		editTool.register(image, window, ro);
		if (canvas.getOverlay()==null) {
			canvas.setOverlay(new Overlay());
		}
		canvas.getOverlay().add(ro);
		
		editTool.updateImageCanvas();
		
		return true;
	}



	@Override
	public String getErrorMessage() {
		return BASE_ERROR_MESSAGE+errorMessage;
	}



	@Override
	public String getKey() {
		return SEGMENTER_KEY;
	}


	@Override
	public ImagePlus getResult() {
		ImagePlus binImage = createRegions();
				
		regionImage = binImage;	
		
		return regionImage;
	}
	
	public void closeSegmentationWindow() {
		if (window!=null) {
			window.setVisible(false);
			window.close();
			window = null;
		}
		if (editTool!=null) {
			editTool.reset();
		}
	}
	
	public void hideSegmentationWindow() {
		if (window!=null) {
			window.setVisible(false);
		}
	}
	
}
