/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iPortAI;

import java.io.UnsupportedEncodingException;
import jssc.SerialPortList;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author ldixon
 */
public class Adm1060_old {
    static public SerialPort serialPort;
    static public int iPortReady = 0;
    static public int i2cAddrSet = 0;
    static public int i2cPortOpen = 0;
    static public int i2cReadComplete = 0;
    static public int dataReady = 0;
    static public int has_written_to_eeprom = 0;
    static public int has_written_to_ram = 0;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //get list of serial ports
        String[] portNames = SerialPortList.getPortNames();
        for(int i = 0; i < 1; i++) {
            System.out.println("Checking Port " + portNames[i] + ":");
            serialPort = new SerialPort(portNames[i]);
            try {
                serialPort.openPort();//Open serial port
                serialPort.setParams(SerialPort.BAUDRATE_19200, 
                                     SerialPort.DATABITS_8,
                                     SerialPort.STOPBITS_1,
                                     SerialPort.PARITY_NONE);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
                
                //check to see iPort/AI is attached to current serial port
                serialPort.writeBytes("//\r".getBytes());//Write data to port
                
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                
                String buffer = serialPort.readString();
                System.out.println(buffer);
                
                System.out.println("iPort/AI Ready");
                iPortReady = 1;
                serialPort.writeBytes("/da8\r".getBytes());//Write data to port
               try {
                            Thread.sleep(100);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                
                System.out.println("i2c Slave Address Set");
                i2cAddrSet = 1;
                serialPort.writeBytes("/O\r".getBytes());//Write data to port
               try {
                            Thread.sleep(100);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }


                // EEPROM can only be written to if it has been erased
                if(has_written_to_eeprom == 0){
                    System.out.println("Erasing EEPROM");
                    serialPort.writeBytes("/*t~90~08\r".getBytes());//Write data to port
                    try {
                            Thread.sleep(100);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    
  
                    for(int addr = 0xf800; addr <= 0xf9ff; addr += 0x20) {
                        
                        String address = Integer.toHexString(addr);
                        String lowByte = address.substring(0,2);
                        String highByte = address.substring(2,4);
                        System.out.println(("/*t~"+lowByte+"~"+highByte+"\r"));
                        serialPort.writeBytes(("/*t~"+lowByte+"~"+highByte+"\r").getBytes());//Write data to port
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        
                        serialPort.writeBytes("/*t~fe\r".getBytes());//Write data to port

                        try {
                            Thread.sleep(25);
                        } catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        System.out.println(serialPort.readString());
                    }
                    
                    System.out.println("Writing EEPROM");
                }
                serialPort.closePort();//Close serial port
            }
            catch (SerialPortException ex) {
                System.out.println(ex);
            }
        }
    }
    
    static public String byteToString(byte[] buffer) {
        String byte_string;
        try {
            byte_string = new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // this should never happen because "UTF-8" is hard-coded.
            throw new IllegalStateException(e);
        }
        return byte_string;
    }
}
