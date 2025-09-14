package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.{GeoTiffReader, GeoTiffTestUtils, MultibandGeoTiff}
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.raster.{FloatCellType, IntCellType, ShortCellType}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PredictorSpec extends AnyFunSpec with Matchers with GeoTiffTestUtils with RasterMatchers with BeforeAndAfterAll {

  override def afterAll(): Unit = purge

  describe("Write compressed tiff files with predictor") {
    it("8 bits, predictor 2, deflate compression") {
      val originalTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-uncompressed.tif"))
      val compression = DeflateCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "8bits_predictor2_deflated.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)
      tags.nonBasicTags.predictor should be(Some(2))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("8 bits, predictor 2, ZStd compression") {
      val originalTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-uncompressed.tif"))
      val compression = ZStdCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "8bits_predictor2_zstd.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZStdCoded)
      tags.nonBasicTags.predictor should be(Some(2))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("16 bits, predictor 2, deflate compression") {
      val sourceTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-uncompressed.tif"))
      val tile = sourceTiff.tile.convert(ShortCellType)
      val originalTiff = MultibandGeoTiff(tile, sourceTiff.extent, sourceTiff.crs, sourceTiff.options)
      val compression = DeflateCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "16bits_predictor2_deflated.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)
      tags.nonBasicTags.predictor should be(Some(2))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("32 bits, predictor 2, deflate compression") {
      val sourceTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-uncompressed.tif"))
      val tile = sourceTiff.tile.convert(IntCellType)
      val originalTiff = MultibandGeoTiff(tile, sourceTiff.extent, sourceTiff.crs, sourceTiff.options)
      val compression = DeflateCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_predictor2_deflated.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)
      tags.nonBasicTags.predictor should be(Some(2))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("32 bits, striped, predictor 3, deflate compression") {
      val originalTiff = GeoTiffReader.readMultiband(s"raster/data/slope.tif")
      val compression = DeflateCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_striped_predictor3_deflated.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)
      tags.nonBasicTags.predictor should be(Some(3))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile, 100)
    }

    it("64 bits, predictor 3, deflate compression") {
      val originalTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-gaussian-expected.tif"))
      val compression = DeflateCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_predictor2_deflated.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)
      tags.nonBasicTags.predictor should be(Some(3))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("64 bits, predictor 3, zstd compression") {
      val originalTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-gaussian-expected.tif"))
      val compression = ZStdCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_predictor2_zstd.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZStdCoded)
      tags.nonBasicTags.predictor should be(Some(3))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }

    it("32 bits, tiled, predictor 3, zstd compression") {
      val sourceTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-gaussian-expected.tif"))
      val tile = sourceTiff.tile.convert(FloatCellType)
      val originalTiff = MultibandGeoTiff(tile, sourceTiff.extent, sourceTiff.crs, sourceTiff.options)
      val compression = ZStdCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_tiled_predictor3_zstd.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZStdCoded)
      tags.nonBasicTags.predictor should be(Some(3))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }


    it("32 bits, tiled, predictor 2, zstd compression") {
      val sourceTiff = GeoTiffReader.readMultiband(geoTiffPath(s"jpeg-test-small-gaussian-expected.tif"))
      val tile = sourceTiff.tile.convert(IntCellType)
      val originalTiff = MultibandGeoTiff(tile, sourceTiff.extent, sourceTiff.crs, sourceTiff.options)
      val compression = ZStdCompression.withPredictor(Predictor(originalTiff.imageData))
      val tiff = MultibandGeoTiff(originalTiff.raster.tile.toArrayTile(), originalTiff.raster.extent, originalTiff.crs, originalTiff.options.copy(compression = compression))
      val path = "32bits_tiled_predictor2_zstd.tif"
      tiff.write(path)
      addToPurge(path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZStdCoded)
      tags.nonBasicTags.predictor should be(Some(2))

      val rewrittenTiff = GeoTiffReader.readMultiband(path)
      assertEqual(originalTiff.tile, rewrittenTiff.tile)
    }
  }
}
