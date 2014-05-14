package geotrellis.gdal

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.gdal.gdal.Band
import org.gdal.gdal.ColorTable
import org.gdal.gdal.Dataset
import org.gdal.gdal.Driver
import org.gdal.gdal.GCP
import org.gdal.gdal.gdal
import org.gdal.gdal.TermProgressCallback
import org.gdal.gdal.RasterAttributeTable
import org.gdal.gdalconst.gdalconstConstants
import org.gdal.osr.CoordinateTransformation
import org.gdal.osr.SpatialReference

import org.gdal.ogr.Geometry

import scala.collection.JavaConversions._

object GdalInfo {
  def main(args:Array[String]): Unit =
    GdalInfoOptions.parse(args) match {
      case Some(options) =>
        apply(options)
      case None =>
        // Argument errored, should have printed usage.
    }

  def apply(options: GdalInfoOptions): Unit = {
    gdal.AllRegister()

    val raster = Gdal.open(options.file.getAbsolutePath)

    val driver = raster.driver

    println(s"Driver: ${driver.getShortName}/${driver.getLongName}")

    val fileList = raster.files
    if(fileList.size == 0) {
      println("Files: none associated")
    } else {
      println("Files:")
      for(f <- fileList) { println(s"       $f") }
    }

    println(s"Size is ${raster.cols}, ${raster.rows}")

    raster.projection match {
      case Some(projection) =>
        val srs = new SpatialReference(projection)
        if(srs != null && projection.length != 0) {
          val arr = Array.ofDim[String](1)
          srs.ExportToPrettyWkt(arr)
          println("Coordinate System is:")
          println(arr(0))
        } else {
          println(s"Coordinate Sytem is ${projection}")
        }
        if(srs != null) { srs.delete() }
      case None =>
    }

    val geoTransform = raster.geoTransform

    if (geoTransform(2) == 0.0 && geoTransform(4) == 0.0) {
      println(s"Origin = (${geoTransform(0)},${geoTransform(3)})")
      println(s"Pixel Size = (${geoTransform(1)},${geoTransform(5)})")
    } else {
      println("GeoTransform =")
      println(s"  ${geoTransform(0)}, ${geoTransform(1)}, ${geoTransform(2)}")
      println(s"  ${geoTransform(3)}, ${geoTransform(4)}, ${geoTransform(5)}")
    }

    if(options.showGcps && raster.groundControlPointCount > 0) {
      for((gcp, i) <- raster.groundControlPoints.zipWithIndex) {
        println(s"GCP[$i]: Id=${gcp.id}, Info=${gcp.info}")
        println(s"    (${gcp.col},${gcp.row}) (${gcp.x},${gcp.y},${gcp.z})")
      }
    }

    if(options.showMetadata) {
      def printMetadata(header: String, id: String = "") = {
        val md = raster.metadata(id)
        if(!md.isEmpty) {
          println(header)
          for(key <- md) {
            println(s"  $key")
          }
        }
      }

      val metadataPairs = List(
        ("Image Structure Metadata:", "IMAGE_STRUCTURE"),
        ("Subdatasets:", "SUBDATASETS"),
        ("Geolocation:", "GEOLOCATION"),
        ("RPC Metadata:", "RPC")
      )

      printMetadata("Metadata:")

      for(domain <- options.mdds) {
        printMetadata("Metadata ($domain):", domain)
      }

      for((header, id) <- metadataPairs) {
        printMetadata(header, id)
      }
    }

    println("Corner Coordinates:")
    reportCorner(raster, "Upper Left ", 0.0, 0.0)
    reportCorner(raster, "Lower Left ", 0.0, raster.rows)
    reportCorner(raster, "Upper Right", raster.cols, 0.0)
    reportCorner(raster, "Lower Right", raster.cols, raster.rows)
    reportCorner(raster, "Center     ", raster.cols / 2.0, raster.rows / 2.0)

    for ((band, i) <- raster.bands.zipWithIndex) {
      print(s"Band ${i+1} Block=${band.blockWidth}x${band.blockHeight} ")
      println(s"Type=${band.rasterType}, ColorInterp=${band.colorName}")
      band.description match {
        case Some(description) => println(s"  Description = $description")
        case None =>
      }

      // TODO: min/max
      // TODO: minimum and mean
      // TODO: bucket count
      // TODO: checksum

      band.noDataValue match {
        case Some(noDataValue) => println(s"  NoData Value=$noDataValue")
        case None =>
      }

      // TODO: overviews
      // TODO: mask flags
      // TODO: mask band overviews
      // TODO: unit type

      if (band.categories.length > 0) {
        println( "  Categories:" )
        for ((category, i) <- band.categories.zipWithIndex) {
          println(s"      $i: $category")
        }
      }

      // TODO: offset
      // TODO: scale
      // TODO: metadata

      if (band.colorName == "Palette") {
        band.colorTable match {
          case Some((colorTable, name)) =>
            println(s"  Color Table ($name with ${colorTable.size} entries)")
            if (options.showColorTable) {
              for ((color, i) <- colorTable.zipWithIndex) {
                println(s"    $i: $color");
              }
            }
          case None =>
        }
      }

      // TODO: RAT
    }
  }

  def reportCorner(raster: RasterDataSet, cornerName: String, x: Double, y: Double) = {
    print(cornerName + " ")
    if (raster.geoTransform.filter(_ != 0).length == 0) {
      println(s"($x,$y)")
    } else {
      val gt = raster.geoTransform
      val geox = gt(0) + gt(1) * x + gt(2) * y
      val geoy = gt(3) + gt(4) * x + gt(5) * y
      print(f"($geox%12.3f,$geoy%12.3f) ")

      raster.projection match {
        case Some(projection) =>
          val srs = new SpatialReference(projection)
          if (srs != null && projection.length != 0) {
            val latLong = srs.CloneGeogCS()
            if (latLong != null) {
              val transform = CoordinateTransformation
                .CreateCoordinateTransformation(srs, latLong)
              if (transform != null) {
		val point = transform.TransformPoint(geox, geoy, 0)
                val xtrans = gdal.DecToDMS(point(0), "Long", 2)
                val ytrans = gdal.DecToDMS(point(1), "Lat", 2)
		println(s"($xtrans,$ytrans)");
		transform.delete();
	      }
            }
          }
          if (srs != null) { srs.delete() }
        case None => println()
      }
    }
  }
}
