package geotrellis.vector.io.shape

import java.nio.ByteBuffer

package object reader {
  implicit class ByteBufferExtensions(val byteBuffer: ByteBuffer) extends AnyVal {
    def moveForward(steps: Int): Unit =  byteBuffer.position(byteBuffer.position + steps)
  }
}
