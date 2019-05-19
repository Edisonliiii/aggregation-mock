/**
 * Implementation for lamport clock
 * @author Jialiang Li
 */

package com.aggregation.app;

public class LamportOnServer {
	/** static variable as a counter*/
    private static int lamportTime;
    /** Constructor */
    public LamportOnServer(){
        lamportTime = 0;
    }
    /** 
     * Compare the coming event lamport time with the local lamport time
     * If coming lamport time < local lamport time; then new lamport = local + 1
     * else; new lamport = coming lamport time + 1
     * @param comingTime
     * @param lamportTime
     * @return void
     */
    public void acceptMessage (int comingTime){
        lamportTime = comingTime < lamportTime ? lamportTime+1 : comingTime+1;
    }
    /**
     * Send message out
     * @param this.lamportTime
     * @return void
     */
    public void sendMessage (){
        ++lamportTime;
    }
    /**
     * Local event handler
     * @param this.lamportTime
     * @return void
     */
    public void localEvent() {
        ++lamportTime;
    }
    /**
     * Get current lamport logic time
     * @param this.lamportTime
     * @return this.lamportTime
     */
    public int getLamportTime (){
        return lamportTime;
    }
}