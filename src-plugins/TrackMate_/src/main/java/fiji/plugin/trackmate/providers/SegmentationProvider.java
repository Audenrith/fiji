package fiji.plugin.trackmate.providers;


import static fiji.plugin.trackmate.segmentation.SegmenterKeys.XML_ATTRIBUTE_SEGMENTER_NAME;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.segmentation.JostSegmenter;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.segmentation.Segmenter;

public class SegmentationProvider extends AbstractProvider {

	private Model model;
	
	/**
	 * This provider provides the GUI with the segmenters currently available in the 
	 * TrackMate plugin. Each segmenter is identified by a key String, which can be used 
	 * to retrieve new instance of the segmenter.
	 * <p>
	 * If you want to add custom segmenters to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom segmenter and pass this 
	 * extended provider to the {@link TrackMate_} plugin.
	 * @param controller 
	 */
	public SegmentationProvider(Model model) {
		this.model = model;
		registerSegmenters();
		currentKey = JostSegmenter.SEGMENTER_KEY;
	}


	/*
	 * METHODS
	 */

	public Segmenter getSegmenter(String key, ImagePlus img) {
		if (key.equals(JostSegmenter.SEGMENTER_KEY)) {
			return new JostSegmenter(model, img);
		} else if (key.equals(ManualSegmenter.SEGMENTER_KEY)) {
			return new ManualSegmenter(model, img, null);
		} else {
			
			errorMessage = "Unknow segmenter factory key: "+key+".\n";			
			return null;
		}
		
	}
	

	/**
	 * Register the standard segmenters shipped with TrackMate.
	 */
	protected void registerSegmenters() {
		// keys
		keys = new ArrayList<String>(2);
		keys.add(JostSegmenter.SEGMENTER_KEY);
		keys.add(ManualSegmenter.SEGMENTER_KEY);
		// names
		names = new ArrayList<String>(2);
		names.add(JostSegmenter.NAME);
		names.add(ManualSegmenter.NAME);
		// infoTexts
		infoTexts = new ArrayList<String>(2);
		infoTexts.add(JostSegmenter.INFO_TEXT);
		infoTexts.add(ManualSegmenter.INFO_TEXT);
		
	}

	/**
	 * Marshall a settings map to a JDom element, ready for saving to XML. 
	 * The element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the target segmenter factory are marshalled.
	 * The element also always receive an attribute named {@value SegmenterKeys#XML_ATTRIBUTE_SEGMENTER_NAME}
	 * that saves the target {@link Segmentation} key.
	 * 
	 * @return true if marshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean marshall(final Map<String, Object> settings, Element element) {

		element.setAttribute(XML_ATTRIBUTE_SEGMENTER_NAME, currentKey);

		if (currentKey.equals(JostSegmenter.SEGMENTER_KEY)) {

			return true;

		} else if (currentKey.equals(ManualSegmenter.SEGMENTER_KEY)) {

			return true;

		} else {

			errorMessage = "Unknow segmentor factory key: "+currentKey+".\n";
			return false;

		}
	}

	/**
	 * Un-marshall a JDom element to update a settings map, and sets the target 
	 * segmenter factory of this provider from the element. 
	 * <p>
	 * Concretely: the segmenter key is read from the element, and is used to set 
	 * the target {@link #currentKey} of this provider. The the specific settings 
	 * map for the targeted segmenter factory is updated from the element.
	 * 
	 * @param element the JDom element to read from.
	 * @param settings the map to update. Is cleared prior to updating, so that it contains
	 * only the parameters specific to the target segmenter factory.
	 * @return true if unmarshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean unmarshall(final Element element, Map<String, Object> settings) {
		
		settings.clear();

		String segmenterKey = element.getAttributeValue(XML_ATTRIBUTE_SEGMENTER_NAME);
		// Try to set the state of this provider from the key read in xml.
		boolean ok = select(segmenterKey);
		if (!ok) {
			errorMessage = "Segmenter key found in XML ("+segmenterKey+") is unknown to this provider.\n";
			return false;
		}

		if (currentKey.equals(JostSegmenter.SEGMENTER_KEY)) {
			
			return true;

		} else if (currentKey.equals(ManualSegmenter.SEGMENTER_KEY)) {

			return true;

		} else {

			errorMessage = "Unknow segmenter factory key: "+currentKey+".\n";
			return false;
		}
	}





	/**
	 * @return a new default settings map suitable for the target segmenter identified by 
	 * the {@link #currentKey}. Settings are instantiated with default values.  
	 * If the key is unknown to this provider, <code>null</code> is returned. 
	 */
	public Map<String, Object> getDefaultSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();

		if (currentKey.equals(JostSegmenter.SEGMENTER_KEY) ) {
			// DO NOTHING
		} else if (currentKey.equals(ManualSegmenter.SEGMENTER_KEY) ) {
			// DO NOTHING
		} else {
			return null;
		}

		return settings;

	}

	/**
	 * @return the html String containing a descriptive information about the target segmenter,
	 * or <code>null</code> if it is unknown to this provider.
	 */
	public String getInfoText() {

		if (currentKey.equals(JostSegmenter.SEGMENTER_KEY)) {
			return JostSegmenter.INFO_TEXT;

		} else if (currentKey.equals(ManualSegmenter.SEGMENTER_KEY)) {
			return ManualSegmenter.INFO_TEXT;

		} else {
			return null;
		}
	}


}
