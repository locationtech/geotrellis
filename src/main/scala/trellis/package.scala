package object trellis {
  // TOOD: phase out trellis.contants package
  final val NODATA = Int.MinValue
}

package trellis {
  package object process {
    type Callback[T] = (List[Any]) => StepOutput[T]
    type Args = List[Any]
  
    def time() = System.currentTimeMillis
    def log(msg:String) = if (false) println(msg)
  }
}
