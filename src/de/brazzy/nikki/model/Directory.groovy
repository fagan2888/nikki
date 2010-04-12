package de.brazzy.nikki.model;
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

import java.io.FilenameFilter
import java.util.HashMap
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.RenderingHints
import de.brazzy.nikki.util.ImageReader
import de.brazzy.nikki.util.ScanResult;
import de.brazzy.nikki.util.TimezoneFinder;

import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.util.Date
import javax.swing.SwingWorker
import java.util.TimeZone
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate;

/**
 * Represents on filesystem directory containing images and GPS tracks
 * from one journey, visible as a list of days.
 * 
 * @author Michael Borgwardt
 */
class Directory extends ListDataModel<Day> implements Comparable<Directory>
{
    public static final long serialVersionUID = 1;
    
    private static final def FILTER_JPG = { dir, name ->
        name.toUpperCase().endsWith(".JPG")
    } as FilenameFilter
    private static final def FILTER_NMEA = { dir, name ->
        name.toUpperCase().endsWith(".NMEA")
    } as FilenameFilter


    /**
     * All the images in this directory, keyed on the file name
     */
    Map<String, Image> images = [:];
    
    /**
     * All the GPS tracks in this directory, keyed on the file name
     */
    Map<String, WaypointFile> waypointFiles = [:];    
    
    /**
     * This directory's filesystem path.
     *  Must not be null (Enforcement currently not possible, as Groovy
     *  ignores "private")
     */
    File path
    
    public String toString()
    {
        path.name+" ("+images.size()+", "+waypointFiles.size()+")"
    }

    /**
     * Scans the filesystem for image and GPS files and processes the data in them
     * 
     * @param worker for updating progress
     * @param zone time zone to which the camera time was set when the images were taken. 
     *             Can be null, which assumes that all images already have time zone
     *             set in their EXIF data
     * @param tzFinder finds time zones for waypoints
     * @return ScanResult.TIMEZONE_MISSING if zone was null and images were found that
     *         have no time zone in their EXIF data
     */
    public ScanResult scan(SwingWorker worker, DateTimeZone zone, TimezoneFinder tzFinder)
    {
        worker?.progress = 0;

        int count = 0;
        def imageFiles = path.listFiles(FILTER_JPG)
        
        for(file in imageFiles){
            if(!this.images[file.name])
            {
                ImageReader reader = new ImageReader(file, zone)
                if(reader.timeZone==null)
                {
                    return ScanResult.TIMEZONE_MISSING
                }
                addImage(reader.createImage())
            }
            
            worker?.progress = new Integer((int)(++count / imageFiles.length * 100))
        }

        def nmeaFiles = path.listFiles(FILTER_NMEA);
        for(file in nmeaFiles){
            if(!this.waypointFiles[file.name])
            {
                WaypointFile wf = WaypointFile.parse(this, file, tzFinder)
                this.waypointFiles[file.name] = wf            
            }
        }       
        
        fireContentsChanged(this, 0, this.size-1)
        worker?.progress = 0
        return ScanResult.COMPLETE
    }
    
    /**
     * Adds an Image, creates a Day as well if necessary
     */
    public void addImage(Image image)
    {
        this.images[image.fileName] = image
        def date = image.time?.toLocalDate()
        def day = getDay(date)
        if(day)
        {
        }
        else
        {
            day = new Day(date:date, directory: this)
            this.add(day)
        } 
        day.images.add(image)
        if(image.waypoint)
        {
            day.waypoints.add(image.waypoint)
        }
        
        def modified = image.modified
        image.day = day
        image.modified = modified
    }

    /**
     * Removes an Image, deletes Day if empty
     */
    public void removeImage(Image image)
    {
        if(!this.images.remove(image.fileName))
        {
            throw new IllegalStateException("tried to remove non-present image ${image.fileName}")
        }
        
        def date = image.time?.toLocalDate()
        def day = getDay(date)
        if(day)
        {
            day.images.remove(image)
            image.day = null
            if(day.images.size() == 0)
            {
                remove(day)
            }
        }
        else
        {
            throw new IllegalStateException("tried to remove image for unknown day $date")            
        }
    }

    /**
     * Returns the Day in this Directory that corresponds to the given date
     */
    public Day getDay(LocalDate date)
    {
        int index = Collections.binarySearch(dataList, new Day(date:date))
        if(index >= 0)
        {
            return getAt(index)
        }
        else
        {
            return null
        }
    }

    /**
     * Saves all changed image data to the EXIF headers
     * 
     * @param worker to update progress
     */
    public void save(SwingWorker worker)
    {
        worker?.progress = 0;
        def count = 0;
        for(image in images.values()){
            if(new File(this.path, image.fileName).exists())
            {
                try
                {
                    image.save(this.path)
                }
                catch(Exception ex)
                {
                    ex.printStackTrace()
                }
            }
            worker?.progress = new Integer((int)(++count/images.size() * 100));
        }

        worker?.progress = 0;
    }

    @Override
    public int hashCode()
    {
        return path.hashCode()
    }
    @Override
    public boolean equals(Object obj)
    {
        if (this.is(obj))
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Directory other = (Directory) obj
        return path.equals(other.path)
    }
    @Override
    public int compareTo(Directory other)
    {
        return path.name.compareTo(other.path.name)
    }
    
}
