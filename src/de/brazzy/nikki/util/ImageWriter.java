package de.brazzy.nikki.util;

import de.brazzy.nikki.model.Image;
import de.brazzy.nikki.model.Waypoint;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import mediautil.gen.Rational;
import mediautil.image.ImageResources;
import mediautil.image.jpeg.Entry;
import mediautil.image.jpeg.Exif;
import mediautil.image.jpeg.IFD;
import mediautil.image.jpeg.LLJTran;
import mediautil.image.jpeg.LLJTranException;

/**
 *
 * @author Brazil
 */
public class ImageWriter extends ImageDataIO
{
    private static final byte[] EMPTY_EXIF = readEmptyExif();
    private static byte[] readEmptyExif()
    {
        try
        {
            return IOUtils.toByteArray(ImageWriter.class.getResourceAsStream("empty_exif.bin"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private Image image;

    public ImageWriter(Image img, File directory) throws LLJTranException
    {
        super(new File(directory.getPath(), img.getFileName()), LLJTran.READ_ALL);

        this.image = img;

        if(exifData == null)
        {
            llj.addAppx(EMPTY_EXIF, 0, EMPTY_EXIF.length, true);
            exifData = (Exif)llj.getImageInfo();
        }
        if(mainIFD==null)
        {
            mainIFD = new IFD(0, Exif.UNDEFINED);
            exifData.getIFDs()[0] = mainIFD;
        }
        if(exifIFD==null)
        {
            exifIFD = new IFD(Exif.EXIFOFFSET, Exif.LONG);
            mainIFD.addIFD(exifIFD);            
        }
        if(nikkiIFD == null)
        {
            nikkiIFD = new IFD(Exif.APPLICATIONNOTE, Exif.LONG);
            nikkiIFD.addEntry(ENTRY_NIKKI, new Entry(Exif.ASCII, ENTRY_NIKKI_CONTENT));
            exifIFD.addIFD(nikkiIFD);
        }
        if(img.getWaypoint() != null && gpsIFD == null)
        {
            gpsIFD = new IFD(Exif.GPSINFO, Exif.LONG);
            mainIFD.addIFD(gpsIFD);
        }
    }

    public void saveImage() throws Exception
    {
        try {
            writeTitle();
            writeDescription();
            writeTime();
            writeExport();
            writeGPS();
            writeThumbnail();

            File tmpFile = File.createTempFile("nikki", "tmp", new File(file.getParent()));
            InputStream fip = new BufferedInputStream(new FileInputStream(file));
            OutputStream out = new BufferedOutputStream(
                                    new FileOutputStream(tmpFile));
            llj.refreshAppx();
            llj.xferInfo(fip, out, LLJTran.REPLACE, LLJTran.RETAIN);
            llj.freeMemory();
            fip.close();
            out.close();
            if(!file.delete())
            {
                throw new IllegalStateException();
            }
            if(!tmpFile.renameTo(file))
            {
                throw new IllegalStateException();
            }

        } catch (LLJTranException ex) {
            Logger.getLogger(ImageWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Entry utf8Entry(String content)
    {
        try {
            Entry entry = new Entry(Exif.UNDEFINED);
            byte[] data = content.getBytes("UTF-8");
            for (int i = data.length - 1; i >= 0; i--) {
                entry.setValue(i, Integer.valueOf(data[i]));
            }
            return entry;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex); // can't happen
        }
    }

    private void writeTitle()
    {
        if(image.getTitle() != null)
        {
            nikkiIFD.addEntry(ENTRY_TITLE, utf8Entry(image.getTitle()));
        }
    }

    private void writeDescription()
    {
        if(image.getDescription() != null)
        {
            nikkiIFD.addEntry(ENTRY_DESCRIPTION, utf8Entry(image.getDescription()));
        }
    }

    private void writeTime()
    {
        if(image.getTime() != null)
        {
            String timeString = TIME_FORMAT.print(image.getTime());
            Entry entry = new Entry(Exif.ASCII);
            entry.setValue(0, timeString);
            mainIFD.addEntry(Exif.DATETIME, entry);
            
            String after = exifData.getDataTimeOriginalString();
            
            entry = new Entry(Exif.ASCII);
            entry.setValue(0, image.getTime().getZone().getID());            
            nikkiIFD.addEntry(ENTRY_TIMEZONE, entry);
        }
    }

    private void writeExport()
    {
        Entry entry = new Entry(Exif.BYTE);
        entry.setValue(0, image.getExport() ? 1 : 0);
        nikkiIFD.addEntry(ENTRY_EXPORT, entry);
    }

    private void writeGPS()
    {
        // Set Latitude
        Waypoint wp = image.getWaypoint();
        if(wp != null)
        {
            Entry entry = new Entry(Exif.ASCII);
            entry.setValue(0, wp.getLatitude().getDirection().getCharacter());
            gpsIFD.setEntry(Integer.valueOf(Exif.GPSLatitudeRef), 0, entry);
            
            gpsIFD.setEntry(Integer.valueOf(Exif.GPSLatitude), 0, 
                    writeGpsMagnitude(wp.getLatitude().getMagnitude()));

            entry = new Entry(Exif.ASCII);
            entry.setValue(0, wp.getLongitude().getDirection().getCharacter());
            gpsIFD.setEntry(Integer.valueOf(Exif.GPSLongitudeRef), 0, entry);

            gpsIFD.setEntry(Integer.valueOf(Exif.GPSLongitude), 0, 
                    writeGpsMagnitude(wp.getLongitude().getMagnitude()));
        }
    }

    public static Entry writeGpsMagnitude(double value)
    {
        Entry entry = new Entry(Exif.RATIONAL);

        double magnitude = Math.abs(value);
        int degrees = (int)magnitude;
        double minutes = (magnitude - degrees) * 60.0;
        double seconds = (minutes - (int)minutes) * 60.0;
        
        entry.setValue(0, new Rational(degrees, 1));
        entry.setValue(1, new Rational((int)minutes, 1));
        entry.setValue(2, new Rational((float)seconds));
        
        return entry;
    }
    private void writeThumbnail() throws IOException
    { // TODO: überschreiben eines existierenden Thumbnails?
        //llj.removeThumbnail();
        if(exifData.getThumbnailBytes() == null &&  image.getThumbnail() != null &&
           !llj.setThumbnail(image.getThumbnail(), 0, image.getThumbnail().length,
                             ImageResources.EXT_JPG))
        {
            throw new IllegalStateException();
        }
    }
}
