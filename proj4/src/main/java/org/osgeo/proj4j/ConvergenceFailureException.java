
package org.osgeo.proj4j;

/**
 * Signals that an interative mathematical algorithm has failed to converge.
 * This is usually due to values exceeding the
 * allowable bounds for the computation in which they are being used.
 * 
 * @author mbdavis
 *
 */
public class ConvergenceFailureException extends Proj4jException 
{
	public ConvergenceFailureException() {
		super();
	}

	public ConvergenceFailureException(String message) {
		super(message);
	}
}
