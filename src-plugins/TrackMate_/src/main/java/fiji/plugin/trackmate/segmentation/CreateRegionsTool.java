package fiji.plugin.trackmate.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.StackWindow;
import ij.gui.Toolbar;

import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;

public class CreateRegionsTool extends AbstractTool implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ToolWithOptions {

	private static final boolean DEBUG = false;

	private static final String TOOL_NAME = "Create Regions Tool";
	private static final String TOOL_ICON ="CdffL10e0"
			+ "CafdD01C67aD11C6aaD21C6fbL31e1CbfeDf1"
			+ "CaedD02C64aD12C35aD22C5fbD32C6fbL42e2CbfdDf2"
			+ "CafdD03C5eaD13C4bbD23C38bD33C5daD43C6fbL53e3CbfdDf3"
			+ "CafdD04C6fbL1424C48aD34C42aD44C5caD54C6fbL64e4CbfdDf4"
			+ "CafdD05C6fbL1525C5caD35C35aD45C4bbD55C6fbL65e5CbfdDf5"
			+ "CafdD06C6fbL1636C5dbD46C38bD56C6fbL66e6CbfdDf6"
			+ "CafdD07C6fbL1747C38bD57C49aD67C6fbL77e7CbfdDf7"
			+ "CafdD08C6fbL1848C58aD58C31aD68C5baD78C6fbL88e8CbfdDf8"
			+ "CafdD09C6fbL1949C4abD59C37bD69C38bD79C4bbD89C5eaD99C6fbLa9e9CbfdDf9"
			+ "CafdD0aC6fbL1a4aC39bD5aC5ebL6a7aC44aD8aC43aD9aC39bDaaC46aDbaC55aDcaC6fbLdaeaCbfdDfa"
			+ "CafdD0bC6fbL1b3bC58aD4bC36aD5bC6fbL6b7bC5aaD8bC59aD9bC5dbDabC47aDbbC34aDcbC5cbDdbC6fbDebCbfdDfb"
			+ "CafdD0cC6fbL1c2cC5cbD3cC33aD4cC55aD5cC6fbL6cbcC5dbDccC38bDdcC5ebDecCbfdDfc"
			+ "CafdD0dC58aD1dC47aD2dC38bD3dC5cbD4dC6eaD5dC6fbL6dcdC49aDddC43aDedCbddDfd"
			+ "CafdD0eC64aD1eC45aD2eC5fbD3eC6fbL4eceC5baDdeC65aDeeCbddDfe"
			+ "CeffD0fCbedD1fCbeeD2fCcfeL3fdfCbfeDefCeffDff";

	private CreateRegionsToolConfigPanel configPanel;
	private Logger logger = Logger.VOID_LOGGER;
	
	/** The singleton instance. */
	private static CreateRegionsTool instance;
	
	/** Stores the view possible attached to each {@link ImagePlus}. */
	HashMap<ImagePlus, StackWindow> displayers = new HashMap<ImagePlus, StackWindow>();
	RegionOverlay ro;
	
	/**
	 * The last {@link ImagePlus} on which an action happened.
	 */
	ImagePlus imp;
	private double oldMagnification = 1.0;

	
	/**
	 * RegionEditing Variables
	 */
	
	boolean startedCreatingRegion = false;
	List<Point> actualRegion = new ArrayList<Point>();
	Point firstPointOfRegion = null;
	Point selectedPoint = null;
	
	List<List<Point>> allRegions = new ArrayList<List<Point>>();
	int pointClickRadius = 5;
	
	
	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	private CreateRegionsTool() {	
		
	}

	/**
	 * Return the singleton instance for this tool. If it was not previously instantiated, this calls
	 * instantiates it. 
	 */
	public static CreateRegionsTool getInstance() {
		if (null == instance) {
			instance = new CreateRegionsTool();
			if (DEBUG)
				System.out.println("[CreateRegionTool] Instantiating: "+instance);
		}
		if (DEBUG)
			System.out.println("[CreateRegionTool] Returning instance: "+instance);
		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched() {
		Toolbar toolbar = Toolbar.getInstance();
		if (null != toolbar && toolbar.getToolId(TOOL_NAME) >= 0) 
			return true;
		return false;
	}

	/*
	 * METHODS
	 */

	@Override
	public String getToolName() {
		return TOOL_NAME;
	}	

	@Override
	public String getToolIcon() {
		return TOOL_ICON;
	}
	
	/**
	 * Overridden so that we can keep track of the last ImagePlus actions are taken on. 
	 * Very much like ImageJ.
	 */
	@Override
	public ImagePlus getImagePlus(ComponentEvent e) {
		imp = super.getImagePlus(e); 
		return imp;
	}
	
	@Override
	public void imageUpdated(ImagePlus imp) {
		super.imageUpdated(imp);
		this.imp = imp;
	}

	/**
	 * Register the given {@link HyperStackDisplayer}. If this method id not called, the tool will not
	 * respond.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
	 */
	public void register(final ImagePlus imp, final StackWindow displayer, RegionOverlay ro) {
		if (DEBUG) System.out.println("[CreateRegionTool] Currently registered: " + displayers);

		if (displayers.containsKey(imp)) {
			unregisterTool(imp);
			if (DEBUG) System.out.println("[CreateRegionTool] De-registering " + imp + " as tool listener.");
		}

		displayers.put(imp, displayer);
		if (DEBUG) {
			System.out.println("[CreateRegionTool] Registering "+imp+" and "+displayer + "." +
					" Currently registered: " + displayers);
		} 
		
		this.ro = ro;
		
	}

	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked(MouseEvent e) {

		final ImagePlus imp = getImagePlus(e);
		final StackWindow displayer = displayers.get(imp);
		if (DEBUG) {
			System.out.println("[CreateRegionTool] @mouseClicked");
			System.out.println("[CreateRegionTool] Got "+imp+ " as ImagePlus");
			System.out.println("[CreateRegionTool] Matching displayer: "+displayer);
			System.out.println("[CreateRegionTool] ClickLocation: ("+e.getPoint().x+","+e.getPoint().y+")");
			
			for (MouseListener ml : imp.getCanvas().getMouseListeners()) {
				System.out.println("[CreateRegionTool] mouse listener: "+ml);
			}

		}

		if (null == displayer)
			return;
				
		double mag = ro.getMagnification();
		Point clickedPoint = new Point((int)((double)e.getPoint().x/mag),(int)((double)e.getPoint().y/mag));
						
		if (mag!=oldMagnification) {
			oldMagnification = mag;
			selectedPoint = null;
			return;
		}
		
		if (!startedCreatingRegion) {
						
			Point pointInClickRadius = getPointInClickRadius(clickedPoint);
			if (pointInClickRadius!=null) {
				// Click to a point
				
				// Select this point 
				selectedPoint = pointInClickRadius;
				
			} else {
				// First Click of Region

				actualRegion = new ArrayList<Point>();
								
				actualRegion.add(clickedPoint);
				firstPointOfRegion = clickedPoint;
				startedCreatingRegion = true;
				
				selectedPoint = null;
			}
			
		} else {
			
			if (firstPointOfRegion.distance(clickedPoint)<pointClickRadius) {
				if (actualRegion.size()>=3) {
					// If Region is finished: Add Region to AllRegions

					allRegions.add(actualRegion);	
					actualRegion = null;
					startedCreatingRegion = false;
					selectedPoint = null;
				} else {
					// Region with less then 3 Point is just a Point or a line
					//TODO What should be done here???
				}
				
				
				
				
			} else { //TODO Started Creating Region but selected another Point
				
				// If Region isn't finished: Add Point to Region
				actualRegion.add(clickedPoint);
			}
			
		}
		
		updateImageCanvas();
		
		
	}



	@Override
	public void mousePressed(MouseEvent e) {}


	@Override
	public void mouseReleased(MouseEvent e) {}


	@Override
	public void mouseEntered(MouseEvent e) {}


	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {

		if (DEBUG) {
			System.out.println("[CreateRegionTool] @mouseDragged");
			System.out.println("[CreateRegionTool] ClickLocation: ("+e.getPoint().x+","+e.getPoint().y+")");
		}
		
		double mag = ro.getMagnification();
		
		if (selectedPoint!=null) {
			final ImagePlus imp = getImagePlus(e);
			// Point is selected
			final double[] calibration = TMUtils.getSpatialCalibration(imp);
			final StackWindow displayer = displayers.get(imp);
			if (null == displayer)
				return;
			
			if (DEBUG) {
				System.out.println("[CreateRegionTool] Got "+imp+ " as ImagePlus");
				System.out.println("[CreateRegionTool] Matching displayer: "+displayer);
				
				for (MouseListener ml : imp.getCanvas().getMouseListeners()) {
					System.out.println("[CreateRegionTool] mouse listener: "+ml);
				}
	
			}
											
			Point mouseLocation = new Point((int)((double)e.getPoint().x/mag),(int)((double)e.getPoint().y/mag));
		//	ImageCanvas canvas = getImageCanvas(e);
		//	double x = (-0.5 + canvas.offScreenXD(mouseLocation.x) ) * calibration[0];
		//	double y = (-0.5 + canvas.offScreenYD(mouseLocation.y) ) * calibration[1];
		//	selectedPoint.x = (int)x;
		//	selectedPoint.y = (int)y;
		
			selectedPoint.x = mouseLocation.x;
			selectedPoint.y = mouseLocation.y;
			
			displayer.getImagePlus().updateAndDraw();
			updateStatusBar(selectedPoint, imp.getCalibration().getUnits());	
		
			
		} else {
			if (DEBUG) {
				System.out.println("[CreateRegionTool] No Point selected");
			}
		}
		updateImageCanvas();
	}

	@Override
	public void mouseMoved(MouseEvent e) {	}


	/*
	 * MOUSEWHEEL 
	 */

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyPressed(KeyEvent e) { 

		if (DEBUG) 
			System.out.println("[CreateRegionsTool] keyPressed: "+e.getKeyChar());

		final ImagePlus imp = getImagePlus(e);
		if (imp == null)
			return;
		final StackWindow displayer = displayers.get(imp);
		if (null == displayer)
			return;

		int keycode = e.getKeyCode(); 

		switch (keycode) {

			// Delete currently edited or the selected Region
			case KeyEvent.VK_DELETE: {
				if (selectedPoint!=null) {
					// Delete Selected Region
					for (List<Point> region : allRegions) {
						if (region.contains(selectedPoint)) {
							allRegions.remove(region);
						}
					}
				} else if(startedCreatingRegion) {
					actualRegion = null;
					startedCreatingRegion = false;
				}
				
				e.consume();
				break;
			}
	
			// Quick add spot at mouse
			case KeyEvent.VK_A:
			case KeyEvent.VK_D:
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_Q:
			case KeyEvent.VK_E:
			case KeyEvent.VK_V:
			case KeyEvent.VK_L:
			case KeyEvent.VK_W: {
				e.consume(); // consume it: we do not want IJ to close the window
				break;
			}

		}
		updateImageCanvas();
	}

	@Override
	public void keyReleased(KeyEvent e) { 	}	


	/*
	 * PRIVATE METHODS
	 */

	private void updateStatusBar(final Point point, final String units) {
		if (null == point)
			return;
		String statusString = "";
		statusString = "Point ("+point.x+", "+point.y+")";
		IJ.showStatus(statusString);
	}
	

	@Override
	public void showOptionDialog() {
		if (null == configPanel) {
			configPanel = new CreateRegionsToolConfigPanel(this);
			configPanel.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					logger = Logger.VOID_LOGGER;
				}
			});
		}
		if (DEBUG) {
			System.out.println("[CreateRegionTool] OptionDialogShown: "+configPanel.toString());
		}
		configPanel.setLocation(toolbar.getLocationOnScreen());
		configPanel.setVisible(true);
		logger = configPanel.getLogger();
	}
	
	
	public void updateImageCanvas() {	
				
		
	//	for (int i=0; i<imp.getOverlay().size(); i++) {
	//		if (imp.getOverlay().get(i) instanceof RegionOverlay) {
	//			RegionOverlay ro = (RegionOverlay) imp.getOverlay().get(i);
		ro.setCreateRegion(actualRegion);
		ro.setSelectedPoint(selectedPoint);
		ro.setRegions(allRegions);
		//	}
		//}
		imp.updateAndDraw();
		
	}
	
	private Point getPointInClickRadius(Point p) {
		for (List<Point> region : allRegions) {
			for (Point point : region) {
				if (p.distance(point)<pointClickRadius) {
					return point;
				}
			}
		}
		return null;
	}

	public List<List<Point>> getAllRegions() {
		return allRegions;
	}
	
	public void reset() {
		actualRegion = new ArrayList<Point>();
		firstPointOfRegion = null;
		selectedPoint = null;
		
		allRegions = new ArrayList<List<Point>>();
	}
	
}
