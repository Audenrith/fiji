package fiji.plugin.spottracker.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.spottracker.Featurable;
import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.TrackNode;

public abstract class SpotDisplayer <K extends Featurable> {

	/** The default display radius. */
	protected static final float DEFAULT_DISPLAY_RADIUS = 5;
	/** The default color. */
	protected static final Color DEFAULT_COLOR = new Color(1f, 0, 1f);
	/** The display radius. */
	protected float radius = DEFAULT_DISPLAY_RADIUS;
	
	/** The colorMap. */
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	/** The spot collections emanating from segmentation. */
	protected TreeMap<Integer,Collection<TrackNode<K>>> spots;
	/** The default color to paint the spots in. */ 
	protected Color color = DEFAULT_COLOR;
	/** If true, tracks will be displayed. */
	protected boolean displayTracks;


	
	public void setDisplayTracks(boolean displayTrack) {
		this.displayTracks = displayTrack;
	}	
	/*
	 * ABSTRACT METHODS
	 */
	
	/**
	 * Prepare this displayer and render it according to its concrete implementation.
	 */
	public abstract void render();
	
	/**
	 * Color all displayed spots according to the feature given. 
	 * If feature is <code>null</code>, then the default color is 
	 * used.
	 */
	public abstract void setColorByFeature(final Feature feature);
	
	/**
	 * Change the visibility of each spot according to the thresholds specified in argument.
	 */
	public abstract void refresh(final Feature[] features, double[] thresholds, boolean[] isAboves);
	
	/**
	 * Make all spots visible.
	 */
	public abstract void resetTresholds();
	
	
	/*
	 * PROTECTED METHODS
	 */
	
	
	/**
	 * Return the subset of spots of this displayer that satisfy the threshold conditions given
	 * in argument.
	 */
	protected TreeMap<Integer,Collection<TrackNode<K>>> threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		if (null == features || null == thresholds || null == isAboves)
			return spots;
		
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
		Collection<TrackNode<K>> spotThisFrame;
		TreeMap<Integer,Collection<TrackNode<K>>> spotsToshow = new TreeMap<Integer, Collection<TrackNode<K>>>();

		for (int key : spots.keySet()) {
			
			spotThisFrame = spots.get(key);
			ArrayList<TrackNode<K>> blobToShow = new ArrayList<TrackNode<K>>(spotThisFrame);
			ArrayList<TrackNode<K>> blobToHide = new ArrayList<TrackNode<K>>(spotThisFrame.size());

			TrackNode<K> blob;

			for (int i = 0; i < features.length; i++) {

				threshold = thresholds[i];
				feature = features[i];
				isAbove = isAboves[i];

				blobToHide.clear();
				if (isAbove) {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getObject().getFeature(feature);
						if (null == val)
							continue;
						if ( val < threshold) {
							blobToHide.add(blob);
						}
					}

				} else {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getObject().getFeature(feature);
						if (null == val)
							continue;
						if ( val > threshold) {
							blobToHide.add(blob); 
						}
					}

				}
				blobToShow.removeAll(blobToHide); // no need to treat them multiple times
			} // loop over features to threshold
			spotsToshow.put(key, blobToShow);
		} // loop over time points
		return spotsToshow;
	}
	
}
