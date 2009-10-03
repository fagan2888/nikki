package de.brazzy.nikki.util;

import javax.swing.SwingWorker;

import de.brazzy.nikki.model.Directory;

public class ScanWorker extends SwingWorker<Void, Void>
{
    private Directory dir;
    
    public ScanWorker(Directory dir)
    {
        super();
        this.dir = dir;
    }
    
    @Override
    protected Void doInBackground() throws Exception
    {
        try
        {
            dir.scan(this);            
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }   
}
