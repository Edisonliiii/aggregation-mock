/**
 * Service responser of aggregation server
 * @author Jialiang Li
 * number: a1700210
 */

package com.aggregation.app;

// IO dependency
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
// Net dependency
import java.net.Socket;
// Redis dependency
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/** 
 * This class handle all of coming events and 
 * give the corresponding appropriate response.
 * --- Handle PUT requests from content servers
 * --- Handle GET requests from clients
 */
public class Responser{
	/** necessary data members */
    private Socket newC;
    private PrintWriter out;
    private BufferedReader in;
    private String httpMethod = "";
    private String httpURI = "";
    private String httpGETResponseBody = "";
    private static boolean dataExistence = false;
    private final char EOS = '\u0000';    // Represents the end of stream
    private Jedis dbNode;
    private JedisPool dbRef;

    /**
     * Constructor
     * @param newC  -- socket
     * @param dbRef -- distributed database ref
     * @exception IOException
     */
    public Responser(Socket newC, JedisPool dbRef){
        this.newC = newC;
        this.dbRef = dbRef;
        try{
            this.out = new PrintWriter(this.newC.getOutputStream(), true);
            this.in = new BufferedReader(
                new InputStreamReader(this.newC.getInputStream()));
            this.dbNode = this.dbRef.getResource();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Close all of the streams or buffer after using them
     * Including:
     *  -- instream
     *  -- outstream
     *  -- socket
     * @exception IOException
     */
    private void closeStream(){
        try{
            in.close();
            out.close();
            newC.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    /**
     * Parse the coming request is legal or not before responsing
     * @return boolean true---legal, vice versa
     * @exception Exception
     * 
     */
    private boolean httpReqestParser() throws Exception{
        /** necessary data members (within this method)*/
        int tmpBuf = 0;
        boolean flag = false;
        String tmpContainer = "";
        String httpPUTRequestBody = "";

        /** keep reading the coming request until meet EOS */
        while ((tmpBuf = in.read()) != -1 && tmpBuf != EOS){
            System.out.print((char)tmpBuf);
            tmpContainer += (char)tmpBuf;    // record the request header+body
        }

        // get the method of http request
        httpMethod = tmpContainer.split("\r\n")[0].split(" ")[0];
        // get the path to locate the new/renew file
        httpURI = "."+tmpContainer.split("\r\n")[0].split(" ")[1];

        // Wrong request type error handling
        if (!httpMethod.equals("GET") && !httpMethod.equals("PUT")) {
            //out.print("Bad Request!");
            flag = false;
        }
        // No HTTP URI error handling
        if (httpURI == null) {
            //out.print("Illegal Request Format!");
            flag = false;
        }
        // PUT -- legal case
        if (httpMethod.equals("PUT") && httpURI != null) {
            // get the http request body
            httpPUTRequestBody = tmpContainer.split("\r\n\r\n")[1];
            // if the key pair does exist in DB, then renew it!
            if (dbNode.get(httpURI) != null) {
                dbNode.set(httpURI, httpPUTRequestBody, "XX", "EX", 15);    // each key value only live for 15 secs
                dataExistence = true;
            }else {// if the key pair does NOT exist in DB, then create it!
                dbNode.set(httpURI, httpPUTRequestBody, "NX", "EX", 15);    // each key value only live for 15 secs
            }
            flag = true;
        }
        // GET -- legal case
        if (httpMethod.equals("GET") && httpURI != null) {
            // if the key pair does exist in DB, then renew it!
            if (dbNode.get(httpURI) != null) {
                flag = true;
                dataExistence = true;
                httpGETResponseBody = dbNode.get(httpURI);
                dbNode.set(httpURI, httpGETResponseBody, "XX", "EX", 15);    // reset the value
            }else {// if the key pair does NOT exist in DB, then create it!
                flag = false;
            }
        }
        
        // return the DB interface back to the pool in the master server
        // Dont forget the close it
        dbRef.returnResource(dbNode);

        return flag;
    }

    /**Generate GETResponseHeaderOK */
    private void GETResponseHeaderOK(){
        Date currentTime = new Date();
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Date: " + currentTime.toString() + "\r\n");
        out.print("Server: Aggregation/1.0" + "\r\n");
        out.print("Date: " + currentTime.toString() + "\r\n");
        //out.print("Content-Length: "+new File(httpURI).length() + "\r\n");
    }

    /** Put response for content server (201) */
    private void PUTResponseHeaderCreated(){
        Date currentTime = new Date();
        out.print("HTTP/1.1 201 Created\r\n");
        out.print("Date: " + currentTime.toString() + "\r\n");
        out.print("Server: Aggregation/1.0" + "\r\n");
        out.print("Content-Location: " + httpURI + "\r\n");
        out.print("\r\n");
        out.flush();
    }

    /** Put response for content server (200) */
    private void PUTResponseHeaderOK() {
        Date currentTime = new Date();
        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Date: " + currentTime.toString() + "\r\n");
        out.print("Server: Aggregation/1.0" + "\r\n");
        out.print("Content-Location: " + httpURI + "\r\n");
        out.print("\r\n");
        out.flush();
    }

    /** Wrong request type response */
    private void badRequest(){
        Date currentTime = new Date();
        out.print("HTTP/1.1 400 Bad request\r\n");
        out.print("\r\n");
        out.flush();
    }

    /** 
     * Make response to the client
     * Use all previous content generator make the complete response
     * @exception Exception
     */
    private void makeResponse() throws Exception{
        // Response for GET request
        if(httpMethod.equals("GET")){
            GETResponseHeaderOK();
            out.print("\r\n");
            System.out.println(httpGETResponseBody);
            new XML2Text(httpGETResponseBody, out).XML2T();
            out.flush();
        }
        // Response for PUT request when data does not exist yet
        if(httpMethod.equals("PUT") && dataExistence == false){
            PUTResponseHeaderCreated();
        }
        // Response for PUT request when data has already existed
        if(httpMethod.equals("PUT") && dataExistence == true){
            PUTResponseHeaderOK();
        }
    }

    /**
     * API for calling
     * Uniform all previous methods and ready for calling
     * @exception Exception
     */
    public void startResponser()throws Exception{
        try{
            System.out.println("------------------" + Thread.currentThread().getName() + " is catched");
            // Legal request, then response
            if(httpReqestParser()){
                makeResponse();
            }else{// Illegal request, then response
                badRequest();
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally{
            closeStream();
            //Thread.sleep(3000);    // sleep for a while after finishing, easy for testing
            System.out.println("------------------" + Thread.currentThread().getName() + " is finished");
        }
    }
}