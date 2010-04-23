package de.brazzy.nikki.util.log_parser;

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

import java.io.FilenameFilter;
import java.util.NoSuchElementException;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.brazzy.nikki.model.GeoCoordinate;
import de.brazzy.nikki.model.Waypoint;

/**
 * Parses the NMEA GPS file format
 * 
 * @author Michael Borgwardt
 */
public class NmeaParser implements LogParser
{
    private static final FilenameFilter FILTER = 
        new ExtensionFilter("NMEA", "NME");

    /* (non-Javadoc)
     * @see de.brazzy.nikki.util.log_parser.LogParser#getParseableFileNameFilter()
     */
    @Override
    public FilenameFilter getParseableFileNameFilter()
    {
        return FILTER;
    }

    /* (non-Javadoc)
     * @see de.brazzy.nikki.util.log_parser.LogParser#parse(java.io.InputStream)
     */
    @Override
    public Iterator<Waypoint> parse(InputStream input) throws ParserException
    {
        if(!input)
        {
            throw new IllegalArgumentException("GPS input stream is null!");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "US-ASCII"))
        return new NmeaIterator(reader)
    }
}

class NmeaIterator implements Iterator<Waypoint>
{
    private static final DateTimeFormatter PARSE_FORMAT = 
        DateTimeFormat.forPattern('ddMMyyHHmmss.SSS').withZone(DateTimeZone.UTC)
        
    private BufferedReader reader
    private String nextLine

    public NmeaIterator(BufferedReader reader){
        this.reader = reader
        read()
    }
    
    boolean hasNext(){
        return nextLine != null
    }

    Waypoint next(){
        if(!nextLine)
        {
            throw new NoSuchElementException();
        }
        Waypoint result = new Waypoint()
        try
        {
            def data = nextLine.trim().tokenize(',')        
            result.latitude = GeoCoordinate.parse(data[3], data[4])
            result.longitude = GeoCoordinate.parse(data[5], data[6])        
            result.timestamp = PARSE_FORMAT.parseDateTime(data[9]+data[1])             
        }
        catch(Exception ex)
        {
            throw new ParserException("line was: "+nextLine, ex)
        }
        read()
        return result
    }
    
    private void read()
    {
        nextLine = reader.readLine()
        while(nextLine != null && !nextLine.startsWith('$GPRMC'))
        {
            nextLine = reader.readLine()
        }
    }

    void remove(){ throw new UnsupportedOperationException() }
}