package org.osgeo.proj4j;

import org.osgeo.proj4j.datum.*;

/**
 * An interface for the operation of transforming 
 * a {@link ProjCoordinate} from one {@link CoordinateReferenceSystem} 
 * into a different one.
 * 
 * @author Martin Davis
 * 
 * @see CoordinateTransformFactory
 */
public interface CoordinateTransform 
{

  public CoordinateReferenceSystem getSourceCRS();
  
  public CoordinateReferenceSystem getTargetCRS();
  
  
	/**
   * Tranforms a coordinate from the source {@link CoordinateReferenceSystem} 
   * to the target one.
   * 
   * @param src the input coordinate to transform
   * @param tgt the transformed coordinate
   * @return the target coordinate which was passed in
   * 
   * @throws Proj4jException if a computation error is encountered
	 */
	public ProjCoordinate transform( ProjCoordinate src, ProjCoordinate tgt )
  throws Proj4jException;
  

}
