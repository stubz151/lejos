package lejos.pc.tools;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.io.*;
import lejos.pc.comm.*;


/**
 * Downloads  data from the RConsole running on a MXT <br>
 * Uses USB by default, or Bluetooth  if selected from buttons.
 * If using Bluetooth, you can get a quicker connection entering the name or address 
 * of you NXT.<br>
 * Do NOT click "connect" unless the NXT displays the correct "Console" message.
 * status field shows messages 
 * @author Roger Glassey 6.1.2008
 *
 * 
 */
public class ConsoleViewer extends JFrame implements ActionListener
{

    private static final long serialVersionUID = -4789857573625988062L;
    private JButton connectButton = new JButton("Connect");
    private JRadioButton usbButton = new JRadioButton("USB");
    private JRadioButton btButton = new JRadioButton("BlueTooth");
    private TextField statusField = new TextField(20);
    private TextField nameField = new TextField(10);
    private TextField addrField = new TextField(12);
    ConsoleViewComms comm;
    /**
     * screen area to hold the downloaded data
     */
    private TextArea theLog;

    /**
     * Constructor builds GUI
     */
    public ConsoleViewer()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("View RConsole output from NXT");
        setSize(500, 600);
        buildGui();
        comm = new ConsoleViewComms(this);
    }

    public void buildGui()
    {
        JPanel connectPanel = new JPanel();  //holds  button and text field
        ButtonGroup choiceGroup = new ButtonGroup();
        choiceGroup.add(usbButton);
        usbButton.setSelected(true);
        choiceGroup.add(btButton);
        connectPanel.add(usbButton);
        connectPanel.add(btButton);
        connectPanel.add(new JLabel(" Name"));
        connectPanel.add(nameField);
        connectButton.addActionListener(this);
        connectPanel.add(new JLabel("Addr"));
        connectPanel.add(addrField);

        JPanel statusPanel = new JPanel();//  holds label and text field
        statusPanel.add(connectButton);
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusField);

        JPanel topPanel = new JPanel();  // North area of the frame
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.add(connectPanel);
        topPanel.add(statusPanel);
        add(topPanel, BorderLayout.NORTH);

        theLog = new TextArea(40, 40); // Center area of the frame
        add(theLog, BorderLayout.CENTER);
        statusField.setText("using USB");
    }

    /**
     * Required by action listener; only action is generated by the get Length button
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == connectButton)
        {
            setMessage("Connecting");
            String name = nameField.getText();
            String address = addrField.getText();
            boolean _useUSB = usbButton.isSelected();
            if (!comm.connectTo(name, address, _useUSB))
            {
                setMessage("Connection Failed");
                if (_useUSB)
                {
                    JOptionPane.showMessageDialog(null, "Sorry... USB did not connect.\n" +
                            "You might want to check:\n " +
                            " Is the NXT turned on and connected? \n " +
                            " Does it display  'USB Console...'? ", "We have a connection problem.",
                            JOptionPane.PLAIN_MESSAGE);
                } else
                {
                    JOptionPane.showMessageDialog(null, "Sorry... Bluetooth did not connect. \n" +
                            "You might want to check:\n" +
                            " Is the dongle plugged in?\n" +
                            " Is the NXT turned on?\n" +
                            " Does it display  'BT Console....'? ",
                            "We have a connection problem.",
                            JOptionPane.PLAIN_MESSAGE);
                }
            }
        }
    }

    public void append(String data)
    {
        theLog.append(data);
    }

    public void connectedTo(String name, String address)
    {
        nameField.setText(name);
        addrField.setText(address);
        setMessage("Connected to " + name);
    }

    /**
     * Initialize the display Frame <br>
     */
    public static void main(String[] args)
    {
        ConsoleViewer frame = new ConsoleViewer();
        frame.setVisible(true);
    }

    /**
     *messages generated by  PCcomSerial show in the status Field
     */
    public void setMessage(String s)
    {
        statusField.setText(s);
    }
}
class ConsoleViewComms
{
    private InputStream is = null;
    private OutputStream os = null;
    private NXTConnector con;
    private ConsoleViewer viewer;
    private Reader reader;
    private boolean _connected = false;

    public ConsoleViewComms(ConsoleViewer viewer)
    {
        this.viewer = viewer;
        reader = new Reader();
        reader.start();
    }

    public boolean connectTo(String name, String address, boolean useUSB)
    {
        con = new NXTConnector();
        con.addLogListener(new ToolsLogger());
        if (!con.connectTo(name, address, (useUSB ? NXTCommFactory.USB : NXTCommFactory.BLUETOOTH)))
        {
            return false;
        } else
        {
            _connected = true;
        }
        is = con.getInputStream();
        _connected = _connected && is != null;
        os = con.getOutputStream();
        _connected = _connected && os != null;
        try  // handshake
        {
            byte[] hello = new byte[]
            {
                'C', 'O', 'N'
            };
            os.write(hello);
            os.flush();
        } catch (IOException e)
        {
            System.out.println(e + " handshake failed to write ");
            _connected = false;
            return false;
        }
        if (_connected)
        {
            name = con.getNXTInfo().name;
            address = con.getNXTInfo().deviceAddress;
            viewer.connectedTo(name, address);
            System.out.println(" connection " + name + " " + address);
        }
        return _connected;
    }

    private class Reader extends Thread
    {
        public void run()
        {
            while (true)
            {              
                if (_connected)
                {
                    try
                    {
                        int input;
                        while ((input = is.read()) >= 0)
                        {
                            viewer.append("" + (char) input);
                        }
                        is.close();
                    } catch (IOException e)
                    {
                        _connected = false;
                    }
                }               
                Thread.yield();
            }
        }
    }
}

