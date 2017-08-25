/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iPortAI;

import java.io.Console;
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
public class Adm1060Console {
    static SerialPort serialPort;
    static public boolean iPortReady = false;
    static public boolean i2cAddrSet = false;
    static public boolean i2cPortOpen = false;
    static public boolean i2cReadComplete = false;
    static public boolean dataReady = false;
    static public boolean masterTxComplete = false;
    static public boolean masterRxComplete = false;
    static public boolean has_erased_eeprom = false;
    static public boolean has_written_to_eeprom = false;
    static public boolean has_written_to_ram = false;
    static public List<String> data = null;
    static public String receivedData = "";
    static public String userInput = "";
    static public boolean exit = false;
    
    public static void main(String[] args) {
       if (args.length != 2) {
            System.out.println("Please supply arguments");
        } else {
            if(parseFile(args[1])) {
                serialPort = new SerialPort(args[0]);
                Console console = System.console();
                try {                
                    System.out.println("Opening Adapter Connection on [" + args[0] + "]...");
                    serialPort.openPort();//Open serial port
                    serialPort.setParams(19200, 8, 1, 0);//Set params.
                    
                    if(iPortAI_reset() == 1) {
                    
                        if(iPortAI_setAddress("a8") == 1) {

                            if(iPortAI_open() == 1) {
                                while(!exit) {
                                    if(adm_erase_eeprom() == 1) {
                                        adm_program_eeprom();
                                    }
                                    
                                    while(true) {
                                        userInput = console.readLine("Press Y to run again or N to exit:");

                                        if(userInput.equals("Y") || userInput.equals("y")) {
                                            exit = false;
                                            break;
                                        } else if(userInput.equals("N") || userInput.equals("n")) {
                                            exit = true;
                                            break;
                                        } else {
                                            System.out.println("Whatcha talkin bout Willis?");
                                        } 
                                    }
                                }
                                
                            }
                        }
                    }
                    

                    
                    
                    
                   /* System.out.println("Writing registers");
                    
                    serialPort.writeBytes("/t~90~04\r".getBytes());//Write data to port
                    while(!masterTxComplete) {
                
                    }

                    masterTxComplete = false;
   
                    
                    serialPort.writeBytes("/t~90\r".getBytes());//Write data to port
                    while(!masterTxComplete) {
                
                    }

                    masterTxComplete = false;

                    serialPort.writeBytes("/r1\r".getBytes());//Write data to port
                    while(!masterRxComplete) {
                
                    }
                    
                    while(!has_written_to_ram) {
                        
                    }
                    
                    if(receivedData.equals("~00")) {
                        
                        masterRxComplete = false;
                        
                        serialPort.writeBytes("/t~90~02\r".getBytes());//Write data to port
                        while(!masterTxComplete) {
                            
                        }

                        masterTxComplete = false;
                        
                        System.out.println("Programming complete");
              
                    }
                    */

                     if(iPortAI_close() == 1) {
                        serialPort.closePort();
                     }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
    
    static String getReply(int size) {
        String rxByte;
        ArrayList<Character> buffer = new ArrayList<>();
        while(true){
            try {
                if(serialPort.getInputBufferBytesCount() > 0) {
                    rxByte = serialPort.readString(1);//Read all available from serial port and add it to buffer
       
                    if(!rxByte.equals("\n") && !rxByte.equals("\r") && !rxByte.equals("\t") && !rxByte.equals("*") && !rxByte.equals("")) {
                        buffer.add(rxByte.charAt(0));
                        if(buffer.size() == size) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        System.out.print(buffer);
        return getStringRepresentation(buffer);
    }
    
    static int iPortAI_open() {
        System.out.println("Opening i2c Port");
        try {
            serialPort.writeBytes("/O\r".getBytes());//Write data to port
        } catch (Exception e) {
            System.out.println(e);
        }

        if(getReply(4).equals("/OCC")) {
            System.out.println("i2c Port Open");
            return 1;
        } else {
            return 0;
        }
    }
    
    static int iPortAI_close() {
        System.out.println("Closing i2c Port");
        try {
            serialPort.writeBytes("/C\r".getBytes());//Write data to port
        } catch (Exception e) {
            System.out.println(e);
        }

        if(getReply(4).equals("/CCC")) {
            System.out.println("i2c Port Closed");
            return 1;
        } else {
            return 0;
        }
    }
    
    static int iPortAI_reset() {
        try {
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
                        return 1;
                    }
                }
            }
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
        return 0;
    }
    
    static int iPortAI_setAddress(String address) {
        try {
            serialPort.writeBytes(("/D"+address+"\r").getBytes());//Write data to port

            //check to make sure iPort is up and running
            while(true) {
                if(serialPort.getInputBufferBytesCount() > 0) {
                    if(serialPort.readString().replace("\n", "").replace("\r", "").equals("*")) {
                        i2cAddrSet = true;
                        System.out.println("i2c Slave Address Set");
                        return 1;
                    }
                }
            }
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
        return 0;
    }
    
    static int adm_erase_eeprom() {
        System.out.println("Erasing EEPROM");
        try {
            //Enable erasure
            serialPort.writeBytes("/t~90~08\r".getBytes());//Write data to port
            String reply = getReply(4);
            if(reply.equals("/MTC")) {   
                double percentComplete = 0;
                System.out.println(Math.round(percentComplete)+"%");
                for(int addr = 0xf800; addr <= 0xf9ff; addr += 0x20) {
                    percentComplete += 6.25;
                    String address = Integer.toHexString(addr);
                    String lowByte = address.substring(0,2);
                    String highByte = address.substring(2,4);

                    serialPort.writeBytes(("/t~"+lowByte+"~"+highByte+"\r").getBytes());//Write data to port
                    if(getReply(4).equals("/MTC")) {
                        serialPort.writeBytes("/t~fe\r".getBytes());//Write data to port
                        if(getReply(4).equals("/MTC")) {
                            try {
                                Thread.sleep(20);
                            } catch(InterruptedException e) {
                                System.out.println(e);
                            }
                        }
                    }
                    System.out.println(Math.round(percentComplete)+"%");
                }
            } else if(reply.equals("/I85")) {
                System.out.println("i2c Bus Time-out Detected");
                return 0;
            }
        } catch(SerialPortException ex) {
            System.out.println(ex);
        }
        return 1;
    }
    
    static void adm_program_eeprom() {
        System.out.println("Programming EEPROM");
        double percentComplete = 0;
        System.out.println(Math.round(percentComplete)+"%");
        int count = 0;
        for(int addr = 0xf800; addr < 0xf8df; addr += 0x01) {
            String address = Integer.toHexString(addr);
            String lowByte = address.substring(0,2);
            String highByte = address.substring(2,4);

            try {
                serialPort.writeBytes(("/t~"+lowByte+"~"+highByte+"~"+data.get(count)+"\r").getBytes());//Write data to port
                if(getReply(4).equals("/MTC")) {            
                    if(count % 32 == 0) {
                        percentComplete += 12.5;
                        System.out.println(Math.round(percentComplete)+"%");
                    }
                }
            } catch (SerialPortException ex) {
                System.out.println(ex);
            }
            count++;
        }
        percentComplete += 12.5;
        System.out.println(Math.round(percentComplete)+"%");
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
