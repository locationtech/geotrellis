package geotrellis.service

object Layer {
  // def asJson(layer:String):String =
  //   asJson(None,layer)

  // def asJson(dataStore:String,layer:String):String =
  //   asJson(Some(dataStore),layer)

  // def asJson(layer:String):String = {
  //   RasterSource(layer:String)
  //   val op = io.LoadRasterLayerInfo(layer);
  //   GeoTrellis.run(op) match {
  //     case process.Complete(info,h) =>
  //       OK.json(s"""{
  //         "name" : "${layer}",
  //         "rasterExtent" : ${info.rasterExtent.toJson},
  //         "datatype" :" ${info.rasterType}"
  //        }""")
  //          .allowCORS()
  //     case process.Error(message,failure) =>
  //       ERROR(message,failure)
  //   }
  // }

  // @GET
  // @Path("/render")
  // def render(
  //   @DefaultValue("") @QueryParam("bbox") bbox:String,
  //   @DefaultValue("256") @QueryParam("cols") cols:Int,
  //   @DefaultValue("256") @QueryParam("rows") rows:Int,
  //   @DefaultValue("") @QueryParam("layer") layer:String,
  //   @DefaultValue("") @QueryParam("palette") palette:String,
  //   @DefaultValue("4") @QueryParam("colors") numColors:String,
  //   @DefaultValue("image/png") @QueryParam("format") format:String,
  //   @DefaultValue("") @QueryParam("breaks") breaks:String,
  //   @DefaultValue("blue-to-red") @QueryParam("colorRamp") colorRampKey:String,
  //   @Context req:HttpServletRequest
  // ):Response = {
  //   val extent = {
  //     val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
  //     Extent(xmin,ymin,xmax,ymax)
  //   }

  //   val rasterExtent = RasterExtent(extent, cols, rows)

  //   val layerOp = io.LoadRaster(layer,rasterExtent).map { r =>
  //     // Convert 0 of bit raster to NODATA
  //     if(r.rasterType == TypeBit) { 
  //       r.convert(TypeByte).map { z => if(z == 0) NODATA else z } 
  //     } else { 
  //       r 
  //     }
  //   }                                                 

  //   val breaksOp = 
  //     logic.ForEach(string.SplitOnComma(breaks))(string.ParseInt(_))
    
  //   val ramp = 
  //     breaksOp.map { b =>
  //       val cr = Colors.rampMap.getOrElse(colorRampKey,BlueToRed)
  //       if(cr.toArray.length < b.length) { cr.interpolate(b.length) }
  //       else { cr }
  //     }

  //   val png = Render(layerOp,ramp,breaksOp)

  //   GeoTrellis.run(png) match {
  //     case process.Complete(img,h) =>
  //       OK.png(img)
  //         .cache(1000)
  //     case process.Error(message,failure) =>
  //       ERROR(message,failure)
  //   }
  // }

  // @GET
  // @Path("/valuegrid")
  // def render(
  //   @DefaultValue("") @QueryParam("layer") layer:String,
  //   @DefaultValue("") @QueryParam("lat") lat:String,
  //   @DefaultValue("") @QueryParam("lng") lng:String,
  //   @DefaultValue("7") @QueryParam("size") size:String,
  //   @Context req:HttpServletRequest
  // ):Response = {
  //   val latOp = string.ParseDouble(lat)
  //   val lngOp = string.ParseDouble(lng)
  //   val sizeOp = string.ParseInt(size)

  //   val layerOp = io.LoadRaster(layer)

  //   val op = for(rast <- layerOp;
  //                lat  <- latOp;
  //                lng  <- lngOp;
  //                size <- sizeOp) yield {
  //     val (x,y) = srs.LatLng.transform(lng,lat,srs.WebMercator)
  //     val s = size / 2
  //     val (col,row) = rast.rasterExtent.mapToGrid(x,y)
  //     for(r <- (row - s) to (row + s);
  //         c <- (col - s) to (col + s)) yield {
  //       if(0 <= c && c <= rast.cols &&
  //          0 <= r && r <= rast.rows) {
  //         "\"%.2f\"".format(rast.getDouble(c,r))
  //       } else {
  //         "\"\""
  //       }
  //     }
  //   }
 
  //   GeoTrellis.run(op) match {
  //     case process.Complete(values,h) =>
  //       val data = s""" { "success" : "1", "values" : [ ${values.mkString(",")} ] } """
  //       OK.json(data)
  //     case process.Error(message,failure) =>
  //       ERROR(message,failure)
  //   }
  // }
}
