package etomica.api;


/**
 * Interface for boundary objects that describe the size and periodic nature
 * of the box boundaries. Each Box has its own instance of this class. It
 * may be referenced by a coordinate pair when computing distances between
 * atoms, or by a cell iterator when generating neighbor lists. It is also used
 * by objects that enforce periodic images.
 * 
 * The boundary is responsible for firing inflate events when the boundary
 * dimensions change.
 */
public interface IBoundary {

    /**
     * Sets the box that holds the IBoundary.  If no box holds the boundary,
     * the box should be set to null.
     */
    public void setBox(IBox newBox);

    /**
     * Returns the boundary's IBox.  Might be null if the boundary is not
     * associated with a box.
     */
    public IBox getBox();

    /**
     * @return the volume enclosed by the boundary
     */
    public double volume();

    /**
     * Determines the translation vector needed to apply a periodic-image
     * transformation that moves the given point to an image point within the
     * boundary (if it lies outside, in a direction subject to periodic
     * imaging).
     * 
     * @param r
     *            vector position of untransformed point; r is not changed by
     *            this method
     * @return the displacement that must be applied to r to move it to its
     *         central-image location
     */
    public IVector centralImage(IVector r);

	/**
	 * The nearest image is the pair of atom images that are closest when all
	 * periodic-boundary images are considered.
	 * 
	 * If the vector passed to this method is the displacement vector between
	 * two points, the vector will be transformed such that it corresponds to
	 * the vector between the nearest image of those two points.
	 */
    public void nearestImage(IVector dr);

    /**
     * Returns a copy of the dimensions, as a Vector. Manipulation of this copy
     * will not cause any change to the boundary's dimensions.
     * 
     * @return a vector giving the nominal length of the boundary in each
     *         direction. This has an obvious interpretation for rectangular
     *         boundaries, while for others (e.g., octahedral) the definition is
     *         particular to the boundary.
     */
    public IVector getDimensions();

    /**
     * Sets the nominal length of the boundary in each direction. Specific
     * interpretation of the given values (which are the elements of the given
     * Vector) depends on the subclass.
     */
    public void setDimensions(IVector v);

    /**
     * @return a point selected uniformly within the volume enclosed by the
     *         boundary.
     */
    public IVector randomPosition();

    /**
     * Returns the length of the sides of a rectangular box oriented in the lab
     * frame and in which the boundary is inscribed.  Each element of the returned
     * vector gives in that coordinate direction the maximum distance from one point 
     * on the boundary to another.  Returned vector should be used immediately or
     * copied to another vector.
     */
    public IVector getBoundingBox();

}