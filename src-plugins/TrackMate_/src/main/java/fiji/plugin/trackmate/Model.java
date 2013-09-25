package fiji.plugin.trackmate;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * <h1>The model for the data managed by TrackMate trackmate.</h1>
 * <p>
 * This is a relatively large class, with a lot of public methods. This
 * complexity arose because this class handles data storage and manipulation,
 * through user manual editing and automatic processing. To avoid conflicting
 * accesses to the data, some specialized methods had to be created, hopefully
 * built in coherent sets.
 * 
 * <h2>Main data stored in this model</h2>
 * 
 * We only list here the central fields. This model has other fields, but they
 * are derived from these following 6 fields or used to modify them. These are
 * the only 6 fields that should be written to a file, and they should be enough
 * to fully reconstruct a new model. By processing order, this model stores the
 * following data.
 * 
 * <h3> {@link #settings}</h3>
 * 
 * The {@link Settings} object that determines the behavior of processes,
 * generating the data stored by this model.
 * 
 * <h3>{@link #spots}</h3>
 * 
 * The raw spots generated by the detection process, stored as
 * {@link SpotCollection}.
 * 
 * <h3>{@link #initialSpotFilterValue}</h3>
 * 
 * The value of the initial Spot {@link FeatureFilter} on
 * {@link SpotFeature#QUALITY}. Since this filter is constrained to be on
 * quality, and above threshold, we do not store the filter itself (preventing
 * some nasty modifications to be possible), but simply the value of the
 * threshold. That filter will be used to crop the {@link #spots} field: spots
 * with quality lower than this threshold will be removed from the
 * {@link SpotCollection}. They will not be stored, nor saved, nor displayed and
 * their features will not be calculated. This is intended to save computation
 * time only.
 * 
 * <h3>{@link #spotFilters}</h3>
 * 
 * The list of Spot {@link FeatureFilter} that the user can set on any computed
 * feature. It will be used to filter spots and generate the
 * {@link #regions} field, that will be used for tracking.
 * <p>
 * Since it only serves to determine the effect of a process (filtering spots by
 * feature), it logically could be a sub-field of the {@link #settings} object.
 * We found more convenient to have it attached to the model.
 * 
 * <h3>{@link #regions}</h3>
 * 
 * The filtered spot, as a new {@link SpotCollection}. It is important that this
 * collection is made with the same spot objects than for the {@link #spots} field.
 * 
 * <h3>{@link #graph}</h3>
 * 
 * The {@link SimpleDirectedWeightedGraph} that contains the map of links between spots.
 * The vertices of the graph are the content of the {@link #regions}
 * field. This is the only convenient way to store links in their more general
 * way we have thought of.
 * <p>
 * It is an undirected graph: we do not indicate the time forward direction
 * using edge direction, but simply refer to the per-frame organization of the
 * {@link SpotCollection}.
 * 
 * <h3>{@link #filteredTrackKeys}</h3>
 * 
 * This Set contains the index of the tracks that are set to be visible. We use this 
 * to flag the tracks that should be retained after filtering the tracks by their
 * features, for instance. Because the user can edit this manually, or because 
 * the track visibility can changed when merging 2 track manually (for instance),
 * we stress on the 'visibility' meaning of this field. 
 * <p>
 * The set contains the indices of the tracks that are visible, in the List
 * of {@link #trackEdges} and {@link #trackSpots}, that are described below.
 * These fields are generated automatically from the track {@link #graph}.
 * For instance, if this set is made of [2, 4], that means the tracks with
 * the indices 2 and 4 in the aforementioned lists are visible, the other not.
 * Of course, {@link TrackMateModelView}s are expected to acknowledge this 
 * content. 
 * <p>
 * This field can be modified publicly using the  {@link #setTrackVisible(Integer, boolean, boolean)}
 * method, or totally overwritten using the {@link #setFilteredTrackIDs(Set, boolean)} method.
 * However, some modifications can arise coming from manual editing of tracks. For instance
 * removing an edge from the middle of a visible tracks generates two new tracks, and
 * possibly shifts the indices of the other tracks. This is hopefully taken care of 
 * the model internal work, and the following rules are implements:
 * <ul>
 * 	<li> TODO
 * </ul>   
 * 
 * <h2>Dependent data</h2>
 * 
 * We list here the fields whose value depends on 
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - 2010-2011
 * 
 */
public class Model {

	/*
	 * CONSTANTS
	 */

	private static final boolean DEBUG = false;

	/*
	 * FIELDS
	 */

	// FEATURES

	private final FeatureModel featureModel;

	// TRACKS

	private final TrackModel trackModel;

	// SPOTS

	/** The spots managed by this model. */
	protected SpotCollection spots = new SpotCollection();


	// TRANSACTION MODEL

	/**
	 * Counter for the depth of nested transactions. Each call to beginUpdate
	 * increments this counter and each call to endUpdate decrements it. When
	 * the counter reaches 0, the transaction is closed and the respective
	 * events are fired. Initial value is 0.
	 */
	private int updateLevel = 0;
	private HashSet<Spot> spotsAdded = new HashSet<Spot>();
	private HashSet<Spot> spotsRemoved = new HashSet<Spot>();
	private HashSet<Spot> spotsMoved = new HashSet<Spot>();
	private HashSet<Spot> spotsUpdated = new HashSet<Spot>();
	/**
	 * The event cache. During a transaction, some modifications might trigger
	 * the need to fire a model change event. We want to fire these events only
	 * when the transaction closes (when the updateLevel reaches 0), so we store
	 * the event ID in this cache in the meantime. The event cache contains only
	 * the int IDs of the events listed in {@link ModelChangeEvent},
	 * namely
	 * <ul>
	 * <li> {@link ModelChangeEvent#SPOTS_COMPUTED}
	 * <li> {@link ModelChangeEvent#SPOT_FILTERED}
	 * <li> {@link ModelChangeEvent#TRACKS_COMPUTED}
	 * <li> {@link ModelChangeEvent#TRACKS_VISIBILITY_CHANGED}
	 * </ul>
	 * The {@link ModelChangeEvent#MODEL_MODIFIED} cannot be cached
	 * this way, for it needs to be configured with modification spot and edge
	 * targets, so it uses a different system (see {@link #flushUpdate()}).
	 */
	private HashSet<Integer> eventCache = new HashSet<Integer>();

	// OTHERS

	/** The logger to append processes messages */
	private Logger logger = Logger.DEFAULT_LOGGER;
	private String spaceUnits = "pixels";
	private String timeUnits = "frames";

	// LISTENERS

	/**
	 * The list of listeners listening to model content change, that is, changes
	 * in {@link #spots}, {@link #filteredSpots} and {@link #trackGraph}.
	 */
	Set<ModelChangeListener> modelChangeListeners = new LinkedHashSet<ModelChangeListener>();






	/*
	 * CONSTRUCTOR
	 */

	public Model() {
		featureModel = new FeatureModel(this);
		trackModel = new TrackModel();
	}


	/*
	 * UTILS METHODS
	 */

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		str.append('\n');
		if (null == spots || spots.keySet().size() == 0) {
			str.append("No spots.\n");
		} else {
			str.append("Contains " + spots.getNSpots(false) + " spots in total.\n");
		}
		if (spots.getNSpots(true) == 0) {
			str.append("No filtered spots.\n");
		} else {
			str.append("Contains " + spots.getNSpots(true) + " filtered spots.\n");
		}

		str.append('\n');
		if (trackModel.nTracks(false) == 0) {
			str.append("No tracks.\n");
		} else {
			str.append("Contains " + trackModel.nTracks(false) + " tracks in total.\n");
		}
		if (trackModel.nTracks(true) == 0) {
			str.append("No filtered tracks.\n");
		} else {
			str.append("Contains " + trackModel.nTracks(true) + " filtered tracks.\n");
		}

		str.append('\n');
		str.append("Physical units:\n  space units: " + spaceUnits + "\n  time units: " + timeUnits + '\n' );
		
		str.append('\n');
		str.append(featureModel.toString());
		
		return str.toString();
	}



	/*
	 * DEAL WITH MODEL CHANGE LISTENER
	 */

	public void addModelChangeListener(ModelChangeListener listener) {
		modelChangeListeners.add(listener);
	}

	public boolean removeModelChangeListener(ModelChangeListener listener) {
		return modelChangeListeners.remove(listener);
	}

	public Set<ModelChangeListener> getModelChangeListener() {
		return modelChangeListeners;
	}

	/*
	 * PHYSICAL UNITS
	 */
	
	/**
	 * Sets the physical units for the quantities stored in this model. 
	 * @param spaceUnits  the spatial units (e.g. μm).
	 * @param timeUnits  the time units (e.g. min).
	 */
	public void setPhysicalUnits(String spaceUnits, String timeUnits) {
		this.spaceUnits = spaceUnits;
		this.timeUnits = timeUnits;
	}
	
	/**
	 * Returns the spatial units for the quantities stored in this model.
	 * @return  the spatial units.
	 */
	public String getSpaceUnits() {
		return spaceUnits;
	}
	
	/**
	 * Returns the time units for the quantities stored in this model.
	 * @return the time units.
	 */
	public String getTimeUnits() {
		return timeUnits;
	}
	
	/*
	 * GRAPH MODIFICATION
	 */

	public synchronized void beginUpdate() {
		updateLevel++;
		if (DEBUG)
			System.out.println("[TrackMateModel] #beginUpdate: increasing update level to " + updateLevel + ".");
	}

	public synchronized void endUpdate() {
		updateLevel--;
		if (DEBUG)
			System.out.println("[TrackMateModel] #endUpdate: decreasing update level to " + updateLevel + ".");
		if (updateLevel == 0) {
			if (DEBUG)
				System.out.println("[TrackMateModel] #endUpdate: update level is 0, calling flushUpdate().");
			flushUpdate();
		}
	}

	/*
	 * TRACK METHODS: WE DELEGATE TO THE TRACK GRAPH MODEL
	 */

	/**
	 * Returns the {@link TrackModel} that manages tracks for this model.
	 */
	public TrackModel getTrackModel() {
		return trackModel;
	}

	/*
	 * GETTERS / SETTERS FOR SPOTS
	 */


	/**
	 * Returns the spot collection managed by this model.
	 * @return the spot collection managed by this model.
	 */
	public SpotCollection getSpots() {
		return spots;
	}

	/**
	 * Set the {@link SpotCollection} managed by this model.
	 * 
	 * @param doNotify if true, will file a  {@link ModelChangeEvent#SPOTS_COMPUTED} event.
	 * @param spots  the {@link SpotCollection} to set.
	 */
	public void setSpots(SpotCollection spots, boolean doNotify) {
		this.spots = spots;
		if (doNotify) {
			final ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.SPOTS_COMPUTED);
			for (ModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
	}
	
	/**
	 * Filters the {@link SpotCollection} managed by this model with the {@link FeatureFilter}s
	 * specified.
	 * 
	 * @param spotFilters the {@link FeatureFilter} collection to use for filtering.
	 * @param doNotify if true, will file a  {@link ModelChangeEvent#SPOTS_FILTERED} event.
	 */
	public void filterSpots(Collection<FeatureFilter> spotFilters, boolean doNotify) {
		spots.filter(spotFilters);
		if (doNotify) {
			final ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.SPOTS_FILTERED);
			for (ModelChangeListener listener : modelChangeListeners)
				listener.modelChanged(event);
		}
		
	}

	/*
	 * LOGGER
	 */

	/**
	 * Set the logger that will receive the messages from the processes
	 * occurring within this trackmate.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Return the logger currently set for this model.
	 */
	public Logger getLogger() {
		return logger;
	}

	/*
	 * FEATURES
	 */

	public FeatureModel getFeatureModel() {
		return featureModel;
	}

	

	/*
	 * MODEL CHANGE METHODS
	 */

	/**
	 * Moves a single spot from a frame to another, make it visible if it was not, 
	 * then mark it for feature update. If the source spot could not be found
	 * in the source frame, nothing is done and <code>null</code> is returned.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * 
	 * @param spotToMove  the spot to move
	 * @param fromFrame  the frame the spot originated from
	 * @param toFrame  the destination frame
	 * @param doNotify   if false, {@link ModelChangeListener}s will not be
	 * notified of this change
	 * @return the spot that was moved, or <code>null</code> if it could not be
	 * found in the source frame
	 */
	public synchronized Spot moveSpotFrom(Spot spotToMove, Integer fromFrame, Integer toFrame) {
		boolean ok = spots.remove(spotToMove, fromFrame);
		if (!ok) {
			if (DEBUG) {
				System.err.println("[TrackMateModel] Could not find spot " + spotToMove + " in frame "+ fromFrame);
			}
			return null;
		}
		spots.add(spotToMove, toFrame);
		if (DEBUG) {
			System.out.println("[TrackMateModel] Moving " + spotToMove + " from frame " + fromFrame + " to frame " + toFrame);
		}

		// Mark for update spot and edges
		trackModel.edgesModified.addAll(trackModel.edgesOf(spotToMove));
		spotsMoved.add(spotToMove); 
		return spotToMove;
	}

	/**
	 * Adds a single spot to the collections managed by this model, mark it as visible, 
	 * then update its features.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @return the spot just added.
	 */
	public synchronized Spot addSpotTo(Spot spotToAdd, Integer toFrame) {
		spots.add(spotToAdd, toFrame);
		spotsAdded.add(spotToAdd); // TRANSACTION
		if (DEBUG) {
			System.out.println("[TrackMateModel] Adding spot " + spotToAdd + " to frame " + toFrame);
		}
		trackModel.addSpot(spotToAdd);
		return spotToAdd;
	}

	/**
	 * Removes a single spot from the collections managed by this model.
	 * If the spot cannot be found in the specified frame, nothing is done
	 * and <code>null</code> is returned. 
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @param spotToRemove  the spot to remove.
	 * @param fromFrame   the frame the spot is in.
	 * @return the spot removed, or <code>null</code> if it could not be found in the
	 * specified frame.
	 */
	public synchronized Spot removeSpot(final Spot spotToRemove) {
		int fromFrame = spotToRemove.getFeature(Spot.FRAME).intValue();
		if (spots.remove(spotToRemove, fromFrame)) {
			spotsRemoved.add(spotToRemove); // TRANSACTION
			if (DEBUG) {
				System.out.println("[TrackMateModel] Removing spot " + spotToRemove + " from frame " + fromFrame);
			}
			trackModel.removeSpot(spotToRemove); // changes to edges will be caught automatically by the TrackGraphModel
			return spotToRemove;
		} else {
			if (DEBUG) {
				System.err.println("[TrackMateModel] The spot " + spotToRemove + " cannot be found in frame " + fromFrame);
			}
			return null;
			
		}
	}

	/**
	 * Mark the specified spot for update. At the end of the model transaction, its features 
	 * will be recomputed, and other edge and track features that depends on it will 
	 * be as well.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @param spotToUpdate  the spot to mark for update
	 */
	public synchronized void updateFeatures(final Spot spotToUpdate) {
		spotsUpdated.add(spotToUpdate); // Enlist for feature update when transaction is marked as finished
		Set<DefaultWeightedEdge> touchingEdges = trackModel.edgesOf(spotToUpdate);
		if (null != touchingEdges) {
			trackModel.edgesModified.addAll(touchingEdges);
		}
	}

	/**
	 * Creates a new edge between two spots, with the specified weight.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @param source  the source spot.
	 * @param target  the target spot.
	 * @param weight  the weight of the edge.
	 * @return  the edge created.
	 */
	public synchronized DefaultWeightedEdge addEdge(final Spot source, final Spot target, final double weight) {
		return trackModel.addEdge(source, target, weight);
	}

	/**
	 * Removes an edge between two spots and returns it. Returns <code>null</code>
	 * and do nothing to the tracks if the edge did not exist.
	 * @param source the source spot.
	 * @param target the target spot.
	 * @return  the edge between the two spots, if it existed.
	 */
	public synchronized DefaultWeightedEdge removeEdge(final Spot source, final Spot target) {
		return trackModel.removeEdge(source, target);
	}

	/**
	 * Removes an existing edge from the model.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre> 
	 * @param edge  the edge to remove.
	 * @return <code>true</code> if the edge existed in the model and was successfully,
	 * <code>false</code> otherwise.
	 */
	public synchronized boolean removeEdge(final DefaultWeightedEdge edge) {
		return trackModel.removeEdge(edge);
	}

	/**
	 * Sets the weight of the specified edge.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @param edge  the edge.
	 * @param weight  the weight to set.
	 */
	public synchronized void setEdgeWeight(final DefaultWeightedEdge edge, double weight) {
		trackModel.setEdgeWeight(edge, weight);
	}

	/**
	 * Sets the visibility of the specified track. Throws a {@link NullPointerException} if the
	 * track ID is unknown to the model.
	 * <p>
	 * For the model update to happen correctly and listeners to be notified
	 * properly, a call to this method must happen within a transaction, as in:
	 * <pre>
	 * model.beginUpdate();
	 * try {
	 * 	... // model modifications here
	 * } finally {
	 * 	model.endUpdate();
	 * }
	 * </pre>
	 * @param trackID  the track ID.
	 * @param visible  the desired visibility.
	 * @return  the specified track visibility prior to calling this method.
	 */
	public synchronized boolean setTrackVisibility(Integer trackID, boolean visible) {
		boolean oldvis = trackModel.setVisibility(trackID, visible);
		boolean modified = oldvis != visible;
		if (modified) {
			eventCache.add(ModelChangeEvent.TRACKS_VISIBILITY_CHANGED);
		}
		return oldvis;
	}

	/*
	 * PRIVATE METHODS
	 */


	/**
	 * Fire events. Regenerate fields derived from the filtered graph.
	 */
	private void flushUpdate() {

		if (DEBUG) {
			System.out.println("[TrackMateModel] #flushUpdate().");
			System.out.println("[TrackMateModel] #flushUpdate(): Event cache is :" + eventCache);
			System.out.println("[TrackMateModel] #flushUpdate(): Track content is:\n" + trackModel.echo());
		}

		/* We recompute tracks only if some edges have been added or removed,
		 * (if some spots have been removed that causes edges to be removes, we already know about it).
		 * We do NOT recompute tracks if spots have been added: they will not result in
		 * new tracks made of single spots.	 */
		int nEdgesToSignal = trackModel.edgesAdded.size() + trackModel.edgesRemoved.size() + trackModel.edgesModified.size();

		// Do we have tracks to update?
		HashSet<Integer> tracksToUpdate = new HashSet<Integer>(trackModel.tracksUpdated);

		// We also want to update the tracks that have edges that were modified
		for (DefaultWeightedEdge modifiedEdge : trackModel.edgesModified) {
			tracksToUpdate.add(trackModel.trackIDOf(modifiedEdge));
		}

		// Deal with new or moved spots: we need to update their features.
		int nSpotsToUpdate = spotsAdded.size() + spotsMoved.size() + spotsUpdated.size();
		if (nSpotsToUpdate > 0) {
			HashSet<Spot> spotsToUpdate = new HashSet<Spot>(nSpotsToUpdate);
			spotsToUpdate.addAll(spotsAdded);
			spotsToUpdate.addAll(spotsMoved);
			spotsToUpdate.addAll(spotsUpdated);
		}

		// Initialize event
		ModelChangeEvent event = new ModelChangeEvent(this, ModelChangeEvent.MODEL_MODIFIED);

		// Configure it with spots to signal.
		int nSpotsToSignal = nSpotsToUpdate + spotsRemoved.size();
		if (nSpotsToSignal > 0) {
			event.addAllSpots(spotsAdded);
			event.addAllSpots(spotsRemoved);
			event.addAllSpots(spotsMoved);
			event.addAllSpots(spotsUpdated);

			for (Spot spot : spotsAdded) {
				event.putSpotFlag(spot, ModelChangeEvent.FLAG_SPOT_ADDED);
			}
			for (Spot spot : spotsRemoved) {
				event.putSpotFlag(spot, ModelChangeEvent.FLAG_SPOT_REMOVED);
			}
			for (Spot spot : spotsMoved) {
				event.putSpotFlag(spot, ModelChangeEvent.FLAG_SPOT_FRAME_CHANGED);
			}
			for (Spot spot : spotsUpdated) {
				event.putSpotFlag(spot, ModelChangeEvent.FLAG_SPOT_MODIFIED);
			}
		}


		// Configure it with edges to signal.
		if (nEdgesToSignal > 0) {
			event.addAllEdges(trackModel.edgesAdded);
			event.addAllEdges(trackModel.edgesRemoved);
			event.addAllEdges(trackModel.edgesModified);

			for (DefaultWeightedEdge edge : trackModel.edgesAdded) {
				event.putEdgeFlag(edge, ModelChangeEvent.FLAG_EDGE_ADDED);
			}
			for (DefaultWeightedEdge edge : trackModel.edgesRemoved) {
				event.putEdgeFlag(edge, ModelChangeEvent.FLAG_EDGE_REMOVED);
			}
			for (DefaultWeightedEdge edge : trackModel.edgesModified) {
				event.putEdgeFlag(edge, ModelChangeEvent.FLAG_EDGE_MODIFIED);
			}
		}

		// Configure it with the tracks we found need updating
		event.setTracksUpdated(tracksToUpdate);

		try {
			if (nEdgesToSignal + nSpotsToSignal > 0) {
				if (DEBUG) {
					System.out.println("[TrackMateModel] #flushUpdate(): firing model modified event");
					System.out.println("[TrackMateModel] to " + modelChangeListeners); 

				}
				for (final ModelChangeListener listener : modelChangeListeners) {
					listener.modelChanged(event);
				}
			}

			// Fire events stored in the event cache
			for (int eventID : eventCache) {
				if (DEBUG) {
					System.out.println("[TrackMateModel] #flushUpdate(): firing event with ID "	+ eventID);
				}
				ModelChangeEvent cachedEvent = new ModelChangeEvent(this, eventID);
				for (final ModelChangeListener listener : modelChangeListeners) {
					listener.modelChanged(cachedEvent);
				}
			}

		} finally {
			spotsAdded.clear();
			spotsRemoved.clear();
			spotsMoved.clear();
			spotsUpdated.clear();
			trackModel.edgesAdded.clear();
			trackModel.edgesRemoved.clear();
			trackModel.edgesModified.clear();
			trackModel.tracksUpdated.clear();
			eventCache.clear();
		}

	}

}
