package geotrellis.raster;

// ###############################################################################
// # Copyright (c) 2008 Klokan Petr Pridal. All rights reserved.
// #
// # Permission is hereby granted, free of charge, to any person obtaining a
// # copy of this software and associated documentation files (the "Software"),
// # to deal in the Software without restriction, including without limitation
// # the rights to use, copy, modify, merge, publish, distribute, sublicense,
// # and/or sell copies of the Software, and to permit persons to whom the
// # Software is furnished to do so, subject to the following conditions:
// #
// # The above copyright notice and this permission notice shall be included
// # in all copies or substantial portions of the Software.
// #
// # THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// # OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// # FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// # THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// # LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// # FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// # DEALINGS IN THE SOFTWARE.
// ###############################################################################

/**
 * Utility functions for working with web mercator tile systems.
 * 
 * Some code and documentation adapted from gdal2tiles.
 *
 * See this site for a great little intro:
 * http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection 
 *
 * It contains classes implementing coordinate conversions for:
 *    - GlobalMercator (based on EPSG:900913 = EPSG:3785)
 *        for Google Maps, Yahoo Maps, Microsoft Maps compatible tiles
 *   - GlobalGeodetic (based on EPSG:4326)
 *        for OpenLayers Base Map and Google Earth compatible tiles

 * More info at:

 * http://wiki.osgeo.org/wiki/Tile_Map_Service_Specification
 * http://wiki.osgeo.org/wiki/WMS_Tiling_Client_Recommendation
 * http://msdn.microsoft.com/en-us/library/bb259689.aspx
 * http://code.google.com/apis/maps/documentation/overlays.html#Google_Maps_Coordinates


 *     TMS Global Mercator Profile
 *     ---------------------------

 *     Functions necessary for generation of tiles in Spherical Mercator projection,
 *     EPSG:900913 (EPSG:gOOglE, Google Maps Global Mercator), EPSG:3785, OSGEO:41001.

 *     Such tiles are compatible with Google Maps, Microsoft Virtual Earth, Yahoo Maps,
 *     UK Ordnance Survey OpenSpace API, ...
 *     and you can overlay them on top of base maps of those web mapping applications.
    
 *     Pixel and tile coordinates are in TMS notation (origin [0,0] in bottom-left).

 *     What coordinate conversions do we need for TMS Global Mercator tiles::

 *          LatLon      <->       Meters      <->     Pixels    <->       Tile     

 *      WGS84 coordinates   Spherical Mercator  Pixels in pyramid  Tiles in pyramid
 *          lat/lon            XY in metres     XY pixels Z zoom      XYZ from TMS 
 *         EPSG:4326           EPSG:900913                                         
 *          .----.              ---------               --                TMS      
 *         /      \     <->     |       |     <->     /----/    <->      Google    
 *         \      /             |       |           /--------/          QuadTree   
 *          -----               ---------         /------------/                   
 *        KML, public         WebMapService         Web Clients      TileMapService

 *     What is the coordinate extent of Earth in EPSG:900913?

 *       [-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244]
 *       Constant 20037508.342789244 comes from the circumference of the Earth in meters,
 *       which is 40 thousand kilometers, the coordinate origin is in the middle of extent.
 *       In fact you can calculate the constant as: 2 * math.pi * 6378137 / 2.0
 *       $ echo 180 85 | gdaltransform -s_srs EPSG:4326 -t_srs EPSG:900913
 *       Polar areas with abs(latitude) bigger then 85.05112878 are clipped off.

 *     What are zoom level constants (pixels/meter) for pyramid with EPSG:900913?

 *       whole region is on top of pyramid (zoom=0) covered by 256x256 pixels tile,
 *       every lower zoom level resolution is always divided by two
 *       initialResolution = 20037508.342789244 * 2 / 256 = 156543.03392804062

 *     What is the difference between TMS and Google Maps/QuadTree tile name convention?

 *       The tile raster itself is the same (equal extent, projection, pixel size),
 *       there is just different identification of the same raster tile.
 *       Tiles in TMS are counted from [0,0] in the bottom-left corner, id is XYZ.
 *       Google placed the origin [0,0] to the top-left corner, reference is XYZ.
 *       Microsoft is referencing tiles by a QuadTree name, defined on the website:
 *       http://msdn2.microsoft.com/en-us/library/bb259689.aspx

 *     The lat/lon coordinates are using WGS84 datum, yeh?

 *       Yes, all lat/lon we are mentioning should use WGS84 Geodetic Datum.
 *       Well, the web clients like Google Maps are projecting those coordinates by
 *       Spherical Mercator, so in fact lat/lon coordinates on sphere are treated as if
 *       the were on the WGS84 ellipsoid.
     
 *       From MSDN documentation:
 *       To simplify the calculations, we use the spherical form of projection, not
 *       the ellipsoidal form. Since the projection is used only for map display,
 *       and not for displaying numeric coordinates, we don't need the extra precision
 *       of an ellipsoidal projection. The spherical projection causes approximately
 *       0.33 percent scale distortion in the Y direction, which is not visually noticable.

 *     How do I create a raster in EPSG:900913 and convert coordinates with PROJ.4?

 *       You can use standard GIS tools like gdalwarp, cs2cs or gdaltransform.
 *       All of the tools supports -t_srs 'epsg:900913'.

 *       For other GIS programs check the exact definition of the projection:
 *       More info at http://spatialreference.org/ref/user/google-projection/
 *       The same projection is degined as EPSG:3785. WKT definition is in the official
 *       EPSG database.

 *       Proj4 Text:
 *         +proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0
 *         +k=1.0 +units=m +nadgrids=@null +no_defs

 *       Human readable WKT format of EPGS:900913:
 *          PROJCS["Google Maps Global Mercator",
 *              GEOGCS["WGS 84",
 *                  DATUM["WGS_1984",
 *                      SPHEROID["WGS 84",6378137,298.2572235630016,
 *                          AUTHORITY["EPSG","7030"]],
 *                      AUTHORITY["EPSG","6326"]],
 *                  PRIMEM["Greenwich",0],
 *                  UNIT["degree",0.0174532925199433],
 *                  AUTHORITY["EPSG","4326"]],
 *              PROJECTION["Mercator_1SP"],
 *              PARAMETER["central_meridian",0],
 *              PARAMETER["scale_factor",1],
 *              PARAMETER["false_easting",0],
 *              PARAMETER["false_northing",0],
 *              UNIT["metre",1,
 *                  AUTHORITY["EPSG","9001"]]]
 */ 
 
object TileUtils { 
  val tileSize = 256
  // initial resolution = 156543.03392804062 
  val initialResolution = 2 * math.Pi * 6378137 / tileSize

  // origin shift = 20037508.342789244
  val originShift = 2 * math.Pi * 6378137 / 2.0

  /**
   * Converts given lat/lon in WGS84 Datum to XY in Spherical Mercator EPSG:900913
   */
  def LatLonToMeters(lat:Double, lon:Double) = {
        val mx = lon * originShift / 180.0
        val my1:Double = ( math.log( math.tan((90 + lat) * math.Pi / 360.0 )) / (math.Pi / 180.0) )
        val my = my1 * originShift / 180 

        (mx, my)
  }

  /**
   * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84 Datum
   */
  def MetersToLatLon(mx:Double, my:Double):(Double,Double) = { 
    val lon = (mx / originShift) * 180.0
    val lat1 = (my / originShift) * 180.0
    
    val lat = 180 / math.Pi * (2 * math.atan( math.exp( lat1 * math.Pi / 180.0)) - math.Pi / 2.0)
    (lat, lon)
 }
  
  /**
   * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
   */
  def PixelsToMeters(px:Int, py:Int, zoom:Int):(Double,Double) = {
    val res = Resolution( zoom )
    val mx = px * res - originShift
    val my = py * res - originShift
    (mx, my)
  }
  
  /**
   * Converts EPSG:900913 to pyramid pixel coordinates in given zoom level
   */
  def MetersToPixels(mx:Double, my:Double, zoom:Int):(Int,Int) = { 
    val res = Resolution( zoom )
    val px = ((mx + originShift) / res).toInt
    val py = ((my + originShift) / res).toInt
    (px, py)
  }
  
  /**
   * Returns a tile covering region in given pixel coordinates
   */
  def PixelsToTile(px:Int, py:Int) = {
    val tx = ( math.ceil( px / tileSize.toDouble ) - 1 ).toInt
    val ty = ( math.ceil( py / tileSize.toDouble ) - 1 ).toInt
    (tx, ty)
  }

  /**
   * Move the origin of pixel coordinates to top-left corner
   */
  def PixelsToRaster(px:Int, py:Int, zoom:Int) = { 
    val mapSize = tileSize << zoom
    (px, mapSize - py)
  }
  
  /**
   * "Returns tile for given mercator coordinates"
   */
  def MetersToTile(mx:Double, my:Double, zoom:Int) = {
    val (px, py) = MetersToPixels( mx, my, zoom)
    PixelsToTile(px, py)
  }

  /**
   * Returns bounds of the given tile in EPSG:900913 coordinates
   */
  def TileBounds(tx:Int, ty:Int, zoom:Int):(Double,Double,Double,Double) = {
    val (minx, miny) = PixelsToMeters( tx*tileSize, ty*tileSize, zoom )
    val (maxx, maxy) = PixelsToMeters( (tx+1)*tileSize, (ty+1)*tileSize, zoom )
    ( minx, miny, maxx, maxy )
  }

  /**
   * Returns bounds of the given tile in latutude/longitude using WGS84 datum
   */
  def TileLatLonBounds(tx:Int, ty:Int, zoom:Int) = {
    val (minx, miny, maxx, maxy) = TileBounds(tx, ty, zoom)
    val (minLat, minLon) = MetersToLatLon(minx, miny)
    val (maxLat, maxLon) = MetersToLatLon(maxx, maxy)
    
    ( minLat, minLon, maxLat, maxLon )
  }

  /**
   * Resolution (meters/pixel) for given zoom level (measured at Equator)
   */
  def Resolution(zoom:Int) = {
    initialResolution / math.pow(2,zoom)
  }
  
  /**
   * Maximal scaledown zoom of the pyramid closest to the pixelSize.
   */
  def ZoomForPixelSize(pixelSize:Double) = {
    for (i <- 1 until 30) { 
      if (pixelSize > Resolution(i)) {
        if (i != 0) i-1 else 0 // scaling down, not up
      }
    }
  }

  /**
   * Converts TMS tile coordinates to Google Tile coordinates
   * (again, see http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/)
   */
  def GoogleTile(tx:Int, ty:Int, zoom:Int) = {
    // coordinate origin is moved from bottom-left to top-left corner of the extent
    (tx, (math.pow(2,zoom) - 1) - ty)
  }
  
  /**
   * Converts TMS tile coordinates to Microsoft QuadTree
   */
  def QuadTree(tx:Int, ty:Int, zoom:Int) = {
    var quadKey = ""
    val tyFlipped = ((math.pow(2,zoom) - 1) - ty).toInt
    for ( i <- zoom until 0 by -1 ) {
      var digit = 0
      val mask = 1 << (i-1)
      if ((tx & mask) != 0) digit += 1
      if ((tyFlipped & mask) != 0) digit += 2
      quadKey += digit.toString()
    }
    quadKey
  }
}
