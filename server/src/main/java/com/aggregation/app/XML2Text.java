/**
 * XML2Text parser
 * @author Jialiang Li
 * number: a1700210
 */

package com.aggregation.app;
// IO dependency
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
// XML dependency
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
// dom parser dependency
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/** The object class of XML2Text operation */
public class XML2Text {
   /** necessary data members */
   private final String content_str;
   private PrintWriter out;
   /** 
    * constructor
    * @param content_str --- the content which need to be parsed
    * @param out         --- reference of outstream
    */
   public XML2Text(String content_str, PrintWriter out) {
      this.content_str = content_str;
      this.out = out;
   }
   /** 
    * directly send out the parsed content
    * @exception ParserConfigurationException
    * @exception SAXException
    * @exception IOException
    */
   public void XML2T() throws
            ParserConfigurationException, SAXException, IOException {
      Document document = DocumentBuilderFactory.newInstance()
                                                .newDocumentBuilder()
                                                .parse(new InputSource(new StringReader(content_str)));

      NodeList nList = document.getElementsByTagName("*");

      // parse line by line
      for(int i=0; i<nList.getLength(); i++){
        Node nNode = nList.item(i);
        if(nNode.getNodeType() == Node.ELEMENT_NODE) {
          String tagName = nNode.getNodeName();
          String tagVal  = "";
          // setup tag name
          if (tagName.equals("feed")   ||
              tagName.equals("author") ||
              tagName.equals("email")){continue;}
          else if (tagName.equals("name")){ tagName = "author:";}
          else if (tagName.equals("entry")){ tagName = "entry";}
          else {tagName = tagName+":";}
          // setup tag value
          if (tagName.equals("link:")) { tagVal = nNode.getAttributes().getNamedItem("href").getNodeValue();}
          else{tagVal  = nNode.getChildNodes().item(0)
                                              .getNodeValue()
                                              .trim();}
          out.println(tagName + tagVal);
        }
      }
   }
}