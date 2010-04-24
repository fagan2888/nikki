package de.brazzy.nikki.model
/*   
 *   Copyright 2010 Michael Borgwardt
 *   Part of the Nikki Photo GPS diary:  http://www.brazzy.de/nikki
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 * Represents one dimension (latitude or longitude) of a GPS coordinate
 * 
 * @author Michael Borgwardt
 */
public class GeoCoordinate implements Serializable{
    public static final long serialVersionUID = 1;

    /**
     * Cardinal direction of this dimension (determines sign)
     */
    Cardinal direction;
    
    /**
     * Unsigned magnitude of this dimension
     */
    double magnitude;
    
    /**
     * @return Signed magnitude of this dimension
     */
    public double getValue()
    {
        return magnitude * direction.sign
    }
    
    public String toString()
    {
        return String.valueOf(getValue()) + " " +direction;
    }
}
