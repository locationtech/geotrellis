package org.osgeo.proj4j.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

public class Proj4FileReader 
{

    public Proj4FileReader() {
        super();
    }

    public String[] readParametersFromFile( String authorityCode, String name ) 
        throws IOException 
    {
        // TODO: read comment preceding CS string as CS description
        // TODO: use simpler parser than StreamTokenizer for speed and flexibility
        // TODO: parse CSes line-at-a-time (this allows preserving CS param string for later access)
    
        String filename = "/geotrellis/proj4/nad/" + authorityCode.toLowerCase();
  	InputStream inStr = Proj4FileReader.class.getResourceAsStream( filename );
  	if (inStr == null) {
            throw new IllegalStateException("Unable to access CRS file: " + filename);
  	}
        BufferedReader reader = new BufferedReader( new InputStreamReader(inStr) );
        String[] args;
        try {
            args = readFile(reader, name);
        }
        finally {
            if (reader != null)
                reader.close();
        }
        return args;
    }
  
    private StreamTokenizer createTokenizer(BufferedReader reader)
    {
        StreamTokenizer t = new StreamTokenizer( reader );
        t.commentChar( '#' );
        t.ordinaryChars( '0', '9' );
        t.ordinaryChars( '.', '.' );
        t.ordinaryChars( '-', '-' );
        t.ordinaryChars( '+', '+' );
        t.wordChars( '0', '9' );
        t.wordChars( '\'', '\'' );
        t.wordChars( '"', '"' );
        t.wordChars( '_', '_' );
        t.wordChars( '.', '.' );
        t.wordChars( '-', '-' );
        t.wordChars( '+', '+' );
        t.wordChars( ',', ',' );
        t.wordChars( '@', '@' );
        return t;
    }
  
    private String[] readFile( BufferedReader reader, String name) 
        throws IOException 
    {
        StreamTokenizer t = createTokenizer(reader);
    
        t.nextToken();
        while ( t.ttype == '<' ) {
            t.nextToken();
            if ( t.ttype != StreamTokenizer.TT_WORD ) 
                throw new IOException( t.lineno()+": Word expected after '<'" );
            String crsName = t.sval;
            t.nextToken();
            if ( t.ttype != '>' )
                throw new IOException( t.lineno()+": '>' expected" );
            t.nextToken();
            List v = new ArrayList();

            while ( t.ttype != '<' ) {
                if ( t.ttype == '+' )
                    t.nextToken();
                if ( t.ttype != StreamTokenizer.TT_WORD ) 
                    throw new IOException( t.lineno()+": Word expected after '+'" );
                String key = t.sval;
                t.nextToken();
        

                // parse =arg, if any
                if ( t.ttype == '=' ) {
                    t.nextToken();
                    //Removed check to allow for proj4 hack +nadgrids=@null 
                    //if ( t.ttype != StreamTokenizer.TT_WORD ) 
                    //  throw new IOException( t.lineno()+": Value expected after '='" );
                    String value = t.sval;
                    t.nextToken();
                    addParam(v, key, value);
                }
                else {
                    // add param with no value
                    addParam(v, key, null);
                }
            }
            t.nextToken();
            if ( t.ttype != '>' )
                throw new IOException( t.lineno()+": '<>' expected" );
            t.nextToken();
      
            // found requested CRS?
            if ( crsName.equals( name ) ) {
                String[] args = (String[]) v.toArray( new String[0] );
                return args;
            }
        }
        return null;
    }
  
    private static void addParam(List v, String key, String value)
    {
        String plusKey = key;
        if ( ! key.startsWith("+") )
            plusKey = "+" + key;
    
        if (value != null)
            v.add( plusKey+"="+value );
        else 
            v.add(plusKey);
    }
  
    /**
     * Gets the list of PROJ.4 parameters which define
     * the coordinate system specified by <tt>name</tt>.
     * 
     * @param crsName the name of the coordinate system
     * @return the PROJ.4 projection parameters which define the coordinate system
     */
    public String[] getParameters(String crsName) {
        try {
            int p = crsName.indexOf(':');
            if (p >= 0) {
                String auth = crsName.substring(0, p);
                String id = crsName.substring(p + 1);
                return readParametersFromFile(auth, id);
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
