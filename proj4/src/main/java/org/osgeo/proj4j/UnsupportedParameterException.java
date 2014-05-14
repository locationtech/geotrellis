
package org.osgeo.proj4j;

/**
 * Signals that a parameter in a CRS specification
 * is not currently supported, or unknown.
 * 
 * @author mbdavis
 *
 */
public class UnsupportedParameterException extends Proj4jException 
{
	public UnsupportedParameterException() {
		super();
	}

	public UnsupportedParameterException(String message) {
		super(message);
	}
}
