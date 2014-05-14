package org.osgeo.proj4j;

/**
 * Signals that a parameter or computed internal variable
 * has a value which lies outside the 
 * allowable bounds for the computation in which it is being used.
 * 
 * @author mbdavis
 *
 */
public class InvalidValueException extends Proj4jException {
	public InvalidValueException() {
		super();
	}

	public InvalidValueException(String message) {
		super(message);
	}
}
