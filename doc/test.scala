// run this as:
// scala -cp ~/trellis/target/scala_2.8.0/classes -Xscript Script test.scala


import trellis.data.png.PNGWriter
//import trellis.core.raster

import java.io.File
import java.nio.ByteBuffer

val height = 600
val width  = 600

val f  = new File("something.png")
val bb = ByteBuffer.allocate(height * width * 3)

for(i <- 0 until height * width) {
  bb.put(i * 3,     0xFF.toByte)
  bb.put(i * 3 + 1, 0x00.toByte)
  bb.put(i * 3 + 2, 0x00.toByte)
}

val t0 = System.currentTimeMillis()
PNGWriter.write(f, bb, height, width)
val t1 = System.currentTimeMillis()
Console.printf("took %d ms\n", t1 - t0)
