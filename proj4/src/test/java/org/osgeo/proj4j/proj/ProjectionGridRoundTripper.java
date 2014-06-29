package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.util.ProjectionUtil;

public class ProjectionGridRoundTripper 
{
	private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
  CRSFactory csFactory = new CRSFactory();

  static final String WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +datum=WGS84 +units=degrees";
  CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", WGS84_PARAM);

	private CoordinateReferenceSystem cs;
  private CoordinateTransform transInverse;
  private CoordinateTransform transForward;
	private int gridSize = 4;
	private boolean debug = false;
	private int transformCount = 0;
	private double[] gridExtent;
	
	public ProjectionGridRoundTripper(CoordinateReferenceSystem cs)
	{
		this.cs = cs;
    transInverse = ctFactory.createTransform(cs, WGS84); 
    transForward = ctFactory.createTransform(WGS84, cs); 
	}
	
	public void setLevelDebug(boolean debug)
	{
		this.debug = debug;
	}
	
	public int getTransformCount()
	{
		return transformCount;
	}
	
	public double[] getExtent()
	{
		return gridExtent;
	}
	public boolean runGrid(double tolerance)
	{
		boolean isWithinTolerance = true;
		
		gridExtent = gridExtent(cs.getProjection());
		double minx = gridExtent[0];
		double miny = gridExtent[1];
		double maxx = gridExtent[2];
		double maxy = gridExtent[3];
		
    ProjCoordinate p = new ProjCoordinate();
		double dx = (maxx - minx) / gridSize;
		double dy = (maxy - miny) / gridSize;
		for (int ix = 0; ix <= gridSize; ix++) {
			for (int iy = 0; iy <= gridSize; iy++) {
				 p.x = ix == gridSize ?
					 		maxx
					 		: minx + ix * dx;

				 p.y = iy == gridSize ?
						 	maxy
					 		: miny + iy * dy;
					 		
				 boolean isWithinTol = roundTrip(p, tolerance);
				 if (! isWithinTol)
					 return false;
			}
		}
		return true;
	}
	
  ProjCoordinate p2 = new ProjCoordinate();
  ProjCoordinate p3 = new ProjCoordinate();

	private boolean roundTrip(ProjCoordinate p, double tolerance)
	{
		transformCount++;
		
    transForward.transform(p, p2);
    transInverse.transform(p2, p3);
		
		if (debug) 
			System.out.println(ProjectionUtil.toString(p) + " -> " + ProjectionUtil.toString(p2) + " ->  " + ProjectionUtil.toString(p3));
		
		double dx = Math.abs(p3.x - p.x);
		double dy = Math.abs(p3.y - p.y);
		
    boolean isInTol = dx <= tolerance && dy <= tolerance;
    
    if (! isInTol) 
      System.out.println("FAIL: " + ProjectionUtil.toString(p) + " -> " + ProjectionUtil.toString(p2) + " ->  " + ProjectionUtil.toString(p3));

    
		return isInTol;
	}
	
	public static double[] gridExtent(Projection proj)
	{
		// scan all lat/lon params to try and determine a reasonable extent
		
		double lon = proj.getProjectionLongitudeDegrees();
		
		double[] latExtent = new double[] {Double.MAX_VALUE, Double.MIN_VALUE };
		updateLat(proj.getProjectionLatitudeDegrees(), latExtent);
		updateLat(proj.getProjectionLatitude1Degrees(), latExtent);
		updateLat(proj.getProjectionLatitude2Degrees(), latExtent);
			
		double centrex = lon;
		double centrey = 0.0;
		double gridWidth = 10;
		
		if (latExtent[0] < Double.MAX_VALUE && latExtent[1] > Double.MIN_VALUE) {
			// got a good candidate
			
			double dlat = latExtent[1] - latExtent[0];
			if (dlat > 0) gridWidth = 2 * dlat;
		  centrey = (latExtent[1] + latExtent[0]) /2;
		}
		double[] extent = new double[4];
		extent[0] = centrex - gridWidth/2;
		extent[1] = centrey - gridWidth/2;
		extent[2] = centrex + gridWidth/2;
		extent[3] = centrey + gridWidth/2;
		return extent;
	}
	
	private static void updateLat(double lat, double[] latExtent)
	{
		// 0.0 indicates not set (for most projections?)
		if (lat == 0.0) return;
		if (lat < latExtent[0])
			latExtent[0] = lat;
		if (lat > latExtent[1])
			latExtent[1] = lat;
	}
}
