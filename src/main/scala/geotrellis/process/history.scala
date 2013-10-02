package geotrellis.process

import scala.collection.mutable

import geotrellis._

object TreeChars {
  val DOWN_OUT = "├"
  val OUT_DOWN = "┬"
  val TOP_OUT = "└"
  val IN_BOTTOM = "¬"
  val OUT_BOTTOM = "┌"
  val OUT = "─"
  val DOWN = "│"
}

object History {
  def apply(op:Op[_]) =
    new History(op.name,Nil,None,System.currentTimeMillis,0)

  def literal[T](v:T) =
    new History("Literal",Nil,None,System.currentTimeMillis,0).withResult(v)
}

abstract sealed trait HistoryResult

case class Success(value:String) extends HistoryResult
case class Failure(msg:String,trace:String) extends HistoryResult

case class History(id:String,
                     steps:List[StepHistory],
                     result:Option[HistoryResult],
                     startTime:Long,
                     endTime:Long) {
  def withResult[T](value:T) = {
    val now = System.currentTimeMillis
    val s = 
      value match {
        case null => "null"
        case s:String => s""""$s""""
        case v:Vector[_] => 
          val sb = new StringBuilder
          sb.append("Vector(")
          val len = v.length
          for(i <- 0 until len) {
            sb.append(v(i).getClass.getSimpleName)
            if(i != len - 1) { sb.append(",") }
          }
          sb.append(")")
          sb.toString
        case l:List[_] => 
          val sb = new StringBuilder
          sb.append("List(")
          val len = l.length
          for(i <- 0 until len) {
            sb.append(l(i).getClass.getSimpleName)
            if(i != len - 1) { sb.append(",") }
          }
          sb.append(")")
          sb.toString
        case _ => value.getClass.getSimpleName
      }
    new History(id,steps,Some(Success(s)),startTime,now)
  }

  def withError(msg:String,trace:String) = {
    val now = System.currentTimeMillis
    new History(id,steps,Some(Failure(msg,trace)),startTime,now)
  }

  def withStep(step:StepHistory) =
    new History(id,step :: steps,result,startTime,endTime)

  def withStep(stepHistorys:List[History]) =
    new History(id,StepHistory(stepHistorys) :: steps,result,startTime,endTime)

  override
  def toString:String = 
    toString("",false)

  def toString(indentString:String,initialIndent:Boolean):String = {
    val halfLen = id.length/2
    val sb = new StringBuilder
    if(initialIndent) {
      sb.append(indentString)
    }
    sb.append(s"$id\n")
    val stepIndentString = indentString + (" " * halfLen) + TreeChars.DOWN + (" " * halfLen)
    val stepOpIndentString = indentString + (" " * halfLen) + TreeChars.DOWN_OUT + (TreeChars.OUT * halfLen)
    for(step <- steps.reverse) {
      sb.append(step.toString(stepIndentString, stepOpIndentString))
    }
    sb.append(indentString)
    sb.append(" " * halfLen)
    sb.append(TreeChars.TOP_OUT)
    sb.append(TreeChars.OUT * halfLen)
    sb.append("Result: ")
    sb.append(result match {
      case Some(Success(s)) => s"$s (in ${endTime - startTime} ms)"
      case Some(Failure(msg,_)) => s"ERROR: $msg (in ${endTime - startTime} ms)"
      case None => "No Result"
    })
    sb.append("\n")
    sb.toString
  }
}

case class StepHistory(opHistorys:List[History]) {
  override
  def toString:String =
    toString("","")

  def toString(indentString:String,opIndentString:String):String = {
    val sb = new StringBuilder
    val len = opHistorys.length

    val firstOpIndentString = opIndentString + TreeChars.OUT_DOWN + TreeChars.OUT
    val otherOpIndentString = indentString + TreeChars.DOWN_OUT + TreeChars.OUT
    val lastOpIndentString = indentString + TreeChars.TOP_OUT + TreeChars.OUT
    val betweenIndentString = indentString + TreeChars.DOWN + " "

    for(i <- 0 until len) {
      if(i == 0 && len == 1) {
        sb.append(opIndentString + (TreeChars.OUT * 2))
      } else if(i == len - 1) { 
        sb.append(lastOpIndentString)
      } else if(i == 0) {
        sb.append(firstOpIndentString) 
      } else { 
        sb.append(otherOpIndentString) 
      }
      val ins = 
        if(i == len - 1) { indentString + (" " * 2) }
        else { betweenIndentString }
      sb.append(opHistorys(i).toString(ins,false))
    }
    sb.toString
  }
}
