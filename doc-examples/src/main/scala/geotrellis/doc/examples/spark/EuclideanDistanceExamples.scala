package geotrellis.doc.examples.distance

object EuclideanDistanceExamples {
  def `Demonstration of shortcomings of dense EuclideanDistance operation`: Unit = {
    import org.locationtech.jts.geom.Coordinate
    import org.apache.spark.SparkContext
    import org.apache.spark.rdd.RDD

    import geotrellis.proj4._
    import geotrellis.raster._
    import geotrellis.raster.render._
    import geotrellis.spark._
    import geotrellis.spark.tiling._
    import geotrellis.vector._

    // This examples show some problems that may arise when using the distribued 
    // Euclidean distance operations on data that does not sufficiently cover 
    // the extent in question.  Run this test and look at the resulting schools.
    // png; you'll notice the lower left hand corner has areas that are empty 
    // and other areas showing discontinuous behavior because the points which 
    // define the Euclidean distance are too far away.

    val sc: SparkContext = ???

    val geomWKT = scala.io.Source.fromFile("geotrellis/spark/src/test/resources/wkt/schools.wkt").getLines.mkString
    val LayoutLevel(z, ld) = ZoomedLayoutScheme(WebMercator).levelForZoom(12)
    val maptrans = ld.mapTransform

    val geom = geotrellis.vector.io.wkt.WKT.read(geomWKT).asInstanceOf[MultiPoint]
    val GridBounds(cmin, rmin, cmax, rmax) = maptrans(geom.envelope)

    val skRDD = sc.parallelize(for (r <- rmin to rmax; c <- cmin to cmax) yield SpatialKey(c, r))

    def createPoints(sk: SpatialKey): (SpatialKey, Array[Coordinate]) = {
      val ex = maptrans(sk)
      val coords = geom.points.filter(ex.contains(_)).map(_.jtsGeom.getCoordinate)
      println(s"$sk has ${coords.size} points")
      (sk, coords)
    }

    val inputRDD = skRDD.map(createPoints)

    val tileRDD: RDD[(SpatialKey, Tile)] = inputRDD.euclideanDistance(ld)

    val maxDistance = tileRDD.map(_._2.findMinMaxDouble).collect.foldLeft(-1.0/0.0){ (max, minMax) => scala.math.max(max, minMax._2) }
    val cm = ColorMap((0.0 to maxDistance by (maxDistance/512)).toArray, ColorRamps.BlueToRed)
    tileRDD.stitch().renderPng(cm).write("schools.png")
  }

}
