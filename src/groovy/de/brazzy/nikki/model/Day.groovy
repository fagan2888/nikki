package de.brazzy.nikki.model;

import javax.swing.table.AbstractTableModel
import de.micromata.opengis.kml.v_2_2_0.Kmlimport de.micromata.opengis.kml.v_2_2_0.KmlFactoryimport de.micromata.opengis.kml.v_2_2_0.Coordinateimport de.micromata.opengis.kml.v_2_2_0.Documentimport de.micromata.opengis.kml.v_2_2_0.Placemarkimport de.micromata.opengis.kml.v_2_2_0.LineStringimport de.micromata.opengis.kml.v_2_2_0.AltitudeModeimport java.util.zip.ZipOutputStreamimport java.util.zip.ZipEntryimport de.brazzy.nikki.util.ImageReaderimport javax.swing.SwingWorkerimport java.util.TimeZoneimport java.text.DateFormatimport java.util.zip.CRC32class Day extends AbstractTableModel implements Externalizable
{
    public static final long serialVersionUID = 1;
    
    public static final int WAYPOINT_THRESHOLD = 1000 * 80

    List<Waypoint> waypoints = [];
    List<Image> images = [];
    
    Date date;
    Directory directory;
    
    public String toString()
    {        
        def format = DateFormat.getDateInstance();
        format.timeZone = directory.zone;
        (date==null? "unknown" : format.format(date))+" ("+images.size()+", "+waypoints.size()+")"
    }

    public int getRowCount()
    {
        images.size()
    }
    
    public int getColumnCount()
    {
        1
    }
    
    public Object getValueAt(int row, int column)
    {
        images[row]
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) 
    {
        true
    }
    
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(waypoints)
        out.writeObject(images)
        out.writeObject(date)
    }
    
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException
    {
        waypoints = oi.readObject()
        images = oi.readObject()
        date = oi.readObject()
    }
    
    public void geotag(int offset = 0)
    {
        waypoints.sort()
        images.each{ image ->
            def imagetime = new Date(image.time.time + (offset*1000))
            def index = Collections.binarySearch(waypoints, new Waypoint(timestamp:imagetime))
            if(index>=0) // direct hit
            {
                image.waypoint = waypoints[index]
            }
            else if(-index==waypoints.size()+1) // after all WPs
            {
                image.waypoint = waypoints[waypoints.size()-1]
            }
            else if(index == -1) // before all WPs
            {
                image.waypoint = waypoints[0]
            }
            else
            {
                def before = waypoints[-(index+2)]
                def after = waypoints[-(index+1)]
                if(imagetime.time - before.timestamp.time > 
                   after.timestamp.time - imagetime.time)
                {
                    image.waypoint = after
                }
                else
                {
                    image.waypoint = before
                }
            }
        }
    }
    
    public void export(ZipOutputStream out, SwingWorker worker)
    {
        worker.progress = 0;
        Kml kml = KmlFactory.createKml()
        Document doc = kml.createAndSetDocument()
        doc.createAndAddStyle().withId("track")
            .createAndSetLineStyle()
            .withWidth(4.0)
            .withColor("801977FF")
        
        int count = 0;
        out.method = ZipOutputStream.STORED
        def entry = new ZipEntry("images/");
        entry.size = 0;
        entry.crc = 0;
        out.putNextEntry(entry)
        out.closeEntry()
        
        entry = new ZipEntry("thumbs/");
        entry.size = 0;
        entry.crc = 0;
        out.putNextEntry(entry)
        out.closeEntry()
        
        images.each{ Image image ->
            Placemark pm = doc.createAndAddPlacemark()
                .withName(image.title)
                .withDescription(image.longDescription)
                .withVisibility(true)
             pm.createAndSetPoint()
                .withCoordinates([new Coordinate(image.waypoint.longitude.value, image.waypoint.latitude.value)])
             pm.createAndAddStyle()
               .createAndSetIconStyle()
               .withScale(1.5) // adjusts default icon size (64) for out icon size (96)
               .createAndSetIcon()
               .withHref("thumbs/"+image.fileName)

            ImageReader reader = new ImageReader(new File(directory.path, image.fileName), null)
            store(reader.scale(592, false), "images/"+image.fileName, out)
            store(reader.scale(96, true), "thumbs/"+image.fileName, out)
            
            worker.progress = new Integer((int)(++count / images.size * 100))
        }
        
        LineString ls;        
        long previous = 0;
        waypoints.each{ waypoint ->
            if(waypoint.timestamp.time - previous > WAYPOINT_THRESHOLD)
            {
                ls = createLine(doc)
            }
            ls.addToCoordinates(waypoint.longitude.value, waypoint.latitude.value)
            previous = waypoint.timestamp.time
        }
        
        out.method = ZipOutputStream.DEFLATED
        out.putNextEntry(new ZipEntry("diary.kml"))
        kml.marshal(out)
        out.closeEntry()
        out.close()
    }
    
    private store(byte[] imgData, String name, ZipOutputStream out)
    {
        def entry = new ZipEntry(name)
        entry.size = imgData.length
        def crc = new CRC32()
        crc.update(imgData)
        entry.crc = crc.value
        out.putNextEntry(entry)
        out.write(imgData)
        out.closeEntry()
    }

    private LineString createLine(Document doc)
    {
        return doc.createAndAddPlacemark()
            .withStyleUrl("#track")
            .createAndSetLineString()
            .withTessellate(Boolean.TRUE)
            .withExtrude(Boolean.TRUE)
            .withAltitudeMode(AltitudeMode.CLAMP_TO_GROUND)
    }
}
