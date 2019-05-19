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

/**
 * Client Class Implementation
 * To save the resource, all connection is temp.
 */
public class GETClientImp implements GETClient{
    // Data member
    private final URL URLNAME;
    private final int PORTNUM;
    private final String HOSTNAME;
    private final char EOS = '\u0000';    // Represents the end of stream
    private BufferedReader readIn;
    private PrintWriter writeOut;
    private Socket socket;

    /**
     * Constructor for Client Instance
     * Initialize all datamember needed for an instance
     * @param URLNAME
     * @param PORTNUM
     */
    public GETClientImp(String URLNAME, int PORTNUM) throws Exception{
            this.URLNAME = new URL(URLNAME);
            this.PORTNUM = PORTNUM;
            this.HOSTNAME = this.URLNAME.getHost();
            this.socket = new Socket(this.HOSTNAME, this.PORTNUM);
            this.writeOut = new PrintWriter(this.socket.getOutputStream(), true);
            this.readIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }
    
    /**
     * Close all of the opening stream when useless
     * @return void
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
     * Send HTTP GET request to the server 
     * To make the test convient, all request
     * was perfectly hard-coded based on RFC standard
     * 
     * There is actually no difference as inputting by hand
     * To make the test process as simple as possible, just use prepared.
     * @return void
     */
    @Override
    public void sendGETRequest(){
        // Open Connection on specific HOSTNAME and port
        try{
            // State writestream to this URL and send request to the server
            // HTTP GET request based on RFC
            writeOut.print("GET " + URLNAME.getPath() + " HTTP/1.1\r\n");
            writeOut.print("HOST: " + HOSTNAME + "\r\n");
            writeOut.print("User-Agent: Command-Line\r\n");
            writeOut.print("Accept: text/html\r\n");
            writeOut.print("Accept-Language: en-US\r\n");
            writeOut.print("\r\n");
            writeOut.print(EOS);
            writeOut.flush();
            // Listen to the response from sever
            int responseContent;
            while ((responseContent = readIn.read()) != -1 && responseContent != EOS){
                System.out.print((char)responseContent);
            }
            //writeOut.close();   // Have to close here, tho, its unnecessary
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /** 
     * Test Case
     *
     * --Please check the config file in the ./client/resource and you can set any
     * parameters you want
     * 
     * --In this test case, 3 clients were fired in parallel. To get the resource
     * from aggregation server. The overall process could be expressed as:
     * 
     * 1. Client send a special reuqest to load balancer to get the port of the freest aggregation server
     * 2. Load balancer sends the port number back
     * 3. Client sends the standard HTTP request to the freest aggregation server.
     * 4. The aggregation server sends the response back to the client.
     */
    public static void main(String[] args) throws Exception{
        /** 
         * Read config properties
         */
        Properties prop = new Properties();
        InputStream configReader = new FileInputStream("./client/resource/config.properties");
        prop.load(configReader);
        int clusterSize = Integer.parseInt(prop.getProperty("NUMBER_OF_CLIENT"));
        /** 
         * client manager (thread pool)
         * Fire multiple client at the same time
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
                        new GETClientImp(prop.getProperty("GET_WHICH_FILE_"+tmp),
                                        new loadBalanceInterface().sendReqToLB()).sendGETRequest();
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
        // Terminate the pool safely
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