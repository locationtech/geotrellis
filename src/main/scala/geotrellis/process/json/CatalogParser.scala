package geotrellis.process.json

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import scala.collection.mutable

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

object CatalogParser {
  val parserFactory = new MappingJsonFactory()

  def error(msg:String) = sys.error(s"Invalid json in catalog: $msg")

  def apply(json:String):CatalogRec = {
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
