/**
 * Implementation for load balancer transaction
 * @author Jialiang Li
 * number a1700210
 */

package com.aggregation.app;
// IO dependency
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
// net dependency
import java.net.Socket;
// reids dependency
import redis.clients.jedis.Tuple;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
// util dependency
import java.util.Collection;

/**
 * Define how load balancer response to the coming clients
 * -- giveResponse() : how to send response back to the clients
 * -- reqFromClient() : generate the response content
 */
public class balancerResponse {
    /** necessary data members */
    private PrintWriter outer;
    private BufferedReader inner;
    private Jedis redisNode;
    private JedisPool redisReturn;
    /**
     * Constructor
     * @param comeInSoc -- bind with the client socket
     * @param redisR -- retrive best server port num from distributed database
     * @exception Exception
     */
    public balancerResponse (Socket comeInSoc, JedisPool redisR)throws Exception{
        this.outer = new PrintWriter(comeInSoc.getOutputStream(), true);
        this.inner = new BufferedReader(new InputStreamReader(comeInSoc.getInputStream()));
        this.redisReturn = redisR;
        this.redisNode = redisR.getResource();
    }
  
    /**
     * Generate the response content
     * -- ATTENTION: to make things easier, make the server high-availiability, I just tried to
     * make the communication between load balancer and client as easy as possible. It is not a 
     * standard communication protocol, but could give the best performance in this assignment.
     * It is definitely not secure enough, maybe, in the future, I will put more effort in how to
     * design a secure and high-efficient protocol.
     */
    private void reqFromClient() {
        // Retrive the port number we need
        int chosenPort = Integer.parseInt(redisNode.zrangeByScoreWithScores("overload", "-inf", "+inf", 0, 1)
                                                                                            .iterator()
                                                                                            .next()
                                                                                            .getElement());
        // Generate the content of the response
        outer.print(chosenPort);
        outer.print('\u0000');    // finish signal
        outer.flush();
        redisReturn.returnResource(redisNode);
    }
    /**
     * -- Design how the balancer response to the request. As I said before, make things as
     * easy as possible. You can check the specific reason on the comments of requFromClient()
     * 1. read requests
     * 2. analyse requests
     * 3. send response
     * @exception Exception
     */
    public void giveResponse() throws Exception{
        /** necessary data memebr */
        int buf = 0;
        String getCommand = "";
        /** Read the coming request (as simple as possible here)*/
        while ((buf = inner.read())!=-1 && buf != '\u0000'){
            //System.out.print((char)buf);
            getCommand+=(char)buf;
        }
        // Analyze header
        String header = getCommand.split("\r\n")[0];
        // Send response
        if (header.equals("!!!")){
            reqFromClient();
        }
    }
}