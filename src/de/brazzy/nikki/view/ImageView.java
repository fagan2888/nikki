package de.brazzy.nikki.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import de.brazzy.nikki.model.Image;
import de.brazzy.nikki.util.Dialogs;


public class ImageView extends JPanel
{
    public static final int DIFF_THRESHOLD = 30;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
    public static final ImageIcon COPY_ICON = new ImageIcon(ImageView.class.getResource("/icons/page_copy.png"));
    public static final ImageIcon PASTE_ICON = new ImageIcon(ImageView.class.getResource("/icons/paste_plain.png"));
    
    private JTextArea textArea = new JTextArea(2, 40);
    private JLabel thumbnail = new JLabel();
    private JTextField title = new JTextField();
    private JTextField filename = new JTextField();
    private JTextField time = new JTextField();
    private JTextField timeDiff = new JTextField();
    private JTextField latitude = new JTextField();
    private JTextField longitude = new JTextField();
    private JButton offsetFinder = new JButton(
            new ImageIcon(ImageView.class.getResource("/icons/find.png")));
    private JButton copyPaste = new JButton(COPY_ICON);
    private JCheckBox export = new JCheckBox("export");
    
    private DateTime clipboard; // TODO: use real (system?) clipboard
    private Image value;
    private Dialogs dialogs;

    public ImageView(final Dialogs dialogs)
    {
        super(new BorderLayout());
        this.dialogs = dialogs;
        setBorder(new EmptyBorder(5,5,5,5));
        add(thumbnail, BorderLayout.WEST);        
        JPanel grid = new JPanel();
        add(grid, BorderLayout.CENTER);
        
        JLabel filenameLabel = new JLabel("File:");
        JLabel timeLabel = new JLabel("Time:");
        JLabel latitudeLabel = new JLabel("Latitude:");
        JLabel longitudeLabel = new JLabel("Longitude:");
        textArea.setBorder(new EmptyBorder(3,3,3,3));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        offsetFinder.setMargin(new Insets(0,0,0,0));
        offsetFinder.addActionListener(offsetFinderAction);
        copyPaste.setMargin(new Insets(0,0,0,0));
        copyPaste.addActionListener(copyPasteAction);

        GroupLayout layout = new GroupLayout(grid);
        grid.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);        
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(title)
                .addComponent(export)
            )
            .addGroup(
                layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(filenameLabel)
                        .addComponent(latitudeLabel))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(filename)
                        .addComponent(latitude))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(timeLabel)
                        .addComponent(longitudeLabel))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(time)
                                .addComponent(timeDiff)
                                .addComponent(copyPaste))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(longitude)
                                .addComponent(offsetFinder)
                        )))
                .addComponent(scrollPane)                    
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup()
                    .addComponent(title, (int)title.getPreferredSize().getHeight(), (int)title.getPreferredSize().getHeight(), (int)title.getPreferredSize().getHeight())
                    .addComponent(export)
            )
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(filenameLabel)
                    .addComponent(filename)
                    .addComponent(timeLabel)
                    .addComponent(time)
                    .addComponent(timeDiff)
                    .addComponent(copyPaste))
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(latitudeLabel)
                    .addComponent(latitude)
                    .addComponent(longitudeLabel)
                    .addComponent(longitude)
                    .addComponent(offsetFinder))
            .addComponent(scrollPane)
        );
        filename.setEditable(false);
        time.setEditable(false);
        timeDiff.setEditable(false);
        timeDiff.setColumns(5);
        latitude.setEditable(false);
        longitude.setEditable(false);
        offsetFinder.setEnabled(true);
    }

    private ActionListener offsetFinderAction = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e)
        {
            OutputStream tmpOut = null;
            try
            {
                File tmpFile = File.createTempFile("nikki", ".kml");
                tmpOut = new FileOutputStream(tmpFile);
                value.offsetFinder(tmpOut);
                dialogs.open(tmpFile);
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(ImageView.this, ex, "Error showing map", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
            finally
            {
                try
                {
                    tmpOut.close();
                }
                catch (Exception e1)
                {
                    e1.printStackTrace();
                }
            }
        }            
    };
    
    private ActionListener copyPasteAction = new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if(value.getTime() != null)
            {
                clipboard = value.getTime();
            }
            else if(clipboard != null)
            {
                value.pasteTime(clipboard);
                repaint();
            }
            else
            {
                throw new IllegalStateException("button should not be "+
                        "enabled when both time and clipboard are empty");
            }
        }
    };
    
    public void setValue(Image value)
    {
        this.value = value;
        timeDiff.setText(null);
        timeDiff.setToolTipText(null);
        title.setText(value.getTitle());
        filename.setText(value.getFileName());
        if(value.getTime() != null)
        {
            copyPaste.setIcon(COPY_ICON);
            copyPaste.setEnabled(true);
            time.setText(TIMESTAMP_FORMAT.print(value.getTime()));
            setTimeDiff(value);
        }
        else
        {
            copyPaste.setIcon(PASTE_ICON);
            copyPaste.setEnabled(clipboard!=null);
            time.setText(null);
        }
        setGpsData(value);
        thumbnail.setIcon(new ImageIcon(value.getThumbnail()));
        textArea.setText(value.getDescription());
        export.setSelected(value.getExport());
    }

    private void setGpsData(Image value)
    {
        if(value.getWaypoint() != null)
        {
            if(value.getWaypoint().getLatitude()!=null)
            {
                latitude.setText(value.getWaypoint().getLatitude().toString());                
            }
            if(value.getWaypoint().getLongitude()!=null)
            {            
                longitude.setText(value.getWaypoint().getLongitude().toString());            
            }
        }
        else
        {
            latitude.setText("?");
            longitude.setText("?");            
        }
    }

    private void setTimeDiff(Image value)
    {
        if(value.getWaypoint() != null && 
           value.getWaypoint().getTimestamp() != null)
        {
            long diff = (value.getTime().getMillis() 
                         - value.getWaypoint().getTimestamp().getMillis()) 
                       / 1000;
            timeDiff.setText(String.valueOf(diff));
            timeDiff.setToolTipText("Difference between photo time and nearest waypoint time");
            if(Math.abs(diff) > DIFF_THRESHOLD)
            {
                timeDiff.setForeground(Color.RED);
            }
            else
            {
                timeDiff.setForeground(Color.BLACK);                    
            }
        }
        else
        {
            timeDiff.setText(null);
        }
    }

    public Image getValue()
    {
        value.setProperty("description", textArea.getText());
        value.setProperty("title", title.getText());
        value.setProperty("export", export.isSelected());
//        value.setDescription(textArea.getText());
//        value.setTitle(title.getText());
//        value.setExport(export.isSelected());

        return value;
    }    

    
}
