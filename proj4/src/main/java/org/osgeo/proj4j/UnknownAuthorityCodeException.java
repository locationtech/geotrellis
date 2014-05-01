
package org.osgeo.proj4j;

/**
 * Signals that an authority code is unknown 
 * and cannot be mapped to a CRS definition.
 * 
 * @author mbdavis
 *
 */
public class UnknownAuthorityCodeException extends Proj4jException 
{
	public UnknownAuthorityCodeException() {
		super();
	}

	public UnknownAuthorityCodeException(String message) {
		super(message);
	}
}
