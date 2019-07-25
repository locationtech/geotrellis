/*
 * Copyright 2019 Azavea & Astraea, Inc.
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
 *
 *
 * This file is a bit modified version of https://github.com/locationtech/rasterframes/blob/dc561dc9ad49eca043a6a0465f11d18d09566db1/bench/src/main/scala/org/locationtech/rasterframes/bench/CRSBench.scala
 */

package geotrellis.proj4

import java.util.concurrent.TimeUnit

import org.locationtech.proj4j.CoordinateReferenceSystem
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class CRSBench {

  var crs1: CRS = _
  var crs2: CRS = _

  /**
    * jmh:run -i 3 -wi 3 -f1 -t1 .*CRSBench.*
    *
    * LazyCRS (rasterframes fix):
    * [info] Benchmark                        Mode  Cnt   Score    Error  Units
    * [info] CRSBench.logicalEqualsFalse      avgt    3  13.534 ±  6.396  us/op
    * [info] CRSBench.logicalEqualsTrue       avgt    3   0.254 ±  0.116  us/op
    * [info] CRSBench.logicalLazyEqualsFalse  avgt    3  14.505 ± 10.106  us/op
    * [info] CRSBench.logicalLazyEqualsTrue   avgt    3   0.035 ±  0.007  us/op
    * [info] CRSBench.resolveCRS              avgt    3   0.069 ±  0.082  us/op
    *
    * Proj4 1.0.0:
    * [info] Benchmark                        Mode  Cnt   Score     Error  Units
    * [info] CRSBench.logicalEqualsFalse      avgt    3  23.397 ±  37.290  us/op
    * [info] CRSBench.logicalEqualsTrue       avgt    3  11.639 ±  49.851  us/op
    * [info] CRSBench.logicalLazyEqualsFalse  avgt    3  27.671 ± 106.851  us/op
    * [info] CRSBench.logicalLazyEqualsTrue   avgt    3  12.928 ±  28.639  us/op
    * [info] CRSBench.resolveCRS              avgt    3   0.233 ±   0.844  us/op
    *
    * GeoTools:
    * [info] Benchmark                        Mode  Cnt  Score   Error  Units
    * [info] CRSBench.logicalEqualsFalse      avgt    3  0.046 ± 0.068  us/op
    * [info] CRSBench.logicalEqualsTrue       avgt    3  0.042 ± 0.043  us/op
    * [info] CRSBench.logicalLazyEqualsFalse  avgt    3  0.047 ± 0.052  us/op
    * [info] CRSBench.logicalLazyEqualsTrue   avgt    3  0.042 ± 0.028  us/op
    * [info] CRSBench.resolveCRS              avgt    3  0.041 ± 0.127  us/op
    *
    * Proj4 1.0.1 (backed by Caffeine, unbounded):
    * [info] Benchmark                       Mode  Cnt  Score   Error  Units
    * [info] CRSBench.epsgCodeReadTest       avgt    3  0.062 ± 0.032  us/op
    * [info] CRSBench.logicalEqualsFalse     avgt    3  0.037 ± 0.009  us/op
    * [info] CRSBench.logicalEqualsTrue      avgt    3  0.054 ± 0.013  us/op
    * [info] CRSBench.logicalVarEqualsFalse  avgt    3  0.038 ± 0.015  us/op
    * [info] CRSBench.logicalVarEqualsTrue   avgt    3  0.053 ± 0.022  us/op
    * [info] CRSBench.resolveCRS             avgt    3  0.035 ± 0.025  us/op
    *
    * Proj4 1.0.1 (backed by ConcurrentHashMap):
    * [info] Benchmark                       Mode  Cnt  Score   Error  Units
    * [info] CRSBench.getEpsgCodeTest        avgt    3  0.064 ± 0.161  us/op
    * [info] CRSBench.logicalEqualsFalse     avgt    3  0.039 ± 0.009  us/op
    * [info] CRSBench.logicalEqualsTrue      avgt    3  0.035 ± 0.020  us/op
    * [info] CRSBench.logicalVarEqualsFalse  avgt    3  0.040 ± 0.068  us/op
    * [info] CRSBench.logicalVarEqualsTrue   avgt    3  0.037 ± 0.034  us/op
    * [info] CRSBench.resolveCRS             avgt    3  0.034 ± 0.008  us/op
    */

  @Setup(Level.Invocation)
  def setupData(): Unit = {
    crs1 = CRS.fromEpsgCode(4326)
    crs2 = WebMercator
  }

  @Benchmark
  def resolveCRS(): CoordinateReferenceSystem = {
    crs1.proj4jCrs
  }

  @Benchmark
  def logicalEqualsTrue(): Boolean = {
    crs1 == LatLng
  }

  @Benchmark
  def logicalEqualsFalse(): Boolean = {
    crs1 == WebMercator
  }

  @Benchmark
  def logicalVarEqualsTrue(): Boolean = {
    crs1 == crs1
  }

  @Benchmark
  def logicalVarEqualsFalse(): Boolean = {
    crs1 == crs2
  }

  @Benchmark
  def getEpsgCodeTest(): Boolean = {
    CRS.getEpsgCode("+proj=longlat +ellps=intl +towgs84=84,274,65,0,0,0,0 +no_defs").get == 4630
  }
}
