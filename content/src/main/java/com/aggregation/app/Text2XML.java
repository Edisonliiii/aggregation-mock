/**
 * Implementation for GETClient interface
 * @author Jialiang Li
 */

package com.aggregation.app;
// IO dependencies needed
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;
// Util dependencies needed
import java.util.Scanner;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

/**
 * Text2XML parser
 * Parse plain text(.txt) file to ATOM XML format
 * To release the pressure of the content server,
 * serving on content server side
 * NO THIRD PARTY DEPENDENCY
 */
public class Text2XML {
    /** necessary data members */
    private final int NUMOFPARTS;
    private String INDENTATION;
    private final String F_TAG_L;
    private final String F_TAG_R;
    private final String BACKTAG;
    private final String sourceFile;
    private List<String> tagSet = new ArrayList<String>();
    private int flag;

    /** Constructor */
    public Text2XML(String sourceFile){
        NUMOFPARTS  = 2;
        INDENTATION = "";
        F_TAG_L     = "<";
        F_TAG_R     = ">";
        BACKTAG     = "</";
        flag        = 0;
        this.sourceFile = sourceFile;
    }

    /**
     * Build the header tag
     * @param str --- content of the tag
     * @return header tag itself
     */
    private String headerMaker(String str){
        return F_TAG_L + str + F_TAG_R;
    }

    /**
     * Build the tail tag
     * @param str --- content of the tag
     * @return tail tag itself
     */
    private String tailMaker(String str){
        return BACKTAG + str + F_TAG_R;
    }

    /** 
     * The main logic 
     * Handle the input line by line.
     * Smoothly connect with the socket stream which means
     * my implementation could boost the parsing process as
     * fast as it could
     * @param str --- one line
     * @return line after parsing
     */
    public String T2XML(String str){

        String outLine="";
        String[] arr = str.split(":", NUMOFPARTS);
        final String HEADER = headerMaker(arr[0]);
        final String TAIL   = tailMaker(arr[0]);

        /** build the final tag of the whole file */
        if (str.equals("EOF")){
            INDENTATION = INDENTATION.substring(0, INDENTATION.length()/2);
            outLine = INDENTATION + tailMaker("entry");
            INDENTATION = "";
            outLine += "\n" + tailMaker("feed");
            return outLine;
        }

        /** build the starting tag of the whole file */
        if (str.equals("SOF")){
            outLine = "<?xml version='1.0' encoding='iso-8859-1' ?>" + "\n"
                      + "<feed xml:lang=\"en-US\" xmlns=\"http://www.w3.org/2005/Atom\">";
            INDENTATION = "        ";
            return outLine;
        }
        
        /** 
         * Special cases handler
         * tag --- author
         * tag --- link
         * tag --- entry
         */
        if (arr.length > 1){
            // normal        
            outLine = INDENTATION+HEADER+arr[1]+TAIL;
            // author tag handler
            if (arr[0].equals("author")){
                outLine = INDENTATION + headerMaker("author") + "\n" + INDENTATION 
                        + INDENTATION + headerMaker("name") + arr[1] 
                        + tailMaker("name") + "\n" + INDENTATION+tailMaker("author");
            }
            if (arr[0].equals("link")) {
                outLine = INDENTATION + "<link href=\""+ arr[1] +"\" rel=\"self\"/>";
            }
            tagSet.add(arr[0]);
        }else if (arr.length == 1 && arr[0].equals("entry")){
            // entry tag handler
            if (flag == 0){
                outLine = INDENTATION+headerMaker("entry");
                flag++;
            }else if (flag == 1){
                INDENTATION = INDENTATION.substring(0, INDENTATION.length()/2);
                outLine = INDENTATION + tailMaker("entry") + "\n" 
                          + INDENTATION + headerMaker("entry");
            }
            INDENTATION += INDENTATION;
            tagSet.add(arr[0]);
        }else{
            outLine = "Bad Format!";
        }
        return outLine;
    }

    /** 
     * Check if the input is legal or not
     * @return legal -- true
     *         illegal -- false
     */
    public boolean preCheck() throws Exception{
        Map<String, Integer> counter = new HashMap<String, Integer>();
        for(String i: tagSet) {
            Integer n = counter.get(i);
            counter.put(i, (n==null) ? 1 : n+1);
        }

        int numEntry = counter.get("entry");
        int numTitle = counter.get("title");
        int numLink = counter.get("link");
        int numId = counter.get("id");

        if (numEntry+1 == numTitle &&
            numEntry+1 == numLink &&
            numEntry+1 == numId){
            return true;
        }

        return false;
    }


    /** 
     * send the input to the corresponding stream
     */
    public void startT2XML(PrintWriter out) throws IOException {
        FileInputStream fstream = new FileInputStream(sourceFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        //Text2XML tmp = new Text2XML();

        String strLine;
        
        // start of file handler
        out.println(T2XML("SOF"));
        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {
            // Print the content on the console
            out.println (T2XML(strLine));
        }
        // end of file handler
        out.println(T2XML("EOF"));

        //Close the input stream
        br.close();
    }
}