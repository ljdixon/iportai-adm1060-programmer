/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iPortAI;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *
 * @author ldixon
 */
public class Adm1060_new {
    static SerialPort serialPort;
    static public boolean iPortReady = false;
    static public boolean i2cAddrSet = false;
    static public boolean i2cPortOpen = false;
    static public boolean i2cReadComplete = false;
    static public boolean dataReady = false;
    static public boolean masterTxComplete = false;
    static public boolean has_erased_eeprom = false;
    static public boolean has_written_to_eeprom = false;
    static public boolean has_written_to_ram = false;
    static public List<String> data = null; //new ArrayList<>();
    
    public static void main(String[] args) {
       if (args.length != 2) {
            System.out.println("Please supply arguments");
        } else {
            if(parseFile(args[1])) {
                serialPort = new SerialPort(args[0]);
                try {                
                    System.out.println("Opening Adapter Connection on [" + args[0] + "]...");
                    serialPort.openPort();//Open serial port
                    serialPort.setParams(19200, 8, 1, 0);//Set params.

                    //reset iPort/AI
                    serialPort.writeByte((byte)0x12);
                    serialPort.writeByte((byte)0x12);
                    serialPort.writeByte((byte)0x12);

                    //check to make sure iPort is up and running
                    while(true) {
                        if(serialPort.getInputBufferBytesCount() > 0) {
                            if(serialPort.readString().replace("\n", "").replace("\r", "").equals("*")) {
                                iPortReady = true;
                                System.out.println("iPort/AI Ready");
                                break;
                            }
                        }
                    }

                    serialPort.writeBytes("/DA8\r".getBytes());//Write data to port

                    //check to make sure iPort is up and running
                    while(true) {
                        if(serialPort.getInputBufferBytesCount() > 0) {
                            if(serialPort.readString().replace("\n", "").replace("\r", "").equals("*")) {
                                i2cAddrSet = true;
                                System.out.println("i2c Slave Address Set");
                                break;
                            }
                        }
                    }

                    //add event listener to handle serial data
                    serialPort.addEventListener(new SerialPortReader());//Add SerialPortEventListener

                    serialPort.writeBytes("/O\r".getBytes());//Write data to port

                    System.out.print("Opening i2c Port");

                    while(!i2cPortOpen) {
                        //System.out.print(".");
                    }

                    System.out.println("");
                    System.out.println("i2c Port Open");
                    System.out.println("Erasing EEPROM");

                    adm_erase_eeprom();

                    System.out.println("Programming EEPROM");

                    adm_program_eeprom();

                    serialPort.writeBytes("/C\r".getBytes());//Write data to port

                    System.out.println("Closing i2c Port");

                    while(i2cPortOpen) {
                        //System.out.print(".");
                    }

                    System.out.println("");
                    System.out.println("i2c Port Closed");

                    serialPort.removeEventListener();//Remove SerialPortEventListener
                    serialPort.closePort();
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
    
    static class SerialPortReader implements SerialPortEventListener {
        private String data;
        private ArrayList<Character> buffer = new ArrayList<>();
        @Override
        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0){ //If data is available
                try {
                    if(serialPort.getInputBufferBytesCount() > 0) {
                        data = serialPort.readString();//Read all available from serial port and add it to buffer
                        if(!data.equals("\n") && !data.equals("\r") && !data.equals("\t") && !data.equals("*") && !data.equals("")) {
                            buffer.add(data.charAt(0));
                            if(buffer.size() == 4) {
                                switch (getStringRepresentation(buffer)) {
                                    case "/OCC":
                                        i2cPortOpen = true;
                                        buffer.clear();
                                        break;
                                    case "/CCC":
                                        i2cPortOpen = false;
                                        buffer.clear();
                                        break;
                                    case "/MTC":
                                        masterTxComplete = true;
                                        buffer.clear();
                                        break;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    //Do nothing
                }
            }
        }
    }
    
    static void adm_erase_eeprom() {
        try {
            //Enable erasure
            serialPort.writeBytes("/t~90~08\r".getBytes());//Write data to port
            while(!masterTxComplete) {
                
            }
            
            masterTxComplete = false;
            double percentComplete = 0;
            System.out.println(Math.round(percentComplete)+"%");
            for(int addr = 0xf800; addr <= 0xf9ff; addr += 0x20) {
                percentComplete += 6.25;
                String address = Integer.toHexString(addr);
                String lowByte = address.substring(0,2);
                String highByte = address.substring(2,4);
                
                serialPort.writeBytes(("/t~"+lowByte+"~"+highByte+"\r").getBytes());//Write data to port
                while(!masterTxComplete) {
                
                }
                masterTxComplete = false;
                
                serialPort.writeBytes("/t~fe\r".getBytes());//Write data to port
                while(!masterTxComplete) {
                
                }
                masterTxComplete = false;
                
                try {
                    Thread.sleep(20);
                } catch(InterruptedException e) {
                    
                }

                System.out.println(Math.round(percentComplete)+"%");
            }
            has_erased_eeprom = true;
        } catch(SerialPortException ex) {
            System.out.println(ex);
        }
    }
    
    static void adm_program_eeprom() {
        double percentComplete = 0;
        System.out.println(Math.round(percentComplete)+"%");
        int count = 0;
        for(int addr = 0xf800; addr < 0xf8df; addr += 0x01) {
            String address = Integer.toHexString(addr);
            String lowByte = address.substring(0,2);
            String highByte = address.substring(2,4);

            try {
                serialPort.writeBytes(("/t~"+lowByte+"~"+highByte+"~"+data.get(count)+"\r").getBytes());//Write data to port
                while(!masterTxComplete) {
                
                }
                
                masterTxComplete = false;
                
                if(count % 32 == 0) {
                    percentComplete += 14.3;
                    System.out.println(Math.round(percentComplete)+"%");
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
            count++;
        }
        has_written_to_eeprom = true;
    }
    
    static boolean parseFile(String filename) {
        File file = new File(filename);
        String dataBytes = "";
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Adm1060.class.getName()).log(Level.SEVERE, null, ex);
        }
        String lineSeparator = System.getProperty("line.separator");

        try {
            while(scanner.hasNextLine()) {        
                fileContents.append(scanner.nextLine()).append(lineSeparator);
            }
            dataBytes = fileContents.toString();
        } finally {
            scanner.close();
        }
        
        Pattern pattern = Pattern.compile("(?!<{1})([A-Fa-f0-9]{2})(;)([A-Fa-f0-9]{2})(?=>{1})");

        Matcher matcher = pattern.matcher(dataBytes);

        boolean found = false;
        data = new ArrayList<>();
        while (matcher.find()) {
            String matches[] = matcher.group().split(";");
            data.add(matches[1]);
            found = true;
        }
        
        if(!found){
            System.out.println("No match found.");
        }
        return true;
    }
    
    static String getStringRepresentation(ArrayList<Character> list) {    
        StringBuilder builder = new StringBuilder(list.size());
        for(Character ch: list)
        {
            builder.append(ch);
        }
        return builder.toString();
    }
}
