package org.osgeo.proj4j;

import org.osgeo.proj4j.io.Proj4FileReader;
import org.osgeo.proj4j.parser.Proj4Parser;

/**
 * A factory which can create {@link CoordinateReferenceSystem}s
 * from a variety of ways
 * of specifying them.
 * This is the primary way of creating coordinate systems 
 * for carrying out projections transformations.
 * <p>
 * <tt>CoordinateReferenceSystem</tt>s can be used to
 * define {@link CoordinateTransform}s to perform transformations
 * on {@link ProjCoordinate}s. 
 * 
 * @author Martin Davis
 *
 */
public class CRSFactory 
{
  private static Proj4FileReader csReader = new Proj4FileReader();
  
  private static Registry registry = new Registry();

	// TODO: add method to allow reading from arbitrary PROJ4 CS file
	
  /**
   * Creates a new factory.
   */
	public CRSFactory()
	{
		
	}
	
  /**
   * Gets the {@link Registry} used by this factory.
   * @return the Registry
   */
  public Registry getRegistry()
  {
    return registry;
  }
  
  /**
   * Creates a {@link CoordinateReferenceSystem} (CRS) from a well-known name.
   * CRS names are of the form: "<tt>authority:code</tt>", 
   * with the components being: 
   * <ul>
   * <li><b><tt>authority</tt></b> is a code for a namespace supported by
   * PROJ.4.  
   * Currently supported values are 
   * <tt>EPSG</tt>, 
   * <tt>ESRI</tt>, 
   * <tt>WORLD</tt>, 
   * <tt>NA83</tt>, 
   * <tt>NAD27</tt>.
   * If no authority is provided, the <tt>EPSG</tt> namespace is assumed.
   * <li><b><tt>code</tt></b> is the id of a coordinate system in the authority namespace.
   * For example, in the <tt>EPSG</tt> namespace a code is an integer value
   * which identifies a CRS definition in the EPSG database.
   * (Codes are read and handled as strings).
   * </ul>
   * An example of a valid CRS name is <tt>EPSG:3005</tt>.
   * <p>
   * @param name the name of a coordinate system, with optional authority prefix
   * @return the {@link CoordinateReferenceSystem} corresponding to the given name
   * @throws UnsupportedParameterException if a PROJ.4 parameter is not supported
   * @throws InvalidValueException if a parameter value is invalid
   * @throws UnknownAuthorityCodeException if the authority code cannot be found
   */
  public CoordinateReferenceSystem createFromName(String name)
  throws UnsupportedParameterException, InvalidValueException, UnknownAuthorityCodeException
  {
    String[] params = csReader.getParameters(name);
    if (params == null)
      throw new UnknownAuthorityCodeException(name);
    return createFromParameters(name, params);
  }
  
  /**
   * Creates a {@link CoordinateReferenceSystem} 
   * from a PROJ.4 projection parameter string.
   * <p>
   * An example of a valid PROJ.4 projection parameter string is:
   * <pre>
   * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
   * </pre>
   * @param name a name for this coordinate system (may be <tt>null</tt> for an anonymous coordinate system)
   * @param paramStr a PROJ.4 projection parameter string
   * @return the specified {@link CoordinateReferenceSystem}
   * @throws UnsupportedParameterException if a given PROJ.4 parameter is not supported
   * @throws InvalidValueException if a supplied parameter value is invalid
   */
  public CoordinateReferenceSystem createFromParameters(String name, String paramStr)
  throws UnsupportedParameterException, InvalidValueException
  {
    return createFromParameters(name, splitParameters(paramStr));
  }
  
  /**
   * Creates a {@link CoordinateReferenceSystem} 
   * defined by an array of PROJ.4 projection parameters.
   * PROJ.4 parameters are generally of the form
   * "<tt>+name=value</tt>".
   * 
   * @param name a name for this coordinate system (may be null)
   * @param params an array of PROJ.4 projection parameters
   * @return a {@link CoordinateReferenceSystem}
   * @throws UnsupportedParameterException if a PROJ.4 parameter is not supported
   * @throws InvalidValueException if a parameter value is invalid
  */
  public CoordinateReferenceSystem createFromParameters(String name, String[] params)
  throws UnsupportedParameterException, InvalidValueException
  {
    if (params == null)
      return null;
    
    Proj4Parser parser = new Proj4Parser(registry);
    return parser.parse(name, params);
  }

  private static String[] splitParameters(String paramStr)
  {
    String[] params = paramStr.split("\\s+");
    return params;
  }
  
}
