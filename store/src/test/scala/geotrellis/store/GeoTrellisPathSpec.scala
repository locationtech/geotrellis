/*
 * Copyright 2020 Azavea
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

package geotrellis.store

import org.scalatest.funspec.AnyFunSpec

class GeoTrellisPathSpec extends AnyFunSpec {
  describe("GeoTrellisPathSpec") {

    it("should fail to parse without a layer") {
      val path = GeoTrellisPath.parseOption("file:///foo/bar?zoom=1")
      assert(path == None)
    }

    it("should fail to parse without a zoom") {
      val path = GeoTrellisPath.parseOption("file:///foo/bar?layer=baz")
      assert(path == None)
    }

    it("should parse a local absolute file path without scheme") {
      val path = GeoTrellisPath.parse("/absolute/path?layer=baz&zoom=1")
      assert(path.value == "file:///absolute/path")
      assert(path.layerName == "baz")
    }

    it("should parse a local absolute file path with scheme") {
      val path = GeoTrellisPath.parse("file:///absolute/path?layer=baz&zoom=1")
      assert(path.value == "file:///absolute/path")
      assert(path.layerName == "baz")
    }

    it("should parse a local relative file path with scheme") {
      val path = GeoTrellisPath.parse("file://relative/path?layer=baz&zoom=1")
      assert(path.value == "file://relative/path")
      assert(path.layerName == "baz")
    }

    it("should parse a local relative file path without scheme") {
      val path = GeoTrellisPath.parse("relative/path?layer=baz&zoom=1")
      assert(path.value == "file://relative/path")
      assert(path.layerName == "baz")
    }

    it("should parse params") {
      val path = GeoTrellisPath.parse("file:///foo/bar?layer=baz&band_count=1&zoom=10")
      assert(path.layerName == "baz")
      assert(path.zoomLevel == 10)
      assert(path.bandCount == Some(1))
    }

    it("should parse hdfs scheme") {
      val path = GeoTrellisPath.parse("hdfs://path/to?layer=foo&zoom=1")
      assert(path.value == "hdfs://path/to")
      assert(path.layerName == "foo")
    }

    it("should parse s3 scheme") {
      val path = GeoTrellisPath.parse("s3://bucket/path?layer=foo&zoom=1")
      assert(path.value == "s3://bucket/path")
      assert(path.layerName == "foo")
    }

    it("should parse absolute file scheme with gt+ prefix") {
      val path = GeoTrellisPath.parse("gt+file:///absolute/path?layer=foo&zoom=1")
      assert(path.value == "file:///absolute/path")
      assert(path.layerName == "foo")
    }

    it("should parse relative file scheme with gt+ prefix") {
      val path = GeoTrellisPath.parse("gt+file://relative/path?layer=foo&zoom=1")
      assert(path.value == "file://relative/path")
      assert(path.layerName == "foo")
    }

    it("should parse s3 scheme with gt+ prefix") {
      val path = GeoTrellisPath.parse("gt+s3://bucket/path?layer=foo&zoom=1")
      assert(path.value == "s3://bucket/path")
      assert(path.layerName == "foo")
    }

    it("should ignore invalid parameters") {
      val path = GeoTrellisPath.parse("file:///foo/bar?layer=baz&zoom=1&invalid=not&nope=1")
      assert(path == GeoTrellisPath("file:///foo/bar", "baz", 1, None))
    }
  }
}
