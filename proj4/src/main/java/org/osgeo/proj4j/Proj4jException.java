package org.osgeo.proj4j;

/**
 * Signals that a situation or data state has been encountered
 * which prevents computation from proceeding,
 * or which would lead to erroneous results.
 * <p>
 * This is the base class for all exceptions 
 * thrown in the Proj4J API.
 * 
 * @author mbdavis
 *
 */
public class Proj4jException extends RuntimeException 
{
	public Proj4jException() {
		super();
	}

	public Proj4jException(String message) {
		super(message);
	}
}
