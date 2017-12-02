package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

import java.net.URI
import scala.reflect.ClassTag

object Implicits extends Implicits

// TODO: remove asInstanceOf casts
trait Implicits {
  implicit class withSinglebandGeoTiffConstructMethods(val self: Tile) extends GeoTiffConstructMethods[Tile] {
    def toGeoTiff[K](
      nextLayout: LayoutDefinition,
      gb: GridBounds,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[Tile]] = Nil
    ): SinglebandGeoTiff = {
      SinglebandGeoTiff(
        GeoTiffTile(self.crop(nextLayout.mapTransform(gb), md.extent, Crop.Options(clamp = false)), options),
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews.map(_.asInstanceOf[SinglebandGeoTiff])
      )
    }
  }

  implicit class withMultibandGeoTiffConstructMethods(val self: MultibandTile) extends GeoTiffConstructMethods[MultibandTile] {
    def toGeoTiff[K](
      nextLayout: LayoutDefinition,
      gb: GridBounds,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[MultibandTile]] = Nil
    ): MultibandGeoTiff = {
      MultibandGeoTiff(
        GeoTiffMultibandTile(self.crop(nextLayout.mapTransform(gb), md.extent, Crop.Options(clamp = false)), options),
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews.map(_.asInstanceOf[MultibandGeoTiff])
      )
    }
  }

  implicit class withSinglebandGeoTiffSegmentConstructMethods[K](val self: Iterable[(K, Tile)])
                                                                (implicit val spatialComponent: SpatialComponent[K]) extends GeoTiffSegmentConstructMethods[K, Tile] {
    // TODO: consider moving this code somewhere else, it's in fact a bit modified GeoTiffTile.apply function code
    def toGeoTiff(
      nextLayout: LayoutDefinition,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[Tile]] = Nil
    ): SinglebandGeoTiff = {
      val gb = md.gridBounds
      val (layoutCols, layoutRows) = gb.width * nextLayout.tileCols -> gb.height * nextLayout.tileRows

      /*println(s"(layoutCols, layoutRows): ${(layoutCols, layoutRows)}")
      println(s"${gb.width} * ${nextLayout.tileCols} -> ${gb.height} * ${nextLayout.tileRows}")*/

      val re = RasterExtent(nextLayout.mapTransform(gb), layoutCols, layoutRows)
      val gridBounds = re.gridBoundsFor(md.extent, clamp = false)

      val geoTiffTile: GeoTiffTile = {
        val segmentLayout = GeoTiffSegmentLayout(layoutCols, layoutRows, options.storageMethod, BandInterleave, BandType.forCellType(md.cellType))

        //println(s"segmentLayout: $segmentLayout")

        val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
        val compressor = options.compression.createCompressor(segmentCount)

        val segments: Map[Int, Array[Byte]] =
          self
            .map { case (k, v) => k.getComponent[SpatialKey] -> v }
            .toList
            .sortBy(_._1)
            .map { case (key, tile) =>
              val spatialKey = key.getComponent[SpatialKey]
              val updateCol = (spatialKey.col - gb.colMin) * md.tileLayout.tileCols
              val updateRow = (spatialKey.row - gb.rowMin) * md.tileLayout.tileRows
              val index = segmentLayout.getSegmentIndex(updateCol, updateRow)

              /*println(s"key($index): $key")
              println(s"tile.dimensions($index): ${tile.dimensions}")
              println(s"tile.findMinMaxDouble($index): ${tile.findMinMaxDouble}")
              println(s"(updateCol, updateRow)($index): ${(updateCol, updateRow)}")
              println(s"segmentBounds: ${segmentBounds}")*/

              index -> compressor.compress(tile.toBytes, index)
            }
            .toMap

        val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)
        cfor(0)(_ < segmentCount, _ + 1) { i =>
          segmentBytes(i) = segments.getOrElse(i, compressor.compress(ArrayTile.empty(md.cellType, md.tileLayout.tileCols, md.tileLayout.tileRows).toBytes, i))
        }

         GeoTiffTile(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, md.cellType)
      }

      /*(0 to geoTiffTile.segmentCount) foreach { case i =>
        println(s"geoTiffTile.getGridBounds($i): ${geoTiffTile.getGridBounds(i)}")
      }

      println(s"geoTiffTile.getIntersectingSegments(gridBounds).sorted: ${geoTiffTile.getIntersectingSegments(gridBounds).toList.sorted}")

      println(s"gridBounds: ${gridBounds}")
      println(s"gridBounds: ${geoTiffTile.gridBounds}")*/

      /*(0 to 15) foreach { case i =>
        println(s"geoTiffTile.getGridBounds($i).crop(gridBounds): ${geoTiffTile.crop(gridBounds).getGridBounds(i)}")
      }*/

      SinglebandGeoTiff(
        geoTiffTile, //.crop(gridBounds), // impossible to read by segments with this crop function applied
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews.map(_.asInstanceOf[SinglebandGeoTiff])
      )

      /*SinglebandGeoTiff(
        geoTiffTile.crop(gridBounds), // impossible to read by segments with this crop function applied
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews.map(_.asInstanceOf[SinglebandGeoTiff])
      )*/
    }
  }

  implicit class withMultibandGeoTiffSegmentConstructMethods[K](val self: Iterable[(K, MultibandTile)])
                                                               (implicit val spatialComponent: SpatialComponent[K]) extends GeoTiffSegmentConstructMethods[K, MultibandTile] {
    // TODO: consider moving this code somewhere else, it's in fact a bit modified GeoTiffMultibandTile.apply function code
    def toGeoTiff(
      nextLayout: LayoutDefinition,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[MultibandTile]] = Nil
    ): MultibandGeoTiff = {
      val geoTiffTile: GeoTiffMultibandTile = {
        val bandCount = self.head._2.bandCount

        // TODO: Handle band interleave construction.
        val (layoutCols, layoutRows) = gb.width * nextLayout.tileCols -> gb.height * nextLayout.tileRows

        val segmentLayout = GeoTiffSegmentLayout(layoutCols, layoutRows, options.storageMethod, PixelInterleave, BandType.forCellType(md.cellType))

        val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
        val compressor = options.compression.createCompressor(segmentCount)

        val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)
        val segmentTiles = Array.ofDim[Array[Tile]](segmentCount)

        segmentLayout.interleaveMethod match {
          case PixelInterleave => {
            cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
              val bandTiles =
                self
                  .map { case (key, tile) =>
                    val spatialKey = key.getComponent[SpatialKey]
                    val updateCol = (spatialKey.col - gb.colMin) * md.tileLayout.tileCols
                    val updateRow = (spatialKey.row - gb.rowMin) * md.tileLayout.tileRows
                    val index = segmentLayout.getSegmentIndex(updateCol, updateRow)

                    index -> tile.band(bandIndex)
                  }.toMap

              cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
                val bandTile = bandTiles.getOrElse(segmentIndex, ArrayTile.empty(md.cellType, md.tileLayout.tileCols, md.tileLayout.tileRows))
                if (bandIndex == 0) {
                  segmentTiles(segmentIndex) = Array.ofDim[Tile](bandCount)
                }
                segmentTiles(segmentIndex)(bandIndex) = bandTile
              }
            }

            val byteCount = md.cellType.bytes

            cfor(0)(_ < segmentCount, _ + 1) { i =>
              val tiles = segmentTiles(i)
              val cols = tiles(0).cols
              val rows = tiles(0).rows
              val segBytes = Array.ofDim[Byte](cols * rows * bandCount * byteCount)

              val tileBytes = Array.ofDim[Array[Byte]](bandCount)
              cfor(0)(_ < bandCount, _ + 1) { b =>
                tileBytes(b) = tiles(b).toBytes
              }

              var segmentIndex = 0
              cfor(0)(_ < cols * rows, _ + 1) { cellIndex =>
                cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
                  cfor(0)(_ < byteCount, _ + 1) { b =>
                    val bytes = tileBytes(bandIndex)
                    segBytes(segmentIndex) = bytes(cellIndex * byteCount + b)
                    segmentIndex += 1
                  }
                }
              }

              segmentBytes(i) = compressor.compress(segBytes, i)
            }
          }

          case BandInterleave =>
            throw new Exception("Band interleave construction is not supported yet.")
        }

        GeoTiffMultibandTile(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, bandCount, md.cellType)
      }

      val re = RasterExtent(nextLayout.mapTransform(gb), geoTiffTile.cols, geoTiffTile.rows)
      val gridBounds = re.gridBoundsFor(md.extent, clamp = false)

      MultibandGeoTiff(
        geoTiffTile.crop(gridBounds),
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews.map(_.asInstanceOf[MultibandGeoTiff])
      )
    }
  }

  implicit class withCOGLayerWriteMethods[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](val self: RDD[(K, GeoTiff[V])]) extends MethodExtensions[RDD[(K, GeoTiff[V])]] {
    def write(keyIndex: KeyIndex[K], uri: URI): Unit =
      COGLayer.write[K, V](self)(keyIndex, uri)
  }
}
