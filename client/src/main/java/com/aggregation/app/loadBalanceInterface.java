
package com.aggregation.app;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class loadBalanceInterface {
	public int sendReqToLB() throws Exception{
        int fromLB = 0;
        String getPort = "";
        Socket toLB = new Socket("localhost", 6666);
        PrintWriter out2LB = new PrintWriter(toLB.getOutputStream(), true);
        BufferedReader in2LB = new BufferedReader(new InputStreamReader(toLB.getInputStream()));

        out2LB.print("!!!\r\n");
        out2LB.print("\r\n");
        out2LB.print('\u0000');
        out2LB.flush();

        while ((fromLB = in2LB.read())!=-1 && fromLB != '\u0000'){
            //System.out.print((char)fromLB);
            getPort+=(char)fromLB;
        }
        //Thread.sleep(1000);
        System.out.println("Waiting..................................");
        out2LB.close();
        in2LB.close();
        toLB.close();
        System.out.println("Will go to the freest port: " + getPort);
        System.out.println("---------- Finish LB process ! ----------");

        return Integer.parseInt(getPort);
    }
}