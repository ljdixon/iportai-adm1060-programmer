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
import jssc.SerialPortException;

/**
 *
 * @author ldixon
 */
public class Adm1060_v1 {
    static SerialPort serialPort;
    static public boolean iPortReady = false;
    static public boolean i2cAddrSet = false;
    static public boolean i2cPortOpen = false;
    static public boolean i2cReadComplete = false;
    static public boolean dataReady = false;
    static public boolean exitFlag = false;
    static public boolean has_erased_eeprom = false;
    static public boolean has_written_to_eeprom = false;
    static public boolean has_written_to_ram = false;
    static public List<String> data = null; //new ArrayList<>();
    static public int selectedPort;
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please supply arguments");
        } else {
            if(parseFile(args[1])) {
                serialPort = new SerialPort(args[0]);
                try {                
                    System.out.println("Opening Adapter Connection on [" + args[0] + "]...");
                    serialPort.openPort();//Open serial port
                    serialPort.setParams(SerialPort.BAUDRATE_19200, SerialPort.DATABITS_8,SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);//Set params.'
                    serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

                    //reset iPort/AI
                    serialPort.writeByte((byte)0x12);
                    serialPort.writeByte((byte)0x12);
                    serialPort.writeByte((byte)0x12);

                    //check to make sure iPort is up and running
                    if(getReply(1000).replace("\n", "").replace("\r", "").equals("*")) {
                        System.out.println("iPort/AI Ready");
                        iPortReady = true;
                    }
                    
                    serialPort.writeBytes("/DA8\r".getBytes());//Write data to port
                    sleep(100);
                    System.out.println("i2c Slave Address Set");
                    i2cAddrSet = true;

                    serialPort.writeBytes("/O\r".getBytes());//Write data to port
                    if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/OCC")) {
                        System.out.println("i2c Port Open");
                        i2cPortOpen = true;
                    }
                    if(iPortReady && i2cAddrSet && !has_written_to_eeprom) {
                        System.out.println("Erasing EEPROM");
                        adm_erase_eeprom();
                        System.out.println("Programming EEPROM");
                        adm_program_eeprom();
                        System.out.println("Writing registers");
                        
                        serialPort.writeBytes("/t~90~04\r".getBytes());//Write data to port
                        if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                            sleep(100);
                            serialPort.writeBytes("/t~90\r".getBytes());//Write data to port
                            if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                                serialPort.writeBytes("/r1\r".getBytes());//Write data to port
                                if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MRC")) {
                                    sleep(100);
                                    String result = serialPort.readString();
                                    System.out.println(result);
                                    if(result.equals("~00")) {
                                        serialPort.writeBytes("/t~90~01\r".getBytes());//Write data to port
                                        if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                                            System.out.println("Programming complete");
                                        }
                                    }
                                }
                            }
                        }
                    }

                    serialPort.writeBytes("/C\r".getBytes());//Write data to port
                    if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/CCC")) {
                        System.out.println("i2c Port Closed");
                        i2cPortOpen = false;
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
    
    static void adm_erase_eeprom() {
        try {
            //Enable erasure
            serialPort.writeBytes("/t~90~08\r".getBytes());//Write data to port
            if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                double percentComplete = 0;
                System.out.println(Math.round(percentComplete)+"%");
                for(int addr = 0xf800; addr <= 0xf9ff; addr += 0x20) {
                    percentComplete += 6.25;
                    String address = Integer.toHexString(addr);
                    String lowByte = address.substring(0,2);
                    String highByte = address.substring(2,4);

                    try {
                        serialPort.writeBytes(("/t~"+lowByte+"~"+highByte+"\r").getBytes());//Write data to port
                        if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                            serialPort.writeBytes("/t~fe\r".getBytes());//Write data to port
                            if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                                System.out.println(Math.round(percentComplete)+"%");
                            }
                        }
                    } catch (SerialPortException ex) {
                        System.out.println(ex);
                    }
                }
                has_erased_eeprom = true;
            }
        } catch (SerialPortException ex) {
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
                if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                    if(count % 32 == 0) {
                        percentComplete += 14.3;
                        System.out.println(Math.round(percentComplete)+"%");
                    }
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
            count++;
        }
        has_erased_eeprom = true;
    }
    
    static void adm_program_registers() {
        double percentComplete = 0;
        System.out.println(Math.round(percentComplete)+"%");
        int count = 0;
        for(int addr = 0x00; addr < 0xdf; addr += 0x01) {
            String address = Integer.toHexString(addr);

            try {
                serialPort.writeBytes(("/*t~"+address+"~"+data.get(count)+"\r").getBytes());//Write data to port
                if(getReply(1000).replace("\n", "").replace("\r", "").replace("*", "").equals("/MTC")) {
                    if(count % 32 == 0) {
                        percentComplete += 14.3;
                        System.out.println(Math.round(percentComplete)+"%");
                    }
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
            count++;
        }
        has_erased_eeprom = true;
    }
    
    static void adm_read_eeprom() {
        
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
    
    static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
   //Listen to the IPortAI for a reply
    //Takes:
    //	timeout: int - time in milliseconds
    //Returns:
    //	int: 1 on success, 0 on timeout
    static String getReply (int timeout) {
        String reply;
        //Run continuously, until a reply is gathered or the iPortAI stops responding
        while(true) {
            sleep(30);
            try {
                if(serialPort.getInputBufferBytesCount() > 0) { 
                    reply = serialPort.readString();
                    break;
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
        //System.out.println(reply);
        return reply;
    }
}
