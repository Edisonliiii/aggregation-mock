/**
 * Implementation for parser testing
 * @author Jialiang Li
 * number: a1700210
 */

package com.aggregation.app;
// util dependency
import java.util.Arrays;
import java.util.Collection;
// IO dependency
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
// junit dependency
import org.junit.Test;
import org.junit.Before;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runner.RunWith;
// Comparison
import static org.junit.Assert.assertEquals;


/**
 * Unit test for simple App.
 */
@RunWith (Parameterized.class)
public class AppTest {
    private String inputFile;
    private String outputFile;
    
    public AppTest(String inputFile, String outputFile){
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }
    /**
     * Pre-setup the parameters
     * @return parameter group that needed to be tested
     */
    @Parameterized.Parameters
    public static Collection testSet() {
        return Arrays.asList(new Object[][] {
            {"./toAgg/input1.txt", "./toAgg/output1.xml"},
            {"./toAgg/company1.txt", "./toAgg/output2.xml"},
            {"./toAgg/company2.txt", "./toAgg/output3.xml"}
        });
    }
    /**
     * Test the functionality of Text2XML
     * setup normal and error test case
     */
    @Test
    public void testNormal() throws Exception{
        System.out.println("Input File is: " + inputFile);
        Text2XML tx = new Text2XML(inputFile);
        FileInputStream fstream = new FileInputStream(inputFile);
        FileInputStream fstreamO = new FileInputStream(outputFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        BufferedReader brO = new BufferedReader(new InputStreamReader(fstreamO));
        //Text2XML tmp = new Text2XML();

        String strLineIn;
        String strLineOut;
        String tmp = "";
        String tmpO = "";
        
        // start of file handler
        tmp += tx.T2XML("SOF");
        tmp += "\n";
        //Read File Line By Line
        while ((strLineIn = br.readLine()) != null)   {
            // Print the content on the console
           tmp += tx.T2XML(strLineIn);
           tmp += "\n";
        }
        // end of file handler
        tmp += tx.T2XML("EOF");
        tmp += "\n";
        
        while ((strLineOut = brO.readLine()) != null) {
            tmpO += strLineOut;
            tmpO += "\n";
        }
        //Close the input stream
        br.close();
        brO.close();

        assertEquals(tmp, tmpO);
    }
    
    /**
     * Normal structure check
     */
    @Test
    public void testEntryCount() throws Exception{
        System.out.println("Input File is: " + inputFile);
        Text2XML tx = new Text2XML(inputFile);
        FileInputStream fstream = new FileInputStream(inputFile);
        FileInputStream fstreamO = new FileInputStream(outputFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        BufferedReader brO = new BufferedReader(new InputStreamReader(fstreamO));
        //Text2XML tmp = new Text2XML();

        String strLineIn;
        String strLineOut;
        String tmp = "";
        String tmpO = "";
        
        // start of file handler
        tmp += tx.T2XML("SOF");
        tmp += "\n";
        //Read File Line By Line
        while ((strLineIn = br.readLine()) != null)   {
            // Print the content on the console
           tmp += tx.T2XML(strLineIn);
           tmp += "\n";
        }
        // end of file handler
        tmp += tx.T2XML("EOF");
        tmp += "\n";
        
        while ((strLineOut = brO.readLine()) != null) {
            tmpO += strLineOut;
            tmpO += "\n";
        }
        //Close the input stream
        br.close();
        brO.close();
        assertEquals(tx.preCheck(), true);
    }
}