/**
 * Implementation for GETClient interface
 * @author Jialiang Li
 */

package com.aggregation.app;

// IO dependency
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
// Net dependency
import java.net.UnknownHostException;
import java.net.URL;
import java.net.Socket;
// Util dependency
import java.util.concurrent.*;
import java.util.Properties;

//import parser.Text2XML;

public class ContentServerImp implements ContentServer{
    // Data member
    private final URL URLNAME;
    private final int PORTNUM;
    private final String HOSTNAME;
    private final String sourceFile;      // Which file you wanna put

    private final char EOS = '\u0000';    // Represents the end of stream
    private final BufferedReader readIn;  // Input stream
    private final PrintWriter writeOut;   // Output stream
    private final Socket socket;          // Socket

    /**
     * Constructor
     * @param URLNAME
     * @param PORTNUM
     * @param sourceFile
     * Initilize all data members
     */
    public ContentServerImp(String URLNAME, int PORTNUM, String sourceFile) throws Exception{
            this.URLNAME = new URL(URLNAME);
            this.PORTNUM = PORTNUM;
            this.HOSTNAME = this.URLNAME.getHost();
            this.socket = new Socket(this.HOSTNAME, this.PORTNUM);
            this.writeOut = new PrintWriter(this.socket.getOutputStream(), true);
            this.readIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.sourceFile = sourceFile;
    }
    
    /** 
     * Close all of the streams or buffer after using them
     * Grabage collector
     */
    private void closeStream(){
        try{
            readIn.close();
            if (writeOut != null){
                writeOut.close();
            }
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Send HTTP PUT request to the server
     */
    @Override
    public void sendPUTRequest(){
        // Open Connection on specific HOSTNAME and port
        try{
            int responseContent;
            // HTTP PUT request based on RFC
            writeOut.print("PUT " + URLNAME.getPath() + " HTTP/1.1\r\n");
            writeOut.print("HOST: " + HOSTNAME + "\r\n");
            writeOut.print("User-Agent: Command-Line\r\n");
            writeOut.print("\r\n");
            new Text2XML(sourceFile).startT2XML(writeOut);    // parse and transfer
            writeOut.print(EOS);
            writeOut.flush();
            // Listen to the response from sever
            while ((responseContent = readIn.read()) != -1 && responseContent != EOS){
                System.out.print((char)responseContent);
            }
            //writeOut.close();   // Have to close here, tho, its unnecessary
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Test case for Content server
     * 
     * --Plz check the config file in the /content/resource/config.properties and
     * you can setup any parameter based on the rules
     *
     * --In this test case, 3 content server was fired in parallel. They send the
     * standard HTTP request to the aggregation server. The overall process is:
     *
     * 1. Content servers send PUT request to the aggregation server cluster
     * 2. Aggregation server give the corresponding appropriate responses back to the content server
     * 3. Aggregation server clusters make synchronization on the distributed database
     * 
     * Time delay or timeout case and disconnection case are both simulated by thread.sleep() and perfectly
     * handled.
     */
    public static void main(String[] args) throws Exception{
        /** 
         * Read config properties
         */
        Properties prop = new Properties();
        InputStream configReader = new FileInputStream("./content/resource/config.properties");
        prop.load(configReader);
        // set cluster size
        int clusterSize = Integer.parseInt(prop.getProperty("NUMBER_OF_CONTENT_SERVER"));
        /** 
         * content server manager (thread pool)
         * Fire multiple content server at the same time
         * The time delay and timeout have beem simulated by thread.sleep()
         */
        ExecutorService exec = Executors.newFixedThreadPool(clusterSize);
        for(int i=0; i<clusterSize; i++)
        {
            final String tmp = Integer.toString(i);
            exec.execute(new Runnable(){
                @Override
                public void run(){
                    try{
                        new ContentServerImp(prop.getProperty("SAVE_AS_NAME_"+tmp),
                                            Integer.parseInt(prop.getProperty("TO_WHICH_PORT_"+tmp)),
                                            prop.getProperty("SOURCE_FILE_"+tmp)).sendPUTRequest();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
        /**
         * Stop accepting coming event and terminate the pool safely
         */
        exec.shutdown();

        try{
            if(!exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
            {
                exec.shutdownNow();
            }
        }catch (InterruptedException e){
            exec.shutdownNow();
        }
    }
}