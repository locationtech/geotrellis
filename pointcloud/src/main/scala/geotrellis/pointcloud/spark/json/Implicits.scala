/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.pointcloud.spark.json

import io.pdal.pipeline.ReaderTypes
import geotrellis.pointcloud.spark.{Extent3D, ProjectedExtent3D}
import geotrellis.proj4.CRS
import geotrellis.util.EitherMethods

import io.circe.Decoder

object Implicits extends Implicits

trait Implicits {
  implicit val extent3DDecoder: Decoder[Extent3D] = Decoder.instance { cursor =>
    val md = cursor.downField("metadata")
    val driver =
      ReaderTypes
        .all.flatMap(s => md.downField(s.toString).focus)
        .headOption
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.fields.getOrElse(Nil)}"))

    EitherMethods.sequence(
      driver.downField("minx").as[Double] ::
      driver.downField("miny").as[Double] ::
      driver.downField("minz").as[Double] ::
      driver.downField("maxx").as[Double] ::
      driver.downField("maxy").as[Double] ::
      driver.downField("maxz").as[Double] :: Nil
    ).right.map { case List(xmin, ymin, zmin, xmax, ymax, zmax) =>
      Extent3D(xmin, ymin, zmin, xmax, ymax, zmax)
    }
  }

  implicit val projectedExtent3DDecoder: Decoder[ProjectedExtent3D] = Decoder.instance { cursor =>
    val md = cursor.downField("metadata")
    val driver =
      ReaderTypes
        .all.flatMap(s => md.downField(s.toString).focus)
        .headOption
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.fields.getOrElse(Nil)}"))

    val crs =
      CRS.fromString(driver.downField("srs").downField("proj4").as[String] match {
        case Right(s) => s
        case Left(e) => throw new Exception("Incorrect CRS metadata information, try to provide the input CRS").initCause(e)
      })

    cursor.value.as[Extent3D].right.map(ProjectedExtent3D(_, crs))
  }
}
