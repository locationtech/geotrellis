/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.util.ProjectionMath;

/**
 * The superclass for all azimuthal map projections
 */
public abstract class AzimuthalProjection extends Projection {

	public final static int NORTH_POLE = 1;
	public final static int SOUTH_POLE = 2;
	public final static int EQUATOR = 3;
	public final static int OBLIQUE = 4;
	
	protected int mode;
	protected double sinphi0, cosphi0;
	private double mapRadius = 90.0;
	
	public AzimuthalProjection() {
		this( Math.toRadians(45.0), Math.toRadians(45.0) );
	}

	public AzimuthalProjection(double projectionLatitude, double projectionLongitude) {
		this.projectionLatitude = projectionLatitude;
		this.projectionLongitude = projectionLongitude;
		initialize();
	}
	
	public void initialize() {
		super.initialize();
		if (Math.abs(Math.abs(projectionLatitude) - ProjectionMath.HALFPI) < EPS10)
			mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
		else if (Math.abs(projectionLatitude) > EPS10) {
			mode = OBLIQUE;
			sinphi0 = Math.sin(projectionLatitude);
			cosphi0 = Math.cos(projectionLatitude);
		} else
			mode = EQUATOR;
	}

	public boolean inside(double lon, double lat) {
		return ProjectionMath.greatCircleDistance( Math.toRadians(lon), Math.toRadians(lat), projectionLongitude, projectionLatitude) < Math.toRadians(mapRadius);
	}

	/**
	 * Set the map radius (in degrees). 180 shows a hemisphere, 360 shows the whole globe.
	 */
	public void setMapRadius(double mapRadius) {
		this.mapRadius = mapRadius;
	}

	public double getMapRadius() {
		return mapRadius;
	}

}

