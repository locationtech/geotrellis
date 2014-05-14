package org.osgeo.proj4j.parser;

import java.util.HashMap;
import java.util.Map;

import org.osgeo.proj4j.*;
import org.osgeo.proj4j.datum.Datum;
import org.osgeo.proj4j.datum.Ellipsoid;
import org.osgeo.proj4j.proj.Projection;
import org.osgeo.proj4j.proj.TransverseMercatorProjection;
import org.osgeo.proj4j.units.Angle;
import org.osgeo.proj4j.units.AngleFormat;
import org.osgeo.proj4j.units.Unit;
import org.osgeo.proj4j.units.Units;
import org.osgeo.proj4j.util.ProjectionMath;

public class Proj4Parser 
{
  private Registry registry;
  
  public Proj4Parser(Registry registry) {
    this.registry = registry;
  }
  
  public CoordinateReferenceSystem parse(String name, String[] args)
  {
    if (args == null)
      return null;
    
    Map params = createParameterMap(args);
    Proj4Keyword.checkUnsupported(params.keySet());
    DatumParameters datumParam = new DatumParameters();
    parseDatum(params, datumParam);
    parseEllipsoid(params, datumParam);
    Datum datum = datumParam.getDatum();
    Ellipsoid ellipsoid = datum.getEllipsoid(); 
    // TODO: this makes a difference - why?
    // which is better?
//    Ellipsoid ellipsoid = datumParam.getEllipsoid(); 
    Projection proj = parseProjection(params, ellipsoid);
    return new CoordinateReferenceSystem(name, args, datum, proj);
  }
  
 /**
  * Creates a {@link Projection}
  * initialized from a PROJ.4 argument list.
  */
 private Projection parseProjection( Map params, Ellipsoid ellipsoid ) {
   Projection projection = null;

   String s;
   s = (String)params.get( Proj4Keyword.proj );
   if ( s != null ) {
     projection = registry.getProjection( s );
     if ( projection == null )
       throw new InvalidValueException( "Unknown projection: "+s );
   }

   projection.setEllipsoid(ellipsoid);
   
   //TODO: better error handling for things like bad number syntax.  
   // Should be able to report the original param string in the error message
   // Should the exception be lib-specific?  (e.g. ParseException)
   
   s = (String)params.get( Proj4Keyword.alpha );
   if ( s != null ) 
     projection.setAlphaDegrees( Double.parseDouble( s ) );
   
   s = (String)params.get( Proj4Keyword.lonc );
   if ( s != null ) 
     projection.setLonCDegrees( Double.parseDouble( s ) );
   
   s = (String)params.get( Proj4Keyword.lat_0 );
   if ( s != null ) 
     projection.setProjectionLatitudeDegrees( parseAngle( s ) );
   
   s = (String)params.get( Proj4Keyword.lon_0 );
   if ( s != null ) 
     projection.setProjectionLongitudeDegrees( parseAngle( s ) );
   
   s = (String)params.get( Proj4Keyword.lat_1 );
   if ( s != null ) 
     projection.setProjectionLatitude1Degrees( parseAngle( s ) );
   
   s = (String)params.get( Proj4Keyword.lat_2 );
   if ( s != null ) 
     projection.setProjectionLatitude2Degrees( parseAngle( s ) );
   
   s = (String)params.get( Proj4Keyword.lat_ts );
   if ( s != null ) 
     projection.setTrueScaleLatitudeDegrees( parseAngle( s ) );
   
   s = (String)params.get( Proj4Keyword.x_0 );
   if ( s != null ) 
     projection.setFalseEasting( Double.parseDouble( s ) );
   
   s = (String)params.get( Proj4Keyword.y_0 );
   if ( s != null ) 
     projection.setFalseNorthing( Double.parseDouble( s ) );

   s = (String)params.get( Proj4Keyword.k_0 );
   if ( s == null ) 
     s = (String)params.get( Proj4Keyword.k );
   if ( s != null ) 
     projection.setScaleFactor( Double.parseDouble( s ) );

   s = (String)params.get( Proj4Keyword.units );
   if ( s != null ) {
     Unit unit = Units.findUnits( s );
     // TODO: report unknown units name as error
     if ( unit != null ) {
       projection.setFromMetres( 1.0 / unit.value );
       projection.setUnits( unit );
     }
   }
   
   s = (String)params.get( Proj4Keyword.to_meter );
   if ( s != null ) 
     projection.setFromMetres( 1.0/Double.parseDouble( s ) );

   if ( params.containsKey( Proj4Keyword.south ) ) 
     projection.setSouthernHemisphere(true);

   //TODO: implement some of these parameters ?
     
   // this must be done last, since behaviour depends on other params being set (eg +south)
   if (projection instanceof TransverseMercatorProjection) {
     s = (String) params.get(Proj4Keyword.zone);
     if (s != null)
       ((TransverseMercatorProjection) projection).setUTMZone(Integer
           .parseInt(s));
   }

   projection.initialize();

   return projection;
 }

 private void parseDatum(Map params, DatumParameters datumParam) 
 {
   String towgs84 = (String) params.get(Proj4Keyword.towgs84);
   if (towgs84 != null) {
     double[] datumConvParams = parseToWGS84(towgs84); 
     datumParam.setDatumTransform(datumConvParams);
   }

   String code = (String) params.get(Proj4Keyword.datum);
   if (code != null) {
     Datum datum = registry.getDatum(code);
     if (datum == null)
       throw new InvalidValueException("Unknown datum: " + code);
     datumParam.setDatum(datum);
   }
   
 }
 
 private double[] parseToWGS84(String paramList)
 {
   String[] numStr = paramList.split(",");
   
   if (! (numStr.length == 3 || numStr.length == 7)) {
     throw new InvalidValueException("Invalid number of values (must be 3 or 7) in +towgs84: " + paramList);
   }
   double[] param = new double[numStr.length];
   for (int i = 0; i < numStr.length; i++) {
     // TODO: better error reporting
     param[i] = Double.parseDouble(numStr[i]);
   }
   if (param.length > 3) {
     // optimization to detect 3-parameter transform
     if (param[3] == 0.0 
         && param[4] == 0.0 
         && param[5] == 0.0 
         && param[6] == 0.0 
         ) {
       param = new double[] { param[0], param[1], param[2] };
     }
   }
   
   /**
    * PROJ4 towgs84 7-parameter transform uses 
    * units of arc-seconds for the rotation factors, 
    * and parts-per-million for the scale factor.
    * These need to be converted to radians and a scale factor. 
    */
   if (param.length > 3) {
     param[3] *= ProjectionMath.SECONDS_TO_RAD;
     param[4] *= ProjectionMath.SECONDS_TO_RAD;
     param[5] *= ProjectionMath.SECONDS_TO_RAD;
     param[6] = (param[6]/ProjectionMath.MILLION) + 1;
   }
   
   return param;
 }
 
 private void parseEllipsoid(Map params, DatumParameters datumParam) 
 {
   double b = 0;
   String s;

   /*
    * // not supported by PROJ4 s = (String) params.get(Proj4Param.R); if (s !=
    * null) a = Double.parseDouble(s);
    */

   String code = (String) params.get(Proj4Keyword.ellps);
   if (code != null) {
     Ellipsoid ellipsoid = registry.getEllipsoid(code);
     if (ellipsoid == null)
       throw new InvalidValueException("Unknown ellipsoid: " + code);
     datumParam.setEllipsoid(ellipsoid);
   }

   /*
    * Explicit parameters override ellps and datum settings
    */
   s = (String) params.get(Proj4Keyword.a);
   if (s != null) {
     double a = Double.parseDouble(s);
     datumParam.setA(a);
   }
   
   s = (String) params.get(Proj4Keyword.es);
   if (s != null) {
     double es = Double.parseDouble(s);
     datumParam.setES(es);
   }

   s = (String) params.get(Proj4Keyword.rf);
   if (s != null) {
     double rf = Double.parseDouble(s);
     datumParam.setRF(rf);
   }

   s = (String) params.get(Proj4Keyword.f);
   if (s != null) {
     double f = Double.parseDouble(s);
     datumParam.setF(f);
   }

   s = (String) params.get(Proj4Keyword.b);
   if (s != null) {
     b = Double.parseDouble(s);
     datumParam.setB(b);
   }

   if (b == 0) {
     b = datumParam.getA() * Math.sqrt(1. - datumParam.getES());
   }

   parseEllipsoidModifiers(params, datumParam);

   /*
    * // None of these appear to be supported by PROJ4 ??
    * 
    * s = (String)
    * params.get(Proj4Param.R_A); if (s != null && Boolean.getBoolean(s)) { a *=
    * 1. - es * (SIXTH + es * (RA4 + es * RA6)); } else { s = (String)
    * params.get(Proj4Param.R_V); if (s != null && Boolean.getBoolean(s)) { a *=
    * 1. - es * (SIXTH + es * (RV4 + es * RV6)); } else { s = (String)
    * params.get(Proj4Param.R_a); if (s != null && Boolean.getBoolean(s)) { a =
    * .5 * (a + b); } else { s = (String) params.get(Proj4Param.R_g); if (s !=
    * null && Boolean.getBoolean(s)) { a = Math.sqrt(a * b); } else { s =
    * (String) params.get(Proj4Param.R_h); if (s != null &&
    * Boolean.getBoolean(s)) { a = 2. * a * b / (a + b); es = 0.; } else { s =
    * (String) params.get(Proj4Param.R_lat_a); if (s != null) { double tmp =
    * Math.sin(parseAngle(s)); if (Math.abs(tmp) > MapMath.HALFPI) throw new
    * ProjectionException("-11"); tmp = 1. - es * tmp * tmp; a *= .5 * (1. - es +
    * tmp) / (tmp * Math.sqrt(tmp)); es = 0.; } else { s = (String)
    * params.get(Proj4Param.R_lat_g); if (s != null) { double tmp =
    * Math.sin(parseAngle(s)); if (Math.abs(tmp) > MapMath.HALFPI) throw new
    * ProjectionException("-11"); tmp = 1. - es * tmp * tmp; a *= Math.sqrt(1. -
    * es) / tmp; es = 0.; } } } } } } } }
    */
 }
 
 /**
  * Parse ellipsoid modifiers.
  * 
  * @param params
  * @param datumParam
  */
 private void parseEllipsoidModifiers(Map params, DatumParameters datumParam) 
 {
   /**
    * Modifiers are mutually exclusive, so when one is detected method returns
    */
   if ( params.containsKey( Proj4Keyword.R_A ) ) {
     datumParam.setR_A();
     return;
   }

 }
 
 private Map createParameterMap(String[] args) {
   Map params = new HashMap();
   for (int i = 0; i < args.length; i++) {
     String arg = args[i];
     // strip leading "+" if any
     if (arg.startsWith("+")) {
       arg = arg.substring(1);
     }
     int index = arg.indexOf('=');
     if (index != -1) {
       // param of form pppp=vvvv
       String key = arg.substring(0, index);
       String value = arg.substring(index + 1);
       params.put(key, value);
     } else {
       // param of form ppppp
       //String key = arg.substring(1);
       params.put(arg, null);
     }
   }
   return params;
 }

 private static double parseAngle( String s ) {
   return Angle.parse(s);
 }

}
