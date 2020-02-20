package geotrellis.raster

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class CellTypeEncodingBench {

  @Param(Array("int16ud44", "uint8raw", "float32"))
  var cellTypeName: String = _
  var cellType: CellType = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    cellType = CellType.fromName(cellTypeName)
  }

  @Benchmark
  def asString(): String = {
    cellType.toString
  }

  @Benchmark
  def fromString(): CellType = {
    CellType.fromName(cellTypeName)
  }
}
