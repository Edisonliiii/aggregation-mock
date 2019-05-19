/**
 * Implementation for AggregationServer
 * @author Jialiang Li
 * @number a1700210
 */


package com.aggregation.app;
// IO dependency
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
// Net dependency
import java.net.ServerSocket;
import java.net.Socket;
// Util dependency
import java.util.concurrent.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
// Redis dependency
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * Aggregation server main business logic
 * My aggregation server is a pure asynchronous server.
 *  -- 1. Keep listening to the coming events (PUT/GET request).
 *  -- 2. Monitor the status of the aggregation server.
 */
public class AggregationServerImp implements AggregationServer{
    /** necessary data members */
    private final ServerSocket serverSocket;
    private final int serverPortNum;
    private final JedisPool dbRef;
    /**
     * Constructor
     * @param serverPortNum -- choose a port to run one aggregation server
     * @param dbRef         -- bind with the distributed database
     * @exception IOException
     */
    public AggregationServerImp(int serverPortNum, JedisPool dbRef) throws IOException{
        this.serverPortNum = serverPortNum;
        this.dbRef = dbRef;
        this.serverSocket = new ServerSocket(this.serverPortNum);
    }
    /**
     * Fire the asynchronous server
     * Monitor the status of the server as the server is running
     * @exception IOException
     * @exception InterruptedException
     */
    @Override
    public void startServer() throws IOException, InterruptedException{
        System.out.println("------------------Server is running at " +
            serverPortNum + "....");

        // thread pool (for accept coming events without dead lock or resource confliction)
        ExecutorService executorPool = Executors.newCachedThreadPool();
        ThreadPoolExecutor tmp = (ThreadPoolExecutor)executorPool;

        // Keep listening ..... (async)
        CompletableFuture<Void> keepListening = CompletableFuture.runAsync(()->{
            try{
                while(true){
                    System.out.println("Start listing on " + serverPortNum + "....");
                    final Socket client = serverSocket.accept();
                    System.out.println(client.getInetAddress() + ":" +
                        client.getPort() + " is connected!");
                    // Fire a new thread for coming client
                    executorPool.execute(new Runnable(){
                        @Override
                        public void run(){
                            try{
                                new Responser(client, dbRef).startResponser();                            
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        
        // Monitor, supervising the status of the current thread pool.
        while(true){
            Jedis recorder = dbRef.getResource();
            //Thread.sleep(1000);
            recorder.zadd("overload", (double)tmp.getActiveCount(), String.valueOf(serverPortNum));
            //System.out.println(serverPortNum + ": There are " + tmp.getActiveCount() + " active threads are running....");
            dbRef.returnResource(recorder);
        }
    }
    
    /**
     * This is the most important test in the whole system, which is the overall testing!!!!!
     *  1. Start the redis server at port 9000
     *  2. Start the redis client and connect with redis server
     *  3. Start the load balancer in an asynchronous method and keep listenning the coming events
     *  4. Start the aggregation server cluster based on the /server/resource/config.properties
     * --Each aggregation server is able to receive as many clients as it could, no limited executor
     * pool was chosen to use for high-availability purpose. Definitely, the security of aggregation
     * server is more important in the real world, but based on this assignment, I just chose non-limited
     * pool to accept as many clients as I could, which is also easier for testing extreme cases. 
     */
    public static void main(String[] args) throws Exception {

        /** Connect with Jedis server running at 9000 */
        JedisPool dbPool = new JedisPool((new JedisPoolConfig()),
                                        "localhost",
                                        9000);
        /** 
         * Start the load balancer
         * Keep listening coming events at 6666
         * @exception Exception
         */
        CompletableFuture<Void> lbPath = CompletableFuture.runAsync(()->{
            try{
                loadBalancer balancer = new loadBalancer(6666, dbPool);
                balancer.runBalancer();                
            } catch (Exception e){
                e.printStackTrace();
            }

        });

        /**
         * read the config properties from /server/resource/config.properties
         */
        Properties prop = new Properties();
        InputStream configReader = new FileInputStream("./server/resource/config.properties");
        prop.load(configReader);
        int clusterSize = Integer.parseInt(prop.getProperty("AGG_CLUSTER_SIZE"));
        /**
         * Start the aggregation server cluster
         */
        ExecutorService exec = Executors.newFixedThreadPool(clusterSize);
        for(int i=0; i<clusterSize; i++)
        {
            final String bornServer = Integer.toString(i);
            exec.execute(new Runnable(){
                @Override
                public void run(){
                    try{
                        new AggregationServerImp(Integer.parseInt(prop.getProperty("AGG_SERVER_PORT_"+bornServer)),
                                                 dbPool).startServer();
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