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

public class MolleweideProjection extends PseudoCylindricalProjection {

	public static final int MOLLEWEIDE = 0;
	public static final int WAGNER4 = 1;
	public static final int WAGNER5 = 2;

	private static final int MAX_ITER = 10;
	private static final double TOLERANCE = 1e-7;

	private int type = MOLLEWEIDE;
	private double cx, cy, cp;

	public MolleweideProjection() {
		this(Math.PI/2);
	}
	
	public MolleweideProjection(int type) {
		this.type = type;
		switch (type) {
		case MOLLEWEIDE:
			init(Math.PI/2);
			break;
		case WAGNER4:
			init(Math.PI/3);
			break;
		case WAGNER5:
			init(Math.PI/2);
			cx = 0.90977;
			cy = 1.65014;
			cp = 3.00896;
			break;
		}
	}
	
	public MolleweideProjection(double p) {
		init(p);
	}
	
	public void init(double p) {
		double r, sp, p2 = p + p;

		sp = Math.sin(p);
		r = Math.sqrt(Math.PI*2.0 * sp / (p2 + Math.sin(p2)));
		cx = 2. * r / Math.PI;
		cy = r / sp;
		cp = p2 + Math.sin(p2);
	}

	public MolleweideProjection(double cx, double cy, double cp) {
		this.cx = cx;
		this.cy = cy;
		this.cp = cp;
	}

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
		double k, v;
		int i;

		k = cp * Math.sin(lpphi);
		for (i = MAX_ITER; i != 0; i--) {
			lpphi -= v = (lpphi + Math.sin(lpphi) - k) / (1. + Math.cos(lpphi));
			if (Math.abs(v) < TOLERANCE)
				break;
		}
		if (i == 0)
			lpphi = (lpphi < 0.) ? -Math.PI/2 : Math.PI/2;
		else
			lpphi *= 0.5;
		xy.x = cx * lplam * Math.cos(lpphi);
		xy.y = cy * Math.sin(lpphi);
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		double lat, lon;
		
		lat = Math.asin(y / cy);
		lon = x / (cx * Math.cos(lat));
		lat += lat;
		lat = Math.asin((lat + Math.sin(lat)) / cp);
		lp.x = lon;
		lp.y = lat;
		return lp;
	}
	
	public boolean hasInverse() {
		return true;
	}

	public boolean isEqualArea() {
	    return true;
	}
	 
	public String toString() {
		switch (type) {
		case WAGNER4:
			return "Wagner IV";
		case WAGNER5:
			return "Wagner V";
		}
		return "Molleweide";
	}
}
