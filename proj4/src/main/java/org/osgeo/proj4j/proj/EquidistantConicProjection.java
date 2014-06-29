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

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 */
package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.util.ProjectionMath;

/**
 * The Equidistant Conic projection.
 */
public class EquidistantConicProjection extends ConicProjection {

	private double standardLatitude1;
	private double standardLatitude2;
	
	private double eccentricity = 0.822719;
	private double eccentricity2 = eccentricity*eccentricity;
	private double eccentricity4 = eccentricity2*eccentricity2;
	private double eccentricity6 = eccentricity2*eccentricity4;
	private double radius = 1;

	private boolean northPole;
	private double f, n, rho0;

	public EquidistantConicProjection() {
		minLatitude = ProjectionMath.degToRad(10);
		maxLatitude = ProjectionMath.degToRad(70);
		minLongitude = ProjectionMath.degToRad(-90);
		maxLongitude = ProjectionMath.degToRad(90);
		standardLatitude1 = Math.toDegrees( 60 );
		standardLatitude2 = Math.toDegrees( 20 );

		initialize(ProjectionMath.degToRad(0), ProjectionMath.degToRad(37.5), standardLatitude1, standardLatitude2);
	}

	public ProjCoordinate project(ProjCoordinate in, ProjCoordinate out) {
		double lon = ProjectionMath.normalizeLongitude(in.x-projectionLongitude);
		double lat = in.y;
		double rho,theta,hold1,hold2,hold3;
		
		hold2 = Math.pow(((1.0 - eccentricity * Math.sin(lat)) / (1.0 + eccentricity * Math.sin(lat))), 0.5 * eccentricity);
		hold3 = Math.tan(ProjectionMath.QUARTERPI - 0.5 * lat);
		hold1 = (hold3 == 0.0) ? 0.0 : Math.pow(hold3 / hold2, n);
		rho = radius * f * hold1;
		theta = n * lon;

		out.x = rho * Math.sin(theta);
		out.y = rho0 - rho * Math.cos(theta);
		return out;
	}

	public ProjCoordinate inverseProject(ProjCoordinate in, ProjCoordinate out) {
		double theta, temp, rho, t, tphi, phi = 0, delta;
		
		theta = Math.atan(in.x / (rho0 - in.y));
		out.x = (theta / n) + projectionLongitude;
		
		temp = in.x * in.x + (rho0 - in.y) * (rho0 - in.y);
		rho = Math.sqrt(temp);
		if (n < 0)
			rho = - rho;
		t = Math.pow((rho / (radius * f)), 1./n);
		tphi = ProjectionMath.HALFPI - 2.0 * Math.atan(t);
		delta = 1.0;
		for (int i = 0; i < 100 && delta > 1.0e-8; i++) {
			temp = (1.0 - eccentricity * Math.sin(tphi)) / (1.0 + eccentricity * Math.sin(tphi));
			phi = ProjectionMath.HALFPI - 2.0 * Math.atan(t * Math.pow(temp, 0.5 * eccentricity));
			delta = Math.abs(Math.abs(tphi) - Math.abs(phi));
			tphi = phi;
		}
		out.y = phi;
		return out;
	}

	private void initialize(double rlong0, double rlat0, double standardLatitude1, double standardLatitude2) {
		super.initialize();
		double t_standardLatitude1, m_standardLatitude1, t_standardLatitude2, m_standardLatitude2, t_rlat0;
		
		northPole = rlat0 > 0.0;
		projectionLatitude = northPole ? ProjectionMath.HALFPI : -ProjectionMath.HALFPI;
		
		t_standardLatitude1 = Math.tan(ProjectionMath.QUARTERPI - 0.5 * standardLatitude1) / Math.pow((1.0 - eccentricity * 
			Math.sin(standardLatitude1)) /(1.0 + eccentricity * Math.sin(standardLatitude1)), 0.5 * eccentricity);
		m_standardLatitude1 = Math.cos(standardLatitude1) / Math.sqrt(1.0 - eccentricity2 
			* Math.pow(Math.sin(standardLatitude1), 2.0));
		t_standardLatitude2 = Math.tan(ProjectionMath.QUARTERPI - 0.5 * standardLatitude2) / Math.pow((1.0 - eccentricity *
			Math.sin(standardLatitude2)) /(1.0 + eccentricity * Math.sin(standardLatitude2)), 0.5 * eccentricity);
		m_standardLatitude2 = Math.cos(standardLatitude2) / Math.sqrt(1.0 - eccentricity2 
			* Math.pow(Math.sin(standardLatitude2), 2.0));
		t_rlat0 = Math.tan(ProjectionMath.QUARTERPI - 0.5 * rlat0) /
			Math.pow((1.0 - eccentricity * Math.sin(rlat0)) /
			(1.0 + eccentricity * Math.sin(rlat0)), 0.5 * eccentricity);
		
		if (standardLatitude1 != standardLatitude2)
			n = (Math.log(m_standardLatitude1) - Math.log(m_standardLatitude2))/(Math.log(t_standardLatitude1) - Math.log(t_standardLatitude2));
		else
			n = Math.sin(standardLatitude1);
		
		f = m_standardLatitude1/(n * Math.pow(t_standardLatitude1, n));
		projectionLongitude = rlong0;
		rho0 = radius * f * Math.pow(t_rlat0,n);
	}
	
	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Equidistant Conic";
	}

}

