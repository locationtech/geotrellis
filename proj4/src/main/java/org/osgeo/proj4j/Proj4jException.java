/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgeo.proj4j;

/**
 * Signals that a situation or data state has been encountered
 * which prevents computation from proceeding,
 * or which would lead to erroneous results.
 * <p>
 * This is the base class for all exceptions 
 * thrown in the Proj4J API.
 * 
 * @author mbdavis
 *
 */
public class Proj4jException extends RuntimeException 
{
	public Proj4jException() {
		super();
	}

	public Proj4jException(String message) {
		super(message);
	}
}
