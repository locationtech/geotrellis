package geotrellis.process

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import scala.collection.mutable

object CatalogJson {
  val parserFactory = new MappingJsonFactory()

  def error(msg:String) = sys.error(s"Invalid json in catalog: $msg")

  def parse(json:String):CatalogRec = {
    var catalog = ""
    val stores = mutable.ListBuffer[DataStoreRec]()

    val parser = parserFactory.createJsonParser(json)

    if(parser.nextToken() != START_OBJECT) 
      error("Json does not start as object.")
    else { 
      var token = parser.nextToken()
      while (token != null) {
        token match {
          case FIELD_NAME => 
            parser.getCurrentName() match {
              case "catalog" =>
                parser.nextToken()
                catalog = parser.getText()
              case "stores" =>
                if(parser.nextToken() != START_ARRAY) error("Stores must be an array.")
                token = parser.nextToken()
                while(token != END_ARRAY && token != null) {
                  token match {
                    case START_OBJECT =>
                      stores.append(parseDataStore(parser))
                    case _ => error("Stores must be an array of objects.")
                  }
                  token = parser.nextToken()
                }
              case f => error(s"Unknown field name $f.")
            }
          case END_OBJECT => // Done.
          case _ => 
            error("Expecting a field name.")
        }
        token = parser.nextToken()    
      }
    }

    if(catalog == "") error("Catalog must have a name field 'catalog' be non-empty")

    CatalogRec(catalog,stores.toList)
  }

  private def parseDataStore(parser:JsonParser):DataStoreRec = {
    var store = ""
    var params = mutable.HashMap[String,String]()
    var token = parser.nextToken()
    while(token != null && token != END_OBJECT) {
      token match {
        case FIELD_NAME =>
          parser.getCurrentName() match {
            case "store" =>
              parser.nextToken()
              store = parser.getText()
            case "params" =>
              if(parser.nextToken() != START_OBJECT) error("Expecting start of object.")
              token = parser.nextToken()
              while(token != null && token != END_OBJECT) {
                params(parser.getCurrentName()) = { parser.nextToken() ; parser.getText() }
                token = parser.nextToken()
              }
            case f => error(s"Unexpected field name $f") // Unknown name
          }
        case _ => 
          error("Expecting a field name.")
      }
      token = parser.nextToken()
    }

    if(store == "") error("Store must have a name field 'store' be non-empty")
    if(!params.contains("type")) error("Store must include a field 'type' in it's parameters.")
    if(!params.contains("path")) error("Store must include a field 'path' in it's parameters.")

    DataStoreRec(store, params.toMap)
  }
}

object RasterLayerJson {
  class RequiredField(f:JsonParser=>Unit) {
    var hit = false

    def set(parser:JsonParser) = { f(parser) ; hit = true }
  }

  val parserFactory = new MappingJsonFactory()

  def error(msg:String) = sys.error(s"Invalid json for arg file: $msg")

  def parse(json:String):RasterLayerRec = {
    var layer = ""
    var ltype = ""
    var datatype = ""
    var xmin = 0.0
    var xmax = 0.0
    var ymin = 0.0
    var ymax = 0.0
    var cols = 0
    var rows = 0
    var cellheight = 0.0
    var cellwidth = 0.0
    var epsg = 0
    var yskew = 0.0
    var xskew = 0.0

    val requiredFields = Map(
      ( "layer", new RequiredField(parser => layer = parser.getText())),
      ( "type", new RequiredField(parser => ltype = parser.getText())),
      ( "datatype", new RequiredField(parser => datatype = parser.getText())),
      ( "xmin", new RequiredField(parser => xmin = parser.getDoubleValue())),
      ( "xmax", new RequiredField(parser => xmax = parser.getDoubleValue())),
      ( "ymin", new RequiredField(parser => ymin = parser.getDoubleValue())),
      ( "ymax", new RequiredField(parser => ymax = parser.getDoubleValue())),
      ( "cols", new RequiredField(parser => cols = parser.getIntValue())),
      ( "rows", new RequiredField(parser => rows = parser.getIntValue())),
      ( "cellheight", new RequiredField(parser => cellheight = parser.getDoubleValue())),
      ( "cellwidth", new RequiredField(parser => cellwidth = parser.getDoubleValue())),
      ( "epsg", new RequiredField(parser => epsg = parser.getIntValue())),
      ( "yskew", new RequiredField(parser => yskew = parser.getDoubleValue())),
      ( "xskew", new RequiredField(parser => xskew = parser.getDoubleValue()))
    )

    val parser = parserFactory.createJsonParser(json)
    
    if(parser.nextToken() != START_OBJECT) 
      error("No data available.")
    else { 
      var token = parser.nextToken()

      while(token != null) {
        token match {
          case FIELD_NAME => {
            val name = parser.getCurrentName()
            if(requiredFields.contains(name)) {
              parser.nextToken()
              requiredFields(name).set(parser)
            } else {
              parser.nextToken() //Ignore
            }
          }
          case END_OBJECT => // Done
          case _ => error(s"Unexpected token $token.")
        }
        token = parser.nextToken()
      }
    }

    requiredFields.values.find(v => !v.hit) match {
      case Some(kv) =>
        val s = requiredFields.filter(kv => !kv._2.hit).map(kv => kv._1).reduceLeft((a,b) => s"$a, $b")
        error(s"Required fields not found: $s")
      case None =>
        RasterLayerRec(layer, ltype, datatype, xmin, xmax, ymin, ymax, 
                       cols, rows, cellheight, cellwidth, 
                       epsg, yskew, xskew)
    }
  }
}
