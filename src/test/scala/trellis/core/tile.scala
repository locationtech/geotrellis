package trellis.core


import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

import trellis.raster.TileUtils._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TileSpec extends Spec with MustMatchers {
  describe("Web Mercator Tile Utilities") {

    // 340 n 12th St
    val sampleLatLng = (39.95877,-75.15837)
    val (sampleLat, sampleLng) = sampleLatLng
    val sampleMapCoords = (-8366591.477252449,4859952.655881124)

    it("should convert latlng to map XY (web mercator coordinates)") {
      LatLonToMeters(sampleLat, sampleLng) must be === (-8366591.477252449,4859952.655881124)
    }

    it("should convert XY to latlng") {
      val (lat,lng) = MetersToLatLon(-8366591.477252449,4859952.655881124)
      "%.4f".format(lat) must be === "%.4f".format(sampleLat)
      "%.4f".format(lng) must be === "%.4f".format(sampleLng)
    }

    it("should convert pixels to XY") {
      PixelsToMeters(240,100,16) must be === (-2.0036935065077104E7,-2.0037269477075852E7)
    }
    
    it("should convert map XY to pixels at a zoom level") {
      MetersToPixels(0, 0, 16) must be ===  (8388608,8388608)
    }

    it("should return a tile covering the region with the given pixel coords") {
      PixelsToTile(1,1) must be === (0,0)
    }

    it("should return a tile for given coordinates in web mercator") {
      MetersToTile(-8367050,4859888,16) must be === (19085,40715)
    }

    it("should return the bounds of a given tile in web mercator coordinates") {
      val (minx, miny, maxx, maxy) = TileBounds(19085,40715,16)
      minx must be === -8367102.864208534
      maxx must be === -8366491.367982252
      miny must be === 4859560.510258365
      maxy must be === 4860172.006484646
    }
    
    it("should return the bounds of a tile in latlng") {
      val (minLat, minLon, maxLat, maxLon) = TileLatLonBounds(19085,40715,16) 
      "%.4f".format(minLat) must be === "39.9561"
      "%.4f".format(maxLat) must be === "39.9603"
      "%.4f".format(minLon) must be === "-75.1630"
      "%.4f".format(maxLon) must be === "-75.1575"
    }
  }
}

