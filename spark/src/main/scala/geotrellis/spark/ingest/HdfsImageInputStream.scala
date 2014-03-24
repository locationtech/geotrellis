package geotrellis.spark.ingest

import javax.imageio.stream.ImageInputStreamImpl
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.spark.Logging
import scala.collection.mutable.Stack

class HdfsImageInputStream(input: FSDataInputStream) extends ImageInputStreamImpl with Logging {

  bitOffset = 0
  seek(0)
  val stack = new Stack[Long]
  def getStream = input
  override def getStreamPosition: Long = {
    streamPos = input.getPos()
    streamPos
  }

  override def mark = stack.push(input.getPos())
  override def read: Int = {
    val start = System.currentTimeMillis()
    bitOffset = 0
    val result = input.read()
    streamPos = input.getPos()
    logDebug(s"read (1) - ${result} (pos: ${streamPos} time: ${System.currentTimeMillis() - start} ms)")
    result
  }

  // NOTE:  Because Geotools resuses the input stream, we can't close it here.  
  // Instead, the person originally opening the stream is responsible for 
  // closing it in the appropriate place. Yuck!
  //  @Override
  //  public void close() throws IOException
  //  {
  //    _input.close();
  //  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val start = System.currentTimeMillis()

    bitOffset = 0;

    // 
    // FSDataInputStream documentation: read() - Reads up to len bytes of data from the 
    // contained input stream into an array of bytes. An attempt is made to read as 
    // many as len bytes, but a smaller number may be read, possibly zero. 
    // The number of bytes actually read is returned as an integer.
    //
    // this means we need to loop until the correct number of bytes are read, -1 is returned

    var bytesRead: Int = 0
    while (bytesRead < len) {
      val result = input.read(b, bytesRead + off, len - bytesRead)

      // no data left, return -1;
      if (result == -1) {
        return result
      }

      streamPos = input.getPos()
      bytesRead = bytesRead + result
    }

    logDebug(s"read ${bytesRead} (pos: ${streamPos} time: ${System.currentTimeMillis() - start} ms)")

    bytesRead
  }

  override def reset: Unit = {
    bitOffset = 0
    if (!stack.isEmpty) {
      seek(stack.pop())
    }
  }

  override def seek(pos: Long) {
    input.seek(pos)
    streamPos = input.getPos()

    if (pos != streamPos) {
      logDebug("seek error!!!");
    }

  }
}
