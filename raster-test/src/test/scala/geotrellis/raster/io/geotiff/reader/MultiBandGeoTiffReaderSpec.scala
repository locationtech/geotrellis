package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.testkit._

import org.scalatest._

// class MultiBandGeoTiffReaderSpec extends FunSpec 
//     with TestEngine
//     with GeoTiffTestUtils {

//   describe("Reading geotiffs with INTERLEAVE=PIXEL") {
//     ignore("Uncompressed, Stripped") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands.tif")).bands.map(_.tile).toArray

//       println("         PIXEL UNCOMPRESSED STRIPPED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     it("Uncompressed, Tiled") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-tiled.tif")).bands.map(_.tile).toArray

//       println("         PIXEL UNCOMPRESSED TILED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     ignore("COMPRESSION=DEFLATE, Stripped") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-deflate.tif")).bands.map(_.tile).toArray

//       println("         PIXEL COMPRESSED STRIPPED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     it("COMPRESSION=DEFLATE, Tiled") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-tiled-deflate.tif")).bands.map(_.tile).toArray

//       println("         PIXEL COMPRESSED TILED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }
//   }

//   describe("Reading geotiffs with INTERLEAVE=BANDS") {
//     ignore("Uncompressed, Stripped") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-interleave-bands.tif")).bands.map(_.tile).toArray

//       println("         BANDS UNCOMPRESSED STRIPPED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     ignore("Uncompressed, Tiled") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-tiled-interleave-bands.tif")).bands.map(_.tile).toArray
//       println("         BANDS UNCOMPRESSED TILED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     ignore("COMPRESSION=DEFLATE, Stripped") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-interleave-bands-deflate.tif")).bands.map(_.tile).toArray

//       println("         BANDS COMPRESSED STRIPPED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }

//     ignore("COMPRESSION=DEFLATE, Tiled") {
//       val tiles =
//         GeoTiff(geoTiffPath("3bands/3bands-tiled-interleave-bands-deflate.tif")).bands.map(_.tile).toArray

//       println("         BANDS COMPRESSED TILED")
//       println(tiles(0).asciiDraw)
//       println(tiles(1).asciiDraw)
//       println(tiles(2).asciiDraw)

//       // tiles(0).foreach { z => z should be (1) }
//       // tiles(1).foreach { z => z should be (2) }
//       // tiles(2).foreach { z => z should be (3) }
//     }
//   }
// }
