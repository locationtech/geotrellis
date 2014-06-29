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

package org.osgeo.proj4j.datum;

import org.osgeo.proj4j.ProjCoordinate;


/**
 * A class representing a geodetic datum.
 * <p>
 * A geodetic datum consists of a set of reference points on or in the Earth,
 * and a reference {@link Ellipsoid} giving an approximation 
 * to the true shape of the geoid.
 * <p>
 * In order to transform between two geodetic points specified 
 * on different datums, it is necessary to transform between the 
 * two datums.  There are various ways in which this
 * datum conversion may be specified:
 * <ul>
 * <li>A 3-parameter conversion
 * <li>A 7-parameter conversion
 * <li>A grid-shift conversion
 * </ul>
 * In order to be able to transform between any two datums, 
 * the parameter-based transforms are provided as a transform to 
 * the common WGS84 datum.  The WGS transforms of two arbitrary datum transforms can 
 * be concatenated to provide a transform between the two datums.
 * <p>
 * Notable datums in common use include {@link #NAD83} and {@link #WGS84}.
 * 
 */
public class Datum 
{
  public static final int TYPE_UNKNOWN = 0;
  public static final int TYPE_WGS84 = 1;
  public static final int TYPE_3PARAM = 2;
  public static final int TYPE_7PARAM = 3;
  public static final int TYPE_GRIDSHIFT = 4;
  
  private static final double[] DEFAULT_TRANSFORM = new double[] { 0.0, 0.0, 0.0 };

  public static final Datum WGS84 = new Datum("WGS84", 0,0,0, Ellipsoid.WGS84, "WGS84"); 
  public static final Datum GGRS87 = new Datum("GGRS87", -199.87,74.79,246.62, Ellipsoid.GRS80, "Greek_Geodetic_Reference_System_1987");
  public static final Datum NAD83 = new Datum("NAD83", 0,0,0, Ellipsoid.GRS80,"North_American_Datum_1983");
  public static final Datum NAD27 = new Datum("NAD27", "@conus,@alaska,@ntv2_0.gsb,@ntv1_can.dat", Ellipsoid.CLARKE_1866,"North_American_Datum_1927");
  public static final Datum POTSDAM = new Datum("potsdam", 606.0,23.0,413.0, Ellipsoid.BESSEL, "Potsdam Rauenberg 1950 DHDN");
  public static final Datum CARTHAGE = new Datum("carthage",-263.0,6.0,431.0, Ellipsoid.CLARKE_1880, "Carthage 1934 Tunisia");
  public static final Datum HERMANNSKOGEL = new Datum("hermannskogel", 653.0,-212.0,449.0, Ellipsoid.BESSEL, "Hermannskogel");
  public static final Datum IRE65 = new Datum("ire65", 482.530,-130.596,564.557,-1.042,-0.214,-0.631,8.15, Ellipsoid.MOD_AIRY, "Ireland 1965");
  public static final Datum NZGD49 = new Datum("nzgd49", 59.47,-5.04,187.44,0.47,-0.1,1.024,-4.5993, Ellipsoid.INTERNATIONAL, "New Zealand Geodetic Datum 1949");
  public static final Datum OSEB36 = new Datum("OSGB36", 446.448,-125.157,542.060,0.1502,0.2470,0.8421,-20.4894, Ellipsoid.AIRY, "Airy 1830");

  private String code;
	private String name;
	private Ellipsoid ellipsoid;
	private double[] transform = DEFAULT_TRANSFORM;
	
  public Datum(String code, 
      String transformSpec, 
      Ellipsoid ellipsoid,
      String name) {
    // TODO: implement handling of transform specification
    this(code, (double[]) null, ellipsoid, name);
  }
  
  public Datum(String code, 
      double deltaX, double deltaY, double deltaZ, 
      Ellipsoid ellipsoid,
      String name) {
    this(code, new double[] { deltaX, deltaY, deltaZ },ellipsoid, name);
  }
  
  public Datum(String code, 
      double deltaX, double deltaY, double deltaZ,
      double rx, double ry, double rz, double mbf,
      Ellipsoid ellipsoid,
      String name) {
    this(code, new double[] { deltaX, deltaY, deltaZ, rx, ry, rz, mbf },ellipsoid, name);
  }
  
  public Datum(String code, 
      double[] transform, 
      Ellipsoid ellipsoid,
      String name) {
    this.code = code;
    this.name = name;
    this.ellipsoid = ellipsoid;
    if (transform != null)
      this.transform = transform;
  }
  
  public String getCode() { return code; }
  
  public String getName() { return name; }
  
  public String toString() { return "[Datum-" + name + "]"; }
  
  public Ellipsoid getEllipsoid()
  {
    return ellipsoid;
  }
  
  public double[] getTransformToWGS84()
  {
    return transform;
  }
  
  public int getTransformType()
  {
    if (transform  == null) return TYPE_WGS84;
    
    if (isIdentity(transform)) return TYPE_WGS84;
    
    if (transform.length  == 3) return TYPE_3PARAM;
    if (transform.length  == 7) return TYPE_7PARAM;
    
    return TYPE_WGS84;
  }
  
  /**
   * Tests whether the datum parameter-based transform 
   * is the identity transform 
   * (in which case datum transformation can be short-circuited,
   * thus avoiding some loss of numerical precision).
   * 
   * @param transform
   * @return
   */
  private static boolean isIdentity(double[] transform)
  {
    for (int i = 0; i < transform.length; i++) {
      // scale factor will normally be 1 for an identity transform
      if (i == 6) {
        if (transform[i] != 1.0 && transform[i] != 0.0)
          return false;
      }
      else if (transform[i] != 0.0) return false;
    }
    return true;
  }
  
  public boolean hasTransformToWGS84()
  {
    int transformType = getTransformType();
    return transformType == TYPE_3PARAM || transformType == TYPE_7PARAM;
  }
  
  public static final double ELLIPSOID_E2_TOLERANCE = 0.000000000050;
  
  /**
   * Tests if this is equal to another {@link Datum}.
   * <p>
   * Datums are considered to be equal iff:
   * <ul>
   * <li>their transforms are equal
   * <li>OR their ellipsoids are (approximately) equal
   * </ul>
   * 
   * @param datum
   * @return
   */
  public boolean isEqual(Datum datum)
  {
  	// false if tranforms are not equal
    if( getTransformType() != datum.getTransformType()) {
      return false; 
    }
    // false if ellipsoids are not (approximately) equal
    if( ellipsoid.getEquatorRadius() != ellipsoid.getEquatorRadius()) {
      if (Math.abs(ellipsoid.getEccentricitySquared() 
           - datum.ellipsoid.getEccentricitySquared() )  > ELLIPSOID_E2_TOLERANCE)
      return false;
    } 
    
    // false if transform parameters are not identical
    if( getTransformType() == TYPE_3PARAM || getTransformType() == TYPE_7PARAM) {
      for (int i = 0; i < transform.length; i++) {
        if (transform[i] != datum.transform[i])
          return false;
      }
      return true;
    } 
    /* 
     //TODO: complete
    else if( this.datum_type == Proj4js.common.PJD_GRIDSHIFT ) {
      return strcmp( pj_param(this.params,"snadgrids").s,
                     pj_param(dest.params,"snadgrids").s ) == 0;
    }
    */ 
    return true; // datums are equal

  }

  public void transformFromGeocentricToWgs84(ProjCoordinate p) 
  {
    if( transform.length == 3 )
    {
      p.x += transform[0];
      p.y += transform[1];
      p.z += transform[2];

    }
    else if (transform.length == 7)
    {
      double Dx_BF = transform[0];
      double Dy_BF = transform[1];
      double Dz_BF = transform[2];
      double Rx_BF = transform[3];
      double Ry_BF = transform[4];
      double Rz_BF = transform[5];
      double M_BF  = transform[6];
      
      double x_out = M_BF*(       p.x - Rz_BF*p.y + Ry_BF*p.z) + Dx_BF;
      double y_out = M_BF*( Rz_BF*p.x +       p.y - Rx_BF*p.z) + Dy_BF;
      double z_out = M_BF*(-Ry_BF*p.x + Rx_BF*p.y +       p.z) + Dz_BF;

      p.x = x_out;
      p.y = y_out;
      p.z = z_out;
    }
  }
  public void transformToGeocentricFromWgs84(ProjCoordinate p) 
  {
    if( transform.length == 3 )
    {
      p.x -= transform[0];
      p.y -= transform[1];
      p.z -= transform[2];

    }
    else if (transform.length == 7)
    {
      double Dx_BF = transform[0];
      double Dy_BF = transform[1];
      double Dz_BF = transform[2];
      double Rx_BF = transform[3];
      double Ry_BF = transform[4];
      double Rz_BF = transform[5];
      double M_BF  = transform[6];
      
      double x_tmp = (p.x - Dx_BF) / M_BF;
      double y_tmp = (p.y - Dy_BF) / M_BF;
      double z_tmp = (p.z - Dz_BF) / M_BF;

      p.x =        x_tmp + Rz_BF*y_tmp - Ry_BF*z_tmp;
      p.y = -Rz_BF*x_tmp +       y_tmp + Rx_BF*z_tmp;
      p.z =  Ry_BF*x_tmp - Rx_BF*y_tmp +       z_tmp;
    }
  }
}
