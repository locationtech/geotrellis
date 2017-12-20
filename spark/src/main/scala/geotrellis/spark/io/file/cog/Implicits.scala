package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.util.Filesystem

import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI

trait Implicits extends Serializable {
  implicit val fileSinglebandCOGRDDReader: FileCOGRDDReader[Tile] =
    new FileCOGRDDReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val tiff = GeoTiffReader.readSingleband(uri.getPath, false, true)

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: Map[GridBounds, K]): Vector[(K, Tile)] = {
        tiff.tile match {
          case gtTile: GeoTiffTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val fileSinglebandCOGRDDReader2: FileCOGRDDReader2[Tile] =
    new FileCOGRDDReader2[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val tiff = GeoTiffReader.readSingleband(uri.getPath, false, true)

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: Map[GridBounds, K]): Vector[(K, Tile)] = {
        tiff.tile match {
          case gtTile: GeoTiffTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val fileSinglebandCOGCollectionReader: FileCOGCollectionReader[Tile] =
    new FileCOGCollectionReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val tiff = GeoTiffReader.readSingleband(uri.getPath, false, true)

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: Map[GridBounds, K]): Vector[(K, Tile)] = {
        tiff.tile match {
          case gtTile: GeoTiffTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val fileMultibandCOGRDDReader: FileCOGRDDReader[MultibandTile] =
    new FileCOGRDDReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        implicitly[MultibandTile => TileMergeMethods[MultibandTile]]

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        implicitly[MultibandTile => TilePrototypeMethods[MultibandTile]]

      def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
        val tiff = GeoTiffReader.readMultiband(uri.getPath, false, true)

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: Map[GridBounds, K]): Vector[(K, MultibandTile)] = {
        tiff.tile match {
          case gtTile: GeoTiffMultibandTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffMultibandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val fileMultibandCOGCollectionReader: FileCOGCollectionReader[MultibandTile] =
    new FileCOGCollectionReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        implicitly[MultibandTile => TileMergeMethods[MultibandTile]]

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        implicitly[MultibandTile => TilePrototypeMethods[MultibandTile]]

      def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
        val tiff = GeoTiffReader.readMultiband(uri.getPath, false, true)

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: Map[GridBounds, K]): Vector[(K, MultibandTile)] = {
        tiff.tile match {
          case gtTile: GeoTiffMultibandTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffMultibandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }
}
