package org.osgeo.proj4j;

import org.osgeo.proj4j.datum.Datum;

/**
 * Creates {@link CoordinateTransform}s
 * from source and target {@link CoordinateReferenceSystem}s.
 * 
 * @author mbdavis
 *
 */
public class CoordinateTransformFactory 
{
    /**
     * Creates a new factory.
     *
     */
    public CoordinateTransformFactory()
    {
		
    }
	
    /**
     * Creates a transformation from a source CRS to a target CRS,
     * following the logic in PROJ.4.
     * The transformation may include any or all of inverse projection, datum transformation,
     * and reprojection, depending on the nature of the coordinate reference systems 
     * provided.
     *  
     * @param sourceCRS the source CoordinateReferenceSystem
     * @param targetCRS the target CoordinateReferenceSystem
     * @return a tranformation from the source CRS to the target CRS
     */
    public CoordinateTransform createTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS)
    {
        return new BasicCoordinateTransform(sourceCRS, targetCRS);
    }
}
