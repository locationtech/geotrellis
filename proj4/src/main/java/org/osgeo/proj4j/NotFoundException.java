package org.osgeo.proj4j;

/**
 * @author Manuri Perera
 */
public class NotFoundException extends Exception{
    public NotFoundException(){
        super();
    }

    public NotFoundException(String message){
        super(message);
    }
}
