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



/**
 * A projection formed by projecting the sphere 
 * onto a cone tangent, or secant, to the sphere 
 * along any small circle (usually a mid-latitude parallel). 
 * In the normal aspect (which is oblique for conic projections), 
 * parallels are projected as concentric arcs of circles, 
 * and meridians are projected as straight lines 
 * radiating at uniform angular intervals from the apex of the flattened cone. 
 */
public abstract class ConicProjection extends Projection {
	
	public String toString() {
		return "Conic";
	}

}
