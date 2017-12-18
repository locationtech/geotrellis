package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark.io.s3.S3Client

import java.net.URI

import com.amazonaws.services.s3.AmazonS3URI
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.StreamingByteReader

trait Implicits extends Serializable {
  implicit val s3SinglebandCOGRDDReader: S3COGRDDReader[Tile] =
    new S3COGRDDReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      def getS3Client: () => S3Client = () => S3Client.DEFAULT

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val tiff = GeoTiffReader.readSingleband(reader, false, true)

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
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val info =
          readGeoTiffInfo(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = None
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val s3SinglebandCOGCollectionReader: S3COGCollectionReader[Tile] =
    new S3COGCollectionReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      def getS3Client: () => S3Client = () => S3Client.DEFAULT

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val tiff = GeoTiffReader.readSingleband(reader, false, true)

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
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val info =
          readGeoTiffInfo(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = None
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val s3MultibandCOGRDDReader: S3COGRDDReader[MultibandTile] =
    new S3COGRDDReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        implicitly[MultibandTile => TileMergeMethods[MultibandTile]]

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        implicitly[MultibandTile => TilePrototypeMethods[MultibandTile]]

      def getS3Client: () => S3Client = () => S3Client.DEFAULT

      def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val tiff = GeoTiffReader.readMultiband(reader, false, true)

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
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val info =
          readGeoTiffInfo(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = None
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffMultibandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }

  implicit val s3MultibandCOGCollectionReader: S3COGCollectionReader[MultibandTile] =
    new S3COGCollectionReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        implicitly[MultibandTile => TileMergeMethods[MultibandTile]]

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        implicitly[MultibandTile => TilePrototypeMethods[MultibandTile]]

      def getS3Client: () => S3Client = () => S3Client.DEFAULT

      def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val tiff = GeoTiffReader.readMultiband(reader, false, true)

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
        val auri = new AmazonS3URI(uri)

        val reader = StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key    = auri.getKey,
            client = getS3Client()
          )
        )

        val info =
          readGeoTiffInfo(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = None
          )

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
