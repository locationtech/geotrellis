package trellis.rest

//import java.io.{File,FileInputStream}
//import java.nio.MappedByteBuffer
//import java.nio.channels.FileChannel.MapMode._

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

//import scala.collection.mutable.ArrayBuffer
//import scala.collection.mutable.Map
import scala.math._

import trellis._
import trellis.operation._
import trellis.process._


/**
 * Operation to perform the basic weighted overlay calculation.
 */
object WeightedOverlay2 {
  def apply(raster1:Op[IntRaster], weight1:Op[Int],
            raster2:Op[IntRaster], weight2:Op[Int]) = {

    val x:Op[IntRaster] = MultiplyConstant(raster1, weight1)
    val y:Op[IntRaster] = MultiplyConstant(raster2, weight2)
    val z:Op[IntRaster] = Add(x, y)

    val weightSum:Op[Int] = Map2(weight1, weight2)(_ + _)

    DivideConstant(z, weightSum)
  }
}

object WeightedOverlayArray {
  def apply(rasters:Op[Array[IntRaster]], weights:Op[Array[Int]]) = {

    val rs:Op[Array[IntRaster]] = ForEach2(rasters, weights)(MultiplyConstant(_, _))

    val weightsum:Op[Int] = Map1(weights)(_.sum)

    DivideConstant(AddArray(rs), weightsum)
  }
}


///**
// * Operation to optionally normalize a raster if given a min/max.
// */
//case class OptionalNormalize(r:Operation[IntRaster],
//                             v:Operation[Option[Array[Int]]])
//extends SimpleOperation[IntRaster] {
//
//  def childOperations = List(r, v)
//
//  def _value(server:Server) = server.run(v) match {
//    case Some(Array(vmin, vmax)) => server.run(Normalize2(r, vmin, vmax, 1, 100))
//    case _ => server.run(Normalize(r, 1, 100))
//  }
//}
//
//
///**
// * Some basic helper functions.
// */
//object Util {
//  val runtime = Runtime.getRuntime()
//
//  def convertPng(path1:String, path2:String) {
//    val args = Array("convert", path1, "-depth", "8", path2)
//    val proc = runtime.exec(args)
//    val result = proc.waitFor()
//    println("%s exited: %s".format(args(0), result))
//  }
//
//  def deletePath(path:String) {
//    val f = new File(path)
//    f.delete()
//  }
//
//  def readByteArrayFromPath(path:String) = {
//    // create the memory-mapped buffer
//    val f       = new File(path)
//    val fis     = new FileInputStream(f)
//    val size    = f.length.toInt
//    val channel = fis.getChannel
//    val buffer  = channel.map(READ_ONLY, 0, size)
//    fis.close
//
//    // read 256K at a time out of the buffer into our array
//    var i = 0
//    val data = Array.ofDim[Byte](size)
//    while(buffer.hasRemaining()) {
//      val n = min(buffer.remaining(), 262144)
//      buffer.get(data, i, n)
//      i += n
//    }
//
//    data
//  }
//
//  def createTempName() = {
//    val f = java.io.File.createTempFile("trellis", ".tmp")
//    val path = f.getAbsolutePath()
//    f.delete()
//    path
//  }
//}


/**
 * Demo web service.
 */
@Path("/demo")
class WeightedOverlayService {

  @GET
  def weightedOverlay(
    // the type of request: raster/histogram/classes
    @DefaultValue("raster")
    @QueryParam("request")
    foo:String,

    // the requested output format
    @DefaultValue("debug")
    @QueryParam("format")
    bar:String,

    @Context req:HttpServletRequest
  ): Any = {
    val data = "hello world"
    Response.ok(data).`type`("text/plain").build()
  }
}

//
//    // factor1,weight1|factor2,weight2|...
//    @DefaultValue("")
//    @QueryParam("df")
//    df:String,
//
//    // xmin,ymin,xmax,ymax
//    @DefaultValue("-8479445.7,4808260.4,-8310842.8,4964022.2")
//    @QueryParam("bbox")
//    bbox:String,
//
//    // height to resample to
//    @DefaultValue("256")
//    @QueryParam("height")
//    height:Int,
//
//    // width to resample to
//    @DefaultValue("256") 
//    @QueryParam("width")
//    width:Int,
//
//    // factor to use as mask
//    @DefaultValue("")
//    @QueryParam("mask")
//    mask:String,
//
//    // value to use with mask factor
//    @DefaultValue("0")
//    @QueryParam("mask_value")
//    maskValue:Int,
//
//    // value to use with mask factor
//    @DefaultValue("false")
//    @QueryParam("inverse_mask")
//    inverseMask:Boolean,
//
//    // RGB color to use for no data 
//    @DefaultValue("000000")
//    @QueryParam("bgcolor")
//    bgcolorString:String,
//
//    // whether to use transparent background
//    @DefaultValue("true")
//    @QueryParam("transparent")
//    transparent:Boolean,
//
//    // type of breaks to use (quantile|linear|natural)
//    @DefaultValue("quantile")
//    @QueryParam("class_type")
//    classType:String,
//
//    // rgb1,rgb2,...
//    @DefaultValue("")
//    @QueryParam("colors")
//    colorsCsv:String,
//
//    // the number of classes to generate, when colors aren't given
//    @DefaultValue("-1")
//    @QueryParam("num_classes")
//    numClasses:Int,
//
//    // these are the specified linear breaks, or the percents for quantile
//    // breaks
//    @DefaultValue("")
//    @QueryParam("class_breaks")
//    classBreaks:String,
//
//    @DefaultValue("")
//    @QueryParam("value_range")
//    valueRange:String,
//
//    @DefaultValue("")
//    @QueryParam("render_params")
//    renderParams:String,
//
//    @DefaultValue("false")
//    @QueryParam("png_hack")
//    pngHack:Boolean,
//
//    @Context req:HttpServletRequest
//  ):Any = {
//    val t0 = System.currentTimeMillis()
//
//    //println(req.getRequestURL() + "?" + req.getQueryString())
//
//    // ==== PARSING THE USER PARAMETERS ====
//    if(bbox == "") {
//      throw new Exception("No bbox given!")
//    }
//
//    val Array(xmin, ymin, xmax, ymax) = parseBox(bbox).get
//
//    if (xmin >= xmax) {
//      throw new Exception("X range is invalid (%s,%s)".format(xmin, xmax))
//    }
//
//    if (ymin >= ymax) {
//      throw new Exception("Y range is invalid (%s,%s)".format(ymin, ymax))
//    }
//
//    // create tuples associating factors with weights
//    val factors = parseGroups(df).map {
//      case Array(name, w) => ("/var/trellis/" + name, w.toInt)
//      case q => throw new Exception("couldn't parse %s".format(q.toList))
//    } 
//
//    // pre-process the total weights
//    val weightsum = factors.foldLeft(0)((a, b) => (a + abs(b._2)).toInt)
//
//    // these are either values (linear breaks) or percentages (quantile)
//    var classes = parseInts(classBreaks)
//
//    // this is the min and max to use for normalization
//    var valueMinMax = parseInts(valueRange)
//
//    // render params will override classes and the min/max values
//    if (renderParams != "") {
//      val Array(s1, s2) = renderParams.split('|')
//      classes     = parseInts(s1)
//      valueMinMax = parseInts(s2)
//    }
//
//    var numBreaks = numClasses
//
//    if (numClasses > 0 && classes.isDefined) {
//      numBreaks = classes.get.length
//    } else if(classes.isDefined) {
//      numBreaks = classes.get.length
//    } else if(numClasses > 0) {
//      val d = 1.0 / numClasses
//      classes = Some((1 to numClasses).map { x => floor(x * d).toInt }.toArray)
//    } else {
//      throw new Exception("ERROR: saw neither num_classes nor class_breaks")
//    }
//
//    // parse "colors" for class breaks
//    val colors:Seq[Int] = if (colorsCsv == "") {
//      val chooser = new LinearColorRangeChooser(0x0000FF, 0xFF0000)
//      chooser.getColors(numBreaks)
//      //Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)
//    } else { 
//      traverse(colorsCsv.split(',').map(parseColor(_))).get
//    }
//
//    // color to use for no data cells
//    val bgcolor = parseColor(bgcolorString).getOrElse(0)
//
//    // ==== SETTING UP THE OPERATION ====
//    val server = Service.server
//
//    // load the files, and do the basic constant multiplication for weighting
//    val G = BuildRasterExtent(xmin = xmin, ymin = ymin,
//                          xmax = xmax, ymax = ymax,
//                          cols = width, rows = height,
//                          projection = 102113)
//
//    val paths = factors.map { case (path, weight) => path }
//    val weights = factors.map { case (path, weight) => weight }
//
//    val Q = WeightedOverlay(G, paths, weights)
//    val I = OptionalNormalize(Q, valueMinMax)
//
//    // 0-100 means 101 buckets
//    val H = BuildArrayHistogram(I, 101)
//
//    // ==== RUNNING THE OPERATION ====
//
//    // calculate the class breaks
//    var timing:String = ""
//
//    val breaks:Array[Int] = if(classType == "linear" || renderParams != "") {
//      classes.get
//    
//    } else if (classType == "quantile") {
//      val C = FindClassBreaks(H, numBreaks)
//      val result = server.run(C)
//      timing = C.genTimingTree
//      printf(timing)
//      result
//
//    } else if(classType == "natural") {
//      throw new Exception("unimplemented")
//
//    } else {
//      throw new Exception("unknown classType '%s'".format(classType))
//    }
//
//    val t1 = System.currentTimeMillis();
//
//    val response = (request, format) match {
//      // histograms
//      case ("histogram", "json") => {
//        val h = server.run(H)
//        val data = h.toJSON
//        Response.ok(data).`type`("text/plain").build()
//      }
//      case ("histogram", _) => {
//        val h = server.run(H)
//        val data = h.toString
//        Response.ok(data).`type`("text/plain").build()
//      }
//
//      // rparams
//      case ("rparams", "json") => {
//        val (vmin, vmax) = server.run(FindMinMax(Q))
//        val bdata = breaks.map { _.toString }.reduceLeft(_ + "," + _)
//        val fmt   = "{\"class_breaks\":[%s],\"min\":%d,\"max\":%d}"
//        val data  = fmt.format(bdata, vmin, vmax)
//        Response.ok(data).`type`("text/plain").build()
//      }
//      case ("rparams", _) => {
//        val (minval, maxval) = server.run(FindMinMax(Q))
//        val bdata = breaks.map { _.toString }.reduceLeft(_ + "," + _)
//        val ndata = minval + "," + maxval
//        val data  = bdata + "|" + ndata
//        Response.ok(data).`type`("text/plain").build()
//      }
//
//      // classes
//      case ("class", "json") => {
//        val data = "[" + breaks.map { _.toString }.reduceLeft(_ + "," + _) + "]"
//        Response.ok(data).`type`("text/plain").build()
//      }
//      case ("class", _) => {
//        val data = breaks.map { _.toString }.reduceLeft(_ + "," + _)
//        Response.ok(data).`type`("text/plain").build()
//      }
//
//      // raster
//      case ("raster", "debug") => {
//        val colorBreaks = breaks.zip(colors).toArray
//
//        val P = WritePNGFile(I, "/var/www/trellis/output.png", colorBreaks,
//                             bgcolor, transparent)
//
//        val pngStartTime = System.currentTimeMillis();
//        server.run(P)
//        val t2 = System.currentTimeMillis()
//
//        //printf("png time: %d ms\n", t1 - t1)
//        val output = """
//          <html>
//          <head>
//          <body>
//          <h1>TRELLIS!</h1>
//          <h2>%dx%d raster finished in: %d ms</h2>
//          <h2>calc: %d ms / render: %d ms</h2>
//          <img src=\"http://%s/trellis/output.png\" />
//          <pre>%s</pre>
//          </body>
//          </head>
//          </html>""".format(width, height, t2 - t0, t1 - t0, t2 - t1,
//                            req.getServerName(), timing)
//        Response.ok(output).`type`("text/html").build()
//      }
//
//      case ("raster", "kml") => {
//        val (north, east) = TileUtils.MetersToLatLon(xmax, ymax)
//        val (south, west) = TileUtils.MetersToLatLon(xmin, ymin)
//
//        val dy = north - south
//        val dx = east - west
//
//        val height = 1000
//        val width = ((height * dx) / dy).toInt
//
//        val df2 = factors.map {
//          case (path, w) => "%s-latlon.arg,%s".format(path.replace("/var/trellis/", ""), w)
//        }.reduceLeft(_ + "|" + _)
//
//        val bbox2 = "%s,%s,%s,%s".format(west, south, east, north)
//        val url2 = req.getRequestURL() + "?df=%s&amp;bbox=%s&amp;height=%s&amp;width=%s&amp;colors=%s&amp;render_params=%s&amp;png_hack=true&amp;format=png".format(df2, bbox2, height, width, colorsCsv, renderParams)
//
//        val output = """<?xml version="1.0" encoding="UTF-8"?>
//<kml xmlns="http://www.opengis.net/kml/2.2">
//  <Document>
//    <GroundOverlay>
//      <name>Decision Tree: Weighted Overlay</name>
//      <Icon>
//        <href>%s</href>
//      </Icon>
//      <LatLonBox>
//        <north>%s</north>
//        <south>%s</south>
//        <east>%s</east>
//        <west>%s</west>
//      </LatLonBox>
//    </GroundOverlay>
//  </Document>
//</kml>""".format(url2, north, south, east, west) // " ugh
//        val mime = "application/vnd.google-earth.kml+xml"
//
//        // content-disposition doesn't seem to work here :(
//        Response.ok(output).`type`(mime).header("Content-Disposition", "attachment; filename=overlay.kml").build()
//      }
//
//
//      // In this situation the client wants us to run our calculation for a list
//      // of points. We need to find the correct cell in the raster for each point
//      // and run our calculation on just that point.
//      case ("raster", "projects") => {
//
//        val t0 = System.currentTimeMillis()
//
//        val colorBreaks = breaks.zip(colors).toArray
//
//        def findColor(z:Int): Int = {
//          if (z == NODATA) return 0
//          colorBreaks.foreach {
//            tpl => if (z <= tpl._1) return tpl._2
//          }
//          return colorBreaks(colorBreaks.length - 1)._2
//        }
//
//        val sql = "select eroc, cwis, the_geom as the_geom from performance_points"
//        val reader = Service.buildReader()
//        val stmt = PgUtil.prepare(reader.conn, sql, Array())
//        val r = stmt.executeQuery()
//
//        var found = 0
//        var missing = 0
//
//        var starting = true
//        val b = new StringBuilder("[")
//
//        while (r.next()) {
//          val m = PgTypes.resultsAsMap(r)
//          val eroc = m.getOrElse("eroc", "")
//          val cwis = m.getOrElse("cwis", "")
//
//          if (starting) { starting = false } else { b.append(",\n") }
//
//          m.get("the_geom") match {
//            case Some(p:JtsPoint) => {
//              val x = p.getX
//              val y = p.getY
//
//              val g = BuildRasterExtent(xmin = x - 3500.0, ymin = y - 3500.0,
//                                    xmax = x + 3500.0, ymax = y + 3500.0,
//                                    cols = 7, rows = 7,
//                                    projection = 102113)
//              
//              val w = OptionalNormalize(WeightedOverlay(g, paths, weights), valueMinMax)
//              val raster = server.run(w)
//              val z = raster.data(24)
//              val hue = findColor(z)
//
//              if (z == NODATA) {
//                missing += 1
//              } else {
//                found += 1
//              }
//
//              b.append("{\"eroc\":\"%s\", \"cwis\":\"%s\", \"value\":%d, \"hue\":\"%06x\"}".format(eroc, cwis, z, hue))
//            }
//            case q => {
//              println(q)
//              throw new Exception("wtf")
//            }
//          }
//        }
//        b.append("]")
//        println("ran in %d ms".format(System.currentTimeMillis() - t0))
//        printf("found = %d / missing = %d\n", found, missing)
//
//        val json = b.toString
//        Response.ok(json).`type`("text/plain").build()
//      }
//
//
//      case ("raster", "png") => {
//        val colorBreaks = breaks.zip(colors).toArray
//
//        val data = if (pngHack) {
//          println("hack hack hack!")
//          val path1 = Util.createTempName()
//          val path2 = Util.createTempName()
//
//          val P = WritePNGFile(I, path1, colorBreaks, bgcolor, transparent)
//          server.run(P)
//          println("finished op...")
//
//          println("about to convert...")
//          Util.convertPng(path1, path2)
//          Util.deletePath(path1)
//
//          println("about to read back in...")
//          val data = Util.readByteArrayFromPath(path2)
//          Util.deletePath(path2)
//
//          data
//        } else {
//          server.run(RenderPNG(I, colorBreaks, bgcolor, transparent))
//        }
//
//        Response.ok(data).`type`("image/png").build()
//      }
//    }
//    response
//  }
//}
