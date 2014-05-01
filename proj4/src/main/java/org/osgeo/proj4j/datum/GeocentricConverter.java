package org.osgeo.proj4j.datum;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.util.ProjectionMath;

/**
 *  Provides conversions between Geodetic coordinates 
 *  (latitude, longitude in radians and height in meters) 
 *  and Geocentric coordinates
 *  (X, Y, Z) in meters.
 *  <p>
 *  Provenance: Ported from GEOCENTRIC by the U.S. Army Topographic Engineering Center via PROJ.4
 *    
 * @author Martin Davis
 *
 */
public class GeocentricConverter 
{
  /*
   * 
   * REFERENCES
   *    
   *    An Improved Algorithm for Geocentric to Geodetic Coordinate Conversion,
   *    Ralph Toms, February 1996  UCRL-JC-123138.
   *    
   *    Further information on GEOCENTRIC can be found in the Reuse Manual.
   *
   *    GEOCENTRIC originated from : U.S. Army Topographic Engineering Center
   *                                 Geospatial Information Division
   *                                 7701 Telegraph Road
   *                                 Alexandria, VA  22310-3864
   *
   * LICENSES
   *
   *    None apply to this component.
   *
   * RESTRICTIONS
   *
   *    GEOCENTRIC has no restrictions.
   */

  double a;
  double b;
  double a2;
  double b2;
  double e2;
  double ep2;
  
  public GeocentricConverter(Ellipsoid ellipsoid) {
    this(ellipsoid.getA(), ellipsoid.getB());
  }
  public GeocentricConverter(double a, double b) {
    this.a = a;
    this.b = b;
    a2 = a * a;
    b2 = b * b;
    e2 = (a2 - b2) / a2;
    ep2 = (a2 - b2) / b2;
  }

  /**
   * Converts geodetic coordinates
   * (latitude, longitude, and height) to geocentric coordinates (X, Y, Z),
   * according to the current ellipsoid parameters.
   *
   *    Latitude  : Geodetic latitude in radians                     (input)
   *    Longitude : Geodetic longitude in radians                    (input)
   *    Height    : Geodetic height, in meters                       (input)
   *    
   *    X         : Calculated Geocentric X coordinate, in meters    (output)
   *    Y         : Calculated Geocentric Y coordinate, in meters    (output)
   *    Z         : Calculated Geocentric Z coordinate, in meters    (output)
   *
   */
  public void convertGeodeticToGeocentric(ProjCoordinate p)
  {
    double Longitude = p.x;
    double Latitude = p.y;
    double Height = p.hasValidZOrdinate() ? p.z : 0;   //Z value not always supplied
    double X;  // output
    double Y;
    double Z;

    double Rn;            /*  Earth radius at location  */
    double Sin_Lat;       /*  Math.sin(Latitude)  */
    double Sin2_Lat;      /*  Square of Math.sin(Latitude)  */
    double Cos_Lat;       /*  Math.cos(Latitude)  */

    /*
    ** Don't blow up if Latitude is just a little out of the value
    ** range as it may just be a rounding issue.  Also removed longitude
    ** test, it should be wrapped by Math.cos() and Math.sin().  NFW for PROJ.4, Sep/2001.
    */
    if( Latitude < -ProjectionMath.HALFPI && Latitude > -1.001 * ProjectionMath.HALFPI ) {
        Latitude = -ProjectionMath.HALFPI;
    } else if( Latitude > ProjectionMath.HALFPI && Latitude < 1.001 * ProjectionMath.HALFPI ) {
        Latitude = ProjectionMath.HALFPI;
    } else if ((Latitude < -ProjectionMath.HALFPI) || (Latitude > ProjectionMath.HALFPI)) {
      /* Latitude out of range */
      throw new IllegalStateException("Latitude is out of range: " + Latitude);
    }

    if (Longitude > ProjectionMath.PI) Longitude -= (2*ProjectionMath.PI);
    Sin_Lat = Math.sin(Latitude);
    Cos_Lat = Math.cos(Latitude);
    Sin2_Lat = Sin_Lat * Sin_Lat;
    Rn = a / (Math.sqrt(1.0e0 - e2 * Sin2_Lat));
    X = (Rn + Height) * Cos_Lat * Math.cos(Longitude);
    Y = (Rn + Height) * Cos_Lat * Math.sin(Longitude);
    Z = ((Rn * (1 - e2)) + Height) * Sin_Lat;

    p.x = X;
    p.y = Y;
    p.z = Z;
  }
  
  public void convertGeocentricToGeodetic(ProjCoordinate p)
  {
    convertGeocentricToGeodeticIter(p);
  }
    
  public void convertGeocentricToGeodeticIter(ProjCoordinate p)
  {
  	/* local defintions and variables */
  	/* end-criterium of loop, accuracy of sin(Latitude) */
  	double genau = 1.E-12;
  	double genau2 = (genau*genau);
  	int maxiter = 30;

  	double P;        /* distance between semi-minor axis and location */
  	double RR;       /* distance between center and location */
  	double CT;       /* sin of geocentric latitude */
  	double ST;       /* cos of geocentric latitude */
  	double RX;
  	double RK;
  	double RN;       /* Earth radius at location */
  	double CPHI0;    /* cos of start or old geodetic latitude in iterations */
  	double SPHI0;    /* sin of start or old geodetic latitude in iterations */
  	double CPHI;     /* cos of searched geodetic latitude */
  	double SPHI;     /* sin of searched geodetic latitude */
  	double SDPHI;    /* end-criterium: addition-theorem of sin(Latitude(iter)-Latitude(iter-1)) */
  	boolean At_Pole;     /* indicates location is in polar region */
  	int iter;        /* # of continous iteration, max. 30 is always enough (s.a.) */

  	double X = p.x;
  	double Y = p.y;
  	double Z = p.hasValidZOrdinate() ? p.z : 0;   //Z value not always supplied
  	double Longitude;
  	double Latitude;
  	double Height;

  	At_Pole = false;
  	P = Math.sqrt(X*X+Y*Y);
  	RR = Math.sqrt(X*X+Y*Y+Z*Z);

  	/*      special cases for latitude and longitude */
  	if (P/this.a < genau) {

  		/*  special case, if P=0. (X=0., Y=0.) */
  		At_Pole = true;
  		Longitude = 0.0;

  		/*  if (X,Y,Z)=(0.,0.,0.) then Height becomes semi-minor axis
  		 *  of ellipsoid (=center of mass), Latitude becomes PI/2 */
  		if (RR/this.a < genau) {
  			Latitude = ProjectionMath.HALFPI;
  			Height   = -this.b;
  			return;
  		}
  	} else {
  		/*  ellipsoidal (geodetic) longitude
  		 *  interval: -PI < Longitude <= +PI */
  		Longitude=Math.atan2(Y,X);
  	}

  	/* --------------------------------------------------------------
  	 * Following iterative algorithm was developped by
  	 * "Institut fur Erdmessung", University of Hannover, July 1988.
  	 * Internet: www.ife.uni-hannover.de
  	 * Iterative computation of CPHI,SPHI and Height.
  	 * Iteration of CPHI and SPHI to 10**-12 radian resp.
  	 * 2*10**-7 arcsec.
  	 * --------------------------------------------------------------
  	 */
  	CT = Z/RR;
  	ST = P/RR;
  	RX = 1.0/Math.sqrt(1.0-this.e2*(2.0-this.e2)*ST*ST);
  	CPHI0 = ST*(1.0-this.e2)*RX;
  	SPHI0 = CT*RX;
  	iter = 0;

  	/* loop to find sin(Latitude) resp. Latitude
  	 * until |sin(Latitude(iter)-Latitude(iter-1))| < genau */
  	do
  	{
  		iter++;
  		RN = this.a/Math.sqrt(1.0-this.e2*SPHI0*SPHI0);

  		/*  ellipsoidal (geodetic) height */
  		Height = P*CPHI0+Z*SPHI0-RN*(1.0-this.e2*SPHI0*SPHI0);

  		RK = this.e2*RN/(RN+Height);
  		RX = 1.0/Math.sqrt(1.0-RK*(2.0-RK)*ST*ST);
  		CPHI = ST*(1.0-RK)*RX;
  		SPHI = CT*RX;
  		SDPHI = SPHI*CPHI0-CPHI*SPHI0;
  		CPHI0 = CPHI;
  		SPHI0 = SPHI;
  	}
  	while (SDPHI*SDPHI > genau2 && iter < maxiter);

  	/*      ellipsoidal (geodetic) latitude */
  	Latitude=Math.atan(SPHI/Math.abs(CPHI));

  	p.x = Longitude;
  	p.y = Latitude;
  	p.z = Height;
  }
  
  //TODO: port non-iterative algorithm????
}
