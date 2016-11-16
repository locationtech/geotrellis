/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
