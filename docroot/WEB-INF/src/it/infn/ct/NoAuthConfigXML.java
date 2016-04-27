/**************************************************************************
 * Copyright (c) 2011:
 * Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     @author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(INFN)
 *     ****************************************************************************/
package it.infn.ct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import it.infn.ct.AppLogger;

/**
 * Class that manages the Guacamole' noauth-config.xml file content
 */
public class NoAuthConfigXML {

  private      String NoAuthConfigXML_file = "";
  static final String LS = System.getProperty("line.separator");
  Document     NoAuthConfigDocument = null;
  AppLogger    _log=null;

  /**
   * Constructor that reads in memory the whole Guacamole noauth-config XML file
   */
  public NoAuthConfigXML(AppLogger _log, String NoAuthConfigXML_file) {
    this._log=_log;
    _log.info("Logger linked to NoAuthConfigXML_file");
    this.NoAuthConfigXML_file = NoAuthConfigXML_file;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream inputStream = new FileInputStream(this.NoAuthConfigXML_file);
      NoAuthConfigDocument = builder.parse(inputStream);
    } catch(Exception e) {
        _log.error(LS+"Exception!"
                  +LS+"--------------------------"
                  +LS+e.toString());
    }
  } // NoAuthConfigXML(AppLogger _log, Strng NoAuthConfigXML_file) 

  /**
   * Builds a string containing values of the Guacamole noauth-config XML file
   */
  public String dump() {
      String xmlDump="";
      if(NoAuthConfigDocument != null) {
        List<NoAuthConfig> nacList = new ArrayList<NoAuthConfig>();
        NodeList nodeList = NoAuthConfigDocument.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
          Node node = nodeList.item(i);
          if (node instanceof Element) {
            NoAuthConfig nac = new NoAuthConfig();
            nac.name = node.getAttributes().getNamedItem("name").getNodeValue();
            nac.protocol = node.getAttributes().getNamedItem("protocol").getNodeValue();
  
            NodeList childNodes = node.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
              Node cNode = childNodes.item(j);
  
              //Identifying the child tag of employee encountered. 
              if (cNode instanceof Element) {
                String attr_name = cNode.getAttributes().getNamedItem("name").getNodeValue();
                String attr_value= cNode.getAttributes().getNamedItem("value").getNodeValue();
                if(attr_name.equals("hostname"))
                    nac.hostname = attr_value;
                else if(attr_name.equals("port"))
                    nac.port = attr_value;
                else if(attr_name.equals("username"))
                    nac.username = attr_value;
                else if(attr_name.equals("password"))
                    nac.password = attr_value;
                else if(attr_name.equals("workgroup"))
                    nac.workgroup = attr_value;
              } // if cNode ...
            } // childNodes loop
            nacList.add(nac);
          } // if Element
      } // Nodes loop
      // Generates output
      for (NoAuthConfig nac : nacList) {
        xmlDump=xmlDump + nac.toString() + LS;
      } // nacList loop
    } // if NoAuthConfigXML != null
    return xmlDump;
  } // dump()

  /**
   * Return a Guacamole noauth-config.xml node corresponding to the given name
   */
  public Node GetConfigNode(String name) {
    Node retElement=null;
    if (name != null && !name.equals("") && NoAuthConfigDocument != null) {
        List<NoAuthConfig> nacList = new ArrayList<NoAuthConfig>();
        NodeList nodeList = NoAuthConfigDocument.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
          Node node = nodeList.item(i);
          if (node instanceof Element) {
            String nodeName = node.getAttributes().getNamedItem("name").getNodeValue();
            if(nodeName.equals(name)) {
                retElement = node;
                break;
            } // name found!
          } // node
        } // for i
    }
    return retElement;
  }
  
  /**
   * Add a XML configuration node to Guacamole noauth-config.xml
   */
  public void AddNewConfig(String name
                          ,String protocol
                          ,String hostname
                          ,String port
                          ,String username  
                          ,String password
                          ,String workgroup) {
    if(NoAuthConfigDocument!=null) {
        try {
          // New configuration node creation
          if(       name != null 
             && protocol != null 
             && !name.equals("") 
             && !protocol.equals("")) {
              if(GetConfigNode(name) == null) {
                Element newConfig = NoAuthConfigDocument.createElement("config");
                NoAuthConfigDocument.getDocumentElement().appendChild(newConfig);
                newConfig.setAttribute("name",name);
                newConfig.setAttribute("protocol",protocol);
                // hostname
                if(hostname != null && !name.equals("")) {
                    Element newConfigParam_hostname = NoAuthConfigDocument.createElement("param");
                    newConfigParam_hostname.setAttribute("name","hostname");
                    newConfigParam_hostname.setAttribute("value",hostname);
                    newConfig.appendChild(newConfigParam_hostname);
                }
                // port
                if(port != null && !port.equals("")) {
                      Element newConfigParam_port = NoAuthConfigDocument.createElement("param");
                      newConfigParam_port.setAttribute("name","port");
                      newConfigParam_port.setAttribute("value",port);
                      newConfig.appendChild(newConfigParam_port);
                }
                // username
                if(username != null && !username.equals("")) {
                      Element newConfigParam_username = NoAuthConfigDocument.createElement("param");
                      newConfigParam_username.setAttribute("name","username");
                      newConfigParam_username.setAttribute("value",username);
                      newConfig.appendChild(newConfigParam_username);
                }
                // password
                if(password != null && !password.equals("")) {
                      Element newConfigParam_password = NoAuthConfigDocument.createElement("param");
                      newConfigParam_password.setAttribute("name","password");
                      newConfigParam_password.setAttribute("value",password);
                      newConfig.appendChild(newConfigParam_password);
                }
                // workgroup
                if(workgroup != null && !workgroup.equals("")) {
                      Element newConfigParam_workgroup = NoAuthConfigDocument.createElement("param");
                      newConfigParam_workgroup.setAttribute("name","workgroup");
                      newConfigParam_workgroup.setAttribute("value",workgroup);
                      newConfig.appendChild(newConfigParam_workgroup);
                }
                // Append new Configuration Node
                NoAuthConfigDocument.getDocumentElement().appendChild(newConfig);
            } else {
              _log.warn(LS+"Configuration with name '" + name + "' already exists");
            } // GetConfigNode != null
          } // name != null && protocol != null && !name.equals("") && !protocol.equals("")
      } catch (Exception e) {
          _log.error(LS+"Exception!"
                    +LS+"--------------------------"
                    +LS+e.toString());
      }
    } // NoAuthConfigDocument!=null && nac!=null
  }

  /** 
   * Save the Guacamole' noauth-config.xml file as it is in memory
   */
  public void Save(String filename) {
    if (NoAuthConfigDocument != null && filename != null) {
      try {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(NoAuthConfigDocument);
        StreamResult result = new StreamResult(new File(filename));
        transformer.transform(source, result);
      } catch (Exception e) {
          _log.error(LS+"Exception!"
                    +LS+"--------------------------"
                    +LS+e.toString());
      }
    } // NoAuthConfigDocument != null && filename != null
  }

  /**
   * Class that contains a sinlge Guacamole' noauth-config  record
   */ 
  public class NoAuthConfig {
      String name;
      String protocol;
      String hostname;
      String port;
      String username; 
      String password;
      String workgroup;

      public NoAuthConfig()  {}

      public NoAuthConfig(String name
                         ,String protocol
                         ,String hostname
                         ,String port
                         ,String username
                         ,String password
                         ,String workgroup) {
          if(name      != null) this.name=name;
          if(protocol  != null) this.protocol = protocol;
          if(hostname  != null) this.hostname = hostname;
          if(port      != null) this.port     = port;
          if(username  != null) this.username = username;
          if(password  != null) this.password = password;
          if(workgroup != null) this.workgroup= workgroup; 
      } // NoAuthConfig

      /**
       * Stream to a string the noauth-config record
       */
      @Override
      public String toString() {
         return   "name: '"      + name      + "'"
              +" - protocol: '"  + protocol  + "'"
              +" - address: '"   + hostname  + ":" + port + "'" 
              +" - user: '"      + username  + "'"
              +" - password: '"  + password  + "'"
              +" - workgroup: '" + workgroup + "'";
      } // toString
  } // NoAuthCOnfig
} // NoAuthConfigXML
