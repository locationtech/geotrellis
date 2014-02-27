package geotrellis.util

object Units {
  val bytesUnits = List("B", "K", "M", "G", "P")
  def bytes(n:Long) = {
    def xyz(amt:Double, units:List[String]): (Double, String) = units match {
      case Nil => sys.error("invalid units list")
      case u :: Nil => (amt, u)
      case u :: us => if (amt < 1024) (amt, u) else xyz(amt / 1024, us)
    }
    xyz(n, bytesUnits)
  }  
}
