/**
 * Implementation for load balancer server
 * @author Jialiang Li
 * number a1700210
 */

package com.aggregation.app;
// net dependency
import java.net.ServerSocket;
import java.net.Socket;
// io dependency
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
// redis dependency
import redis.clients.jedis.JedisPool;
// util dependency
import java.util.concurrent.*;


/**
 * Object of load balancer
 * Features:
 * -- Always choose the freest server in the cluster
 * -- Auto-checking the number of task running on each server in the cluster
 * -- Send back the port number of the freest server in the cluster
 */
public class loadBalancer {
    /** necessary data members */
    private final ServerSocket balancerSocket;
    private final int portNum;
    private JedisPool redisRef;
    /**
     * constructor
     * @param portNum  --- which port load balancer is gonna run at
     * @param redisRef --- bind with the distributed database
     * @exception Exception
     */
    public loadBalancer(int portNum, JedisPool redisRef) throws Exception{
        this.portNum = portNum;
        this.balancerSocket = new ServerSocket(portNum);
        this.redisRef = redisRef;
    }
    /**
     * Fire the balancer and keep listening the coming events
     * This is a fully asynchronous and non-blocking load balancer
     * So it will work with every clients very smoothly
     */
    public void runBalancer(){
        ExecutorService balancerPool = Executors.newCachedThreadPool();
        try{
            while(true) {
                final Socket newOne = balancerSocket.accept();
                final String comingAddress = newOne.getInetAddress().toString();
                final int comingPort = newOne.getPort();
                System.out.println(comingAddress);
                System.out.println(comingPort);
                // keep listening ....
                balancerPool.execute(
                    new Runnable(){
                        @Override
                        public void run(){
                            try{
                                new balancerResponse(newOne, redisRef).giveResponse();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        /**
         * Stop accepting coming event and terminate the pool safely
         */
        balancerPool.shutdown();

        try{
            if(!balancerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
            {
                balancerPool.shutdownNow();
            }
        }catch (InterruptedException e){
            balancerPool.shutdownNow();
        }
    }
}