package org.osgeo.proj4j;

import java.text.DecimalFormat;


/**
 * Stores a the coordinates for a position  
 * defined relative to some {@link CoordinateReferenceSystem}.
 * The coordinate is defined via X, Y, and optional Z ordinates. 
 * Provides utility methods for comparing the ordinates of two positions and
 * for creating positions from Strings/storing positions as strings.
 * <p>
 * The primary use of this class is to represent coordinate
 * values which are to be transformed
 * by a {@link CoordinateTransform}.
 */
public class ProjCoordinate 
{
  public static String DECIMAL_FORMAT_PATTERN = "0.0###############";
  public static DecimalFormat DECIMAL_FORMAT = new DecimalFormat(DECIMAL_FORMAT_PATTERN);

	/**
	 * The X ordinate for this point. 
	 * <p>
	 * Note: This member variable
	 * can be accessed directly. In the future this direct access should
	 * be replaced with getter and setter methods. This will require 
	 * refactoring of the Proj4J code base.
	 */
	public double x;
	
	/**
	 * The Y ordinate for this point. 
	 * <p>
	 * Note: This member variable
	 * can be accessed directly. In the future this direct access should
	 * be replaced with getter and setter methods. This will require 
	 * refactoring of the Proj4J code base.
	 */
	public double y;
	
	/**
	 * The Z ordinate for this point. 
	 * If this variable has the value <tt>Double.NaN</tt>
	 * then this coordinate does not have a Z value.
	 * <p>
	 * Note: This member variable
	 * can be accessed directly. In the future this direct access should
	 * be replaced with getter and setter methods. This will require 
	 * refactoring of the Proj4J code base.
	 */
	public double z;
	
	/**
	 * Creates a ProjCoordinate with default ordinate values.
	 *
	 */
  public ProjCoordinate()
  {
    this(0.0, 0.0);
  }

	/**
	 * Creates a ProjCoordinate using the provided double parameters.
	 * The first double parameter is the x ordinate (or easting), 
	 * the second double parameter is the y ordinate (or northing), 
	 * and the third double parameter is the z ordinate (elevation or height).
	 * 
	 * Valid values should be passed for all three (3) double parameters. If
	 * you want to create a horizontal-only point without a valid Z value, use
	 * the constructor defined in this class that only accepts two (2) double
	 * parameters.
	 * 
	 * @see #ProjCoordinate(double argX, double argY)
	 */
	public ProjCoordinate(double argX, double argY, double argZ)
	{
		this.x = argX;
		this.y = argY;
		this.z = argZ;
	}
	
	/**
	 * Creates a ProjCoordinate using the provided double parameters.
	 * The first double parameter is the x ordinate (or easting), 
	 * the second double parameter is the y ordinate (or northing). 
	 * This constructor is used to create a "2D" point, so the Z ordinate
	 * is automatically set to Double.NaN. 
	 */
	public ProjCoordinate(double argX, double argY)
	{
		this.x = argX;
		this.y = argY;
		this.z = Double.NaN;
	}
	
	/** 
	 * Create a ProjCoordinate by parsing a String in the same format as returned
	 * by the toString method defined by this class.
	 * 
	 * @param argToParse the string to parse
	 */
	public ProjCoordinate(String argToParse)
	{
		// Make sure the String starts with "ProjCoordinate: ".
		boolean startsWith = argToParse.startsWith("ProjCoordinate: ");
		
		if(startsWith == false)
		{
			IllegalArgumentException toThrow = new IllegalArgumentException
			("The input string was not in the proper format.");
			
			throw toThrow;
		}
		
		// 15 characters should cut out "ProjCoordinate: ".
		String chomped = argToParse.substring(16);
		
		// Get rid of the starting and ending square brackets.
		
		String withoutFrontBracket = chomped.substring(1);
		
		// Calc the position of the last bracket.
		int length = withoutFrontBracket.length();
		int positionOfCharBeforeLast = length - 2;
		String withoutBackBracket = withoutFrontBracket.substring(0, 
				positionOfCharBeforeLast);
		
		// We should be left with just the ordinate values as strings, 
		// separated by spaces. Split them into an array of Strings.
		String[] parts = withoutBackBracket.split(" ");
		
		// Get number of elements in Array. There should be two (2) elements
		// or three (3) elements.
		// If we don't have an array with two (2) or three (3) elements,
		// then we need to throw an exception.
		if(parts.length != 2)
		{
			if(parts.length != 3)
			{
				IllegalArgumentException toThrow = new IllegalArgumentException
				("The input string was not in the proper format.");
				
				throw toThrow;
			}
		}
		
		// Convert strings to doubles.
		this.x = Double.parseDouble(parts[0]);
		this.y = Double.parseDouble(parts[0]);
		
		// You might not always have a Z ordinate. If you do, set it.
		if(parts.length == 3)
		{
			this.z = Double.parseDouble(parts[0]);
		}
	}
	
  /**
   * Sets the value of this coordinate to 
   * be equal to the given coordinate's ordinates.
   * 
   * @param p the coordinate to copy
   */
  public void setValue(ProjCoordinate p)
  {
    this.x = p.x;
    this.y = p.y;
    this.z = p.z;
  }
  
  /**
   * Sets the value of this coordinate to 
   * be equal to the given ordinates.
   * The Z ordinate is set to <tt>NaN</tt>.
   * 
   * @param x the x ordinate
   * @param y the y ordinate
   */
  public void setValue(double x, double y)
  {
    this.x = x;
    this.y = y;
    this.z = Double.NaN;
  }
  
  /**
   * Sets the value of this coordinate to 
   * be equal to the given ordinates.
   * 
   * @param x the x ordinate
   * @param y the y ordinate
   * @param z the z ordinate
   */
  public void setValue(double x, double y, double z)
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  public void clearZ()
  {
    z = Double.NaN;
  }
  
	/**
	 * Returns a boolean indicating if the X ordinate value of the 
	 * ProjCoordinate provided as an ordinate is equal to the X ordinate
	 * value of this ProjCoordinate. Because we are working with floating
	 * point numbers the ordinates are considered equal if the difference
	 * between them is less than the specified tolerance.
	 */	
	public boolean areXOrdinatesEqual(ProjCoordinate argToCompare, 
			double argTolerance)
	{
		// Subtract the x ordinate values and then see if the difference
		// between them is less than the specified tolerance. If the difference
		// is less, return true.
		double difference = argToCompare.x - this.x;
		
		if(difference > argTolerance)
		{
			return false;
		}
		
		else
		{
			return true;
		}
	}
	
	/**
	 * Returns a boolean indicating if the Y ordinate value of the 
	 * ProjCoordinate provided as an ordinate is equal to the Y ordinate
	 * value of this ProjCoordinate. Because we are working with floating
	 * point numbers the ordinates are considered equal if the difference
	 * between them is less than the specified tolerance.
	 */
	public boolean areYOrdinatesEqual(ProjCoordinate argToCompare,
			double argTolerance)
	{
		// Subtract the y ordinate values and then see if the difference
		// between them is less than the specified tolerance. If the difference
		// is less, return true.
		double difference = argToCompare.y - this.y;
		
		if(difference > argTolerance)
		{
			return false;
		}
		
		else
		{
			return true;
		}
	}
	
	/**
	 * Returns a boolean indicating if the Z ordinate value of the 
	 * ProjCoordinate provided as an ordinate is equal to the Z ordinate
	 * value of this ProjCoordinate. Because we are working with floating
	 * point numbers the ordinates are considered equal if the difference
	 * between them is less than the specified tolerance.
	 * 
	 * If both Z ordinate values are Double.NaN this method will return
	 * true. If one Z ordinate value is a valid double value and one is
	 * Double.Nan, this method will return false.
	 */
	public boolean areZOrdinatesEqual(ProjCoordinate argToCompare,
			double argTolerance)
	{
		// We have to handle Double.NaN values here, because not every
		// ProjCoordinate will have a valid Z Value.
		if(Double.isNaN(z))
		{
			if(Double.isNaN(argToCompare.z))
			{
				// Both the z ordinate values are Double.Nan. Return true.
				return true;
			}
			
			else
			{
				// We've got one z ordinate with a valid value and one with
				// a Double.NaN value. Return false.
				return false;
			}
		}
		
		// We have a valid z ordinate value in this ProjCoordinate object.
		else
		{
			if(Double.isNaN(argToCompare.z))
			{
				// We've got one z ordinate with a valid value and one with
				// a Double.NaN value. Return false.
				return false;
			}

			// If we get to this point in the method execution, we have to
			// z ordinates with valid values, and we need to do a regular 
			// comparison. This is done in the remainder of the method.
		}
		
		// Subtract the z ordinate values and then see if the difference
		// between them is less than the specified tolerance. If the difference
		// is less, return true.
		double difference = argToCompare.z - this.z;
		
		if(difference > argTolerance)
		{
			return false;
		}
		
		else
		{
			return true;
		}
	}
	
  public boolean equals(Object other) {
    if (!(other instanceof ProjCoordinate)) {
      return false;
    }
    ProjCoordinate p = (ProjCoordinate) other;
    if (x != p.x) {
      return false;
    }
    if (y != p.y) {
      return false;
    }
    return true;
  }

  /**
   * Gets a hashcode for this coordinate.
   * 
   * @return a hashcode for this coordinate
   */
  public int hashCode() {
    //Algorithm from Effective Java by Joshua Bloch [Jon Aquino]
    int result = 17;
    result = 37 * result + hashCode(x);
    result = 37 * result + hashCode(y);
    return result;
  }

  /**
   * Computes a hash code for a double value, using the algorithm from
   * Joshua Bloch's book <i>Effective Java"</i>
   * 
   * @return a hashcode for the double value
   */
  private static int hashCode(double x) {
    long f = Double.doubleToLongBits(x);
    return (int)(f^(f>>>32));
  }

	/**
	 * Returns a string representing the ProjPoint in the format:
	 * <tt>ProjCoordinate[X Y Z]</tt>.
	 * <p>
	 * Example: 
	 * <pre>
	 *    ProjCoordinate[6241.11 5218.25 12.3]
	 * </pre>
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("ProjCoordinate[");
		builder.append(this.x);
		builder.append(" ");
		builder.append(this.y);
		builder.append(" ");
		builder.append(this.z);
		builder.append("]");
		
		return builder.toString();
	}

	/**
	 * Returns a string representing the ProjPoint in the format:
	 * <tt>[X Y]</tt> 
	 * or <tt>[X, Y, Z]</tt>.
	 * Z is not displayed if it is NaN.
	 * <p>
	 * Example: 
	 * <pre>
	 * 		[6241.11, 5218.25, 12.3]
	 * </pre>
	 */
	public String toShortString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(DECIMAL_FORMAT.format(x));
		builder.append(", ");
		builder.append(DECIMAL_FORMAT.format(y));
		if (! Double.isNaN(z)) {
			builder.append(", ");
			builder.append(this.z);
		}
		builder.append("]");
		
		return builder.toString();
	}
	
	public boolean hasValidZOrdinate()
	{
		if(Double.isNaN(this.z))
		{
			return false;
		}
		
		else
		{
			return true;
		}
	}
	
	/**
	 * Indicates if this ProjCoordinate has valid X ordinate and Y ordinate
	 * values. Values are considered invalid if they are Double.NaN or 
	 * positive/negative infinity.
	 */
	public boolean hasValidXandYOrdinates()
	{
		if(Double.isNaN(x))
		{
			return false;
		}
		
		else if(Double.isInfinite(this.x) == true)
		{
			return false;
		}
		
		if(Double.isNaN(y))
		{
			return false;
		}
		
		else if(Double.isInfinite(this.y) == true)
		{
			return false;
		}
		
		else
		{
			return true;
		}
	}
}
