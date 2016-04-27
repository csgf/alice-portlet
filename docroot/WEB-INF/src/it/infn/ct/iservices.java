/**************************************************************************
Copyright (c) 2011:
Istituto Nazionale di Fisica Nucleare (INFN), Italy
Consorzio COMETA (COMETA), Italy

See http://www.infn.it and and http://www.consorzio-cometa.it for details on
the copyright holders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(COMETA)
****************************************************************************/
package it.infn.ct;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.io.*;
import java.net.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class iservices {

    //  cloudmgr
    //  iservices requires the cloudmgr service since allocated services
    //  cannot contact the portlet directly due to the missing Shibboleth credentials
    //  while on the other hand the portlet cannot contact the service because
    //  the GridEngine does not provide a function to retrieve the service IP that is
    //  directly linked with the submitted job
    //  The cloudmgr service links both the instantiated VM with the portlet by a set
    //  of callable REST APIs made by secure connections done by the eTokenServer's
    //  proxy certificates
    class CloudMgr {

        /**
         * CloudMgrCommands - client based interface to CloudMgr servcice
         */
        class CloudMgrCommands {

              String  cert                = "";
              String  cacert              = "";
              String  capath              = "";
              String  cloudmgrHost        = "";
              String  eTokenHost          = "";
              String  eTokenPort          = "";
              String  eTokenMd5Sum        = "";
              String  eTokenVO            = "";
              String  eTokenVOGroup       = "";
              boolean eTokenProxyRenewal  = false;

              String curlCmdOutput = "";
              String curlCmdError  = "";
              String cmdOutput     = "";
              String cmdError      = "";
              int    cmdExitCode   =  0;

              CloudMgrCommands() {
                  cert                = "";
                  cacert              = "";
                  capath              = "";
                  cloudmgrHost        = "";
                  eTokenHost          = "";
                  eTokenPort          = "";
                  eTokenMd5Sum        = "";
                  eTokenVO            = "";
                  eTokenVOGroup       = "";
                  eTokenProxyRenewal  = false;
              }

              CloudMgrCommands(String cliCert, String caCert, String caPath, String cmgrHost,
                               String eTokenHost, String eTokenPort, String eTokenMd5Sum, String eTokenVO, String eTokenVOGroup, boolean eTokenProxyRenewal) {
                  cert                    = cliCert;
                  cacert                  = caCert;
                  capath                  = caPath; 
                  cloudmgrHost            = cmgrHost;
                  this.eTokenHost         = eTokenHost;
                  this.eTokenPort         = eTokenPort;
                  this.eTokenMd5Sum       = eTokenMd5Sum;
                  this.eTokenVO           = eTokenVO;
                  this.eTokenVOGroup      = eTokenVOGroup;
                  this.eTokenProxyRenewal = eTokenProxyRenewal;
              }
              /**
               * Exec executes a given command
               */
             public String Exec(String[] command) {
               if(command != null && command.length != 0) {
                 try {
                   cmdOutput=cmdError="";
                   Process p = Runtime.getRuntime().exec(command);
                   BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                   BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                   cmdExitCode = p.waitFor();
                   String s;
                   for(int i=0; (s = stdInput.readLine()) != null; i++) cmdOutput+=(i==0)?s:"\n"+s;
                   for(int i=0; (s = stdError.readLine()) != null; i++) cmdError +=(i==0)?s:"\n"+s;
                   String CmdStr="";
                   for(int i=0; i<command.length; i++) 
                     CmdStr += command[i]+" ";
                   _log.info(LS+"Cmd: '"+CmdStr+"'"
                            +LS+"Cmd: Output   -> '" + cmdOutput   + "'"
                            +LS+"Cmd: Error    -> '" + cmdError    + "'"
                            +LS+"Cmd: ExitCode -> '" + cmdExitCode + "'"
                            );
                 }
                 catch(Exception e) {
                   e.printStackTrace();
                 }
                 return cmdOutput;
               }
               return "";
             } 

              /**
               * curlExec executes curl command with given options; returning the output stream
               */
             public String curlExec(String endPoint, String certArgs, String optArgs, String getArgs) {
                 if(    endPoint != null && !endPoint.equals("")
                    &&  certArgs != null && !certArgs.equals(""))
                   try {
                       // First generate the robot proxy
                       getRobotProxyFile(eTokenHost
                                        ,eTokenPort
                                        ,eTokenMd5Sum
                                        ,eTokenVO
                                        ,eTokenVOGroup
                                        ,""+eTokenProxyRenewal
                                        ,cert);
                       curlCmdOutput = curlCmdError = "";
                       if(null == optArgs) optArgs = "";
                       if(null == getArgs) getArgs = "";
                       String command[] = { "/bin/bash", "-c", "curl "+certArgs+" "+optArgs+" \""+endPoint+getArgs+"\"" };
        
                       Process p = Runtime.getRuntime().exec(command);
                       BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                       BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
              
                       String s;
                       for(int i=0; (s = stdInput.readLine()) != null; i++) curlCmdOutput+=(i==0)?s:"\n"+s; 
                       for(int i=0; (s = stdError.readLine()) != null; i++) curlCmdError +=(i==0)?s:"\n"+s; 

                       //String curlCmdStr="";
                       //for(int i=0; i<command.length; i++) 
                       //  curlCmdStr += command[i]+" ";
                       //_log.info(LS+"curlCmd: '"+curlCmdStr+"'"
                       //         +LS+"curlCmd: Output -> '"+curlCmdOutput+"'"
                       //         +LS+"curlCmd: Error  -> '"+curlCmdError +"'"
                       //         );
                       
                   }
                   catch(Exception e) {
                       e.printStackTrace();
                   }
                   return curlCmdOutput;
             }

              /** 
               * cloudMgr tester method that does not require any argument
               */
              public String test() {
                  if(   !      cert.equals("")
                     && !      cacert.equals("")
                     && !      capath.equals("")
                     && !cloudmgrHost.equals("")) {
                      String certArgs = "--cert "+cert+" --cacert "+cacert+" --capath "+capath;
                      String endPoint = cloudmgrHost+"/";
                      return curlExec(endPoint,certArgs,null,null);
                  }
                  else return "";
              }

              /**
               * cloudMgr vminfo - retrieve CloudMgr VM information from a given VM UUID
               */
              public String vmInfo(String vm_uuid) {
                  if(   !        cert.equals("")
                     && !      cacert.equals("")
                     && !      capath.equals("")
                     && !cloudmgrHost.equals("")
                     && null != vm_uuid 
                     && !    vm_uuid.equals("")) {
                      String certArgs = "--cert "+cert+" --cacert "+cacert+" --capath "+capath;
                      String endPoint = cloudmgrHost+"/vminfo";
                      return curlExec(endPoint,certArgs,null,"?vm_uuid="+vm_uuid);
                  }
                  else return "";
              }

              /**
               * cloudMgr vmaccess - retrieve CloudMgr VM access information from a given VM UUID
               */
              public String vmAccess(String vm_uuid) {
                   if(  !        cert.equals("")
                     && !      cacert.equals("")
                     && !      capath.equals("")
                     && !cloudmgrHost.equals("")
                     && null != vm_uuid
                     && !    vm_uuid.equals("")) {
                      String certArgs = "--cert "+cert+" --cacert "+cacert+" --capath "+capath;
                      String endPoint = cloudmgrHost+"/vmaccess";
                      return curlExec(endPoint,certArgs,null,"?vm_uuid="+vm_uuid);
                  }
                  else return "";
              }

              /**
               * Ping a given IP address and return true if successful
               */    
              public boolean pingService(String ipAddr) {
                  String[] cmd = { "/bin/bash", "-c", "(ping -q -w1 -c1 "+ipAddr+" 1>/dev/null 2>/dev/null)" };
                  String output=Exec(cmd);
                  if(cmdExitCode == 0) return true;
                  return false;
              }
        }; // CloudMgrCommands
        CloudMgrCommands cmgrCommands = null;
   
        boolean enabledClass=false; 
        String  host;
        String  cert;
        String  capath;

        public CloudMgr() {
            host   = "";
            cert   = "";
            capath = "";
            enabledClass=false;
        }
        public CloudMgr(String cloudmgrHost) {
            host = cloudmgrHost;
            cert   = "";
            capath = "";
            enabledClass=false;
        }
        public CloudMgr(String cliCert, String caPath, String cloudmgrHost,
                        String eTokenHost, String eTokenPort, String eTokenMd5Sum, String eTokenVO, String eTokenVOGroup, boolean eTokenProxyRenewal) {
            host   = cloudmgrHost;
            cert   = cliCert;
            capath = caPath;
            enabledClass=false;
            cmgrCommands = new CloudMgrCommands(cert, cert, capath, host, eTokenHost, eTokenPort, eTokenMd5Sum, eTokenVO, eTokenVOGroup, eTokenProxyRenewal);
            String test=cmgrCommands.test();
            if(null!=cmgrCommands && !test.equals("")) enabledClass = true;
        }

        public boolean isEnabled() { return enabledClass; }

        /**
         * getRobotProxy - retrieves a valid proxy from the eTokenServer 
         *
         * @param eTokenServer - eTokenServer host IP or address 
         * @param eTokenServerPort - eTokenServer port number
         * @param proxyId - Proxy identifier (MD5SUM of certificate subject)
         * @param VO - virtual organization name
         * @param FQAN - VOMS attributes
         * @param proxyRenewal - Boolean flag for automatic proxy renewal 
         * 
         */
         public String getRobotProxy (String eTokenServer
                                     ,String eTokenServerPort
                                     ,String proxyId
                                     ,String VO
                                     ,String FQAN
                                     ,String proxyRenewal)
        {
            String proxyContent="";
            try {
                URL proxyURL =
                       new URL("http://"
                       + eTokenServer
                       + ":"
                       + eTokenServerPort
                       + "/eTokenServer/eToken/"
                       + proxyId
                       + "?voms="
                       + VO
                       + ":/"
                       + VO
                       + "&proxy-renewal="
                       + proxyRenewal
                       + "&disable-voms-proxy=false&rfc-proxy=false&cn-label=Insert");
               URLConnection proxyConnection = proxyURL.openConnection();
               proxyConnection.setDoInput(true);

               InputStream proxyStream = proxyConnection.getInputStream();
               BufferedReader input = new BufferedReader(new InputStreamReader(proxyStream));

               String line = "";
               while ((line = input.readLine()) != null)
                   proxyContent += line+"\n";
           } catch (IOException e) { _log.info("Caught excepion: "+e.toString()); }
           
           return proxyContent;
        }
        public void getRobotProxyFile(String eTokenServer
                                     ,String eTokenServerPort
                                     ,String proxyId
                                     ,String VO
                                     ,String FQAN
                                     ,String proxyRenewal
                                     ,String proxyFile)
        {
            if(null != proxyFile && !proxyFile.equals("")) {
                String robotProxy = getRobotProxy(eTokenServer
                                                 ,eTokenServerPort
                                                 ,proxyId
                                                 ,VO
                                                 ,FQAN
                                                 ,proxyRenewal);
                if (null != proxyFile) {
                    try {
                        File pxFile;
                        pxFile = new File(proxyFile);
                        FileWriter fw = new FileWriter(pxFile);
                        fw.write(robotProxy);
                        fw.close();
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }

        public String vmAccess(String vmuuid) { return cmgrCommands.vmAccess(vmuuid); }
        public boolean pingService(String ipAddr) { return (cmgrCommands==null)?false:cmgrCommands.pingService(ipAddr); }
    };
    CloudMgr cloudmgr = null;

    public boolean isCloudMgrEnabled() { return null==cloudmgr?false:cloudmgr.isEnabled(); }
    
    public void initCloudMgr(String cliCert, String caPath, String cloudmgrHost,
                             String eTokenHost, String eTokenPort, String eTokenMd5Sum, 
                             String eTokenVO, String eTokenVOGroup, boolean eTokenProxyRenewal) { 
        cloudmgr = new CloudMgr(cliCert,caPath,cloudmgrHost,eTokenHost,eTokenPort,eTokenMd5Sum,eTokenVO,eTokenVOGroup,eTokenProxyRenewal); 
    }
    public String getRobotProxy(String eTokenServer
                               ,String eTokenServerPort
                               ,String proxyId
                               ,String VO
                               ,String FQAN
                               ,String proxyRenewal) { if (cloudmgr != null) return cloudmgr.getRobotProxy(eTokenServer
                                                                                                          ,eTokenServerPort
                                                                                                          ,proxyId
                                                                                                          ,VO
                                                                                                          ,FQAN
                                                                                                          ,proxyRenewal);
                                                       else return null; }
    public void getRobotProxyFile(String eTokenServer
                                 ,String eTokenServerPort
                                 ,String proxyId
                                 ,String VO
                                 ,String FQAN
                                 ,String proxyRenewal
                                 ,String proxyFile) { if (cloudmgr != null) cloudmgr.getRobotProxyFile(eTokenServer
                                                                                                      ,eTokenServerPort
                                                                                                      ,proxyId
                                                                                                      ,VO
                                                                                                      ,FQAN
                                                                                                      ,proxyRenewal
                                                                                                      ,proxyFile); }

    public boolean pingService(String ipAddr) {
        if(!ebabled_class || cloudmgr==null) return false;
        return cloudmgr.pingService(ipAddr);
    } 

    // Instantiate the logger object
    AppLogger _log = new AppLogger(iservices.class);
    // Line separator
    public static final String LS = System.getProperty("line.separator");

    // DB settings
    String iservices_dbname = "";
    String iservices_dbhost = "";
    String iservices_dbport = ""; 
    String iservices_dbuser = "";
    String iservices_dbpass = "";
    String iservices_srvname= "";

    // Database variables
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private String connectionURL = null;

    // Class activation flag; used by all methods
    boolean ebabled_class = false;

    // Services
    int allocatedServices=0;
    int maxAllowedAllocations=0;

    // Constructor
    public iservices(String iservices_srvname
                    ,String iservices_dbname
                    ,String iservices_dbhost
                    ,String iservices_dbport  
                    ,String iservices_dbuser
                    ,String iservices_dbpass) {
        this.iservices_srvname= iservices_srvname;
        this.iservices_dbname = iservices_dbname;
        this.iservices_dbhost = iservices_dbhost;
        this.iservices_dbport = iservices_dbport;
        this.iservices_dbuser = iservices_dbuser;
        this.iservices_dbpass = iservices_dbpass;

        connectionURL="jdbc:mysql://" + iservices_dbhost
                     +":"             + iservices_dbport
                     +"/"             + iservices_dbname
                     +"?user="        + iservices_dbuser
                     +"&password="    + iservices_dbpass;
        _log.info("connectionURL: '"+connectionURL+"'");
	
	if(!test_connection())
             _log.error("Unable to connect iservies database; iservices will be not available");
        else {
            _log.info("Successfully connected to iservices database; iservices will be available");
            ebabled_class = true;
        }
    }

    public boolean isEnabled() { return ebabled_class; }

    public boolean db_connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(connectionURL);
        } catch (Exception e) {
            _log.error(e.toString());
        }
        return (connect != null);
    }

    public void db_closeall() {
        try {
            if(resultSet         != null) { resultSet.close();         resultSet         = null; }
            if(statement         != null) { statement.close();         statement         = null; }
            if(preparedStatement != null) { preparedStatement.close(); preparedStatement = null; }
            if(connect           != null) { connect.close();           connect           = null; }
        } catch (Exception e) {
            _log.error(e.toString());
        }
    }

    public boolean test_connection() { return db_connect(); }
    public int     getServiceId()    { return getServiceId(iservices_srvname); }
    public String  getServiceName()  { return iservices_srvname; }

    public int getServiceId(String serviceName) {
        int serviceId = -1;
        if(!ebabled_class) return -1;
        if(!db_connect() ) return -2;
        if(null == serviceName || serviceName.equals("")) return -3;
        try {
            String query="select srv_id from services where srv_name = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setString(1,serviceName);
            resultSet=preparedStatement.executeQuery();
            if(resultSet.next())
              serviceId = resultSet.getInt("srv_id");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return serviceId;
    }

    public String getServiceShDesc() {
        return getServiceShDesc(getServiceId());
    }
    public String getServiceShDesc(int serviceId) {
        String serviceShDesc = "";
        if(!ebabled_class) return "";
        if(!db_connect() ) return "";
        if(serviceId <= 0) return "";
        try {
            String query="select srv_shdesc from services where srv_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,serviceId);
            resultSet=preparedStatement.executeQuery();
            if(resultSet.next())
              serviceShDesc = resultSet.getString("srv_shdesc");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return serviceShDesc;
    }

    public String getServiceDesc() {
        return getServiceDesc(getServiceId());
    }
    public String getServiceDesc(int serviceId) {
        String serviceDesc = "";
        if(!ebabled_class) return "";
        if(!db_connect() ) return "";
        if(serviceId <= 0) return "";
        try {
            String query="select srv_desc from services where srv_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,serviceId);
            resultSet=preparedStatement.executeQuery();
            if(resultSet.next())
              serviceDesc = resultSet.getString("srv_desc");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return serviceDesc;
    }

    public int  getMaxAllowedAllocations() { return maxAllowedAllocations; }
    public void getMaxAllowedAllocations(int serviceId) {
        maxAllowedAllocations = -1;
        if(!ebabled_class) return;
        if(0 > serviceId ) return;
        if(!db_connect() ) return;
        try {
            String query="select srv_limit from services where srv_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,serviceId);
            resultSet=preparedStatement.executeQuery();
            resultSet.next();
            maxAllowedAllocations = resultSet.getInt("srv_limit");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
    }

    public int getServiceDuration() { return  getServiceDuration(getServiceId(iservices_srvname)); }
    public int getServiceDuration(int serviceId) {
        int serviceDuration = -1;
        if(!ebabled_class) return -1;
        if(!db_connect() ) return -2;
        if(0 > serviceId ) return -3;
        try {
            String query="select srv_duration from services where srv_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,serviceId);
            resultSet=preparedStatement.executeQuery();
            resultSet.next();
            serviceDuration = resultSet.getInt("srv_duration");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return serviceDuration;
    }

    public String getUUID() {
        String UUID = "";
        if(!ebabled_class) return UUID;
        if(!db_connect() ) return UUID;
        try {
            String query="select UUID() uuid;";
            statement = connect.createStatement();
            resultSet=statement.executeQuery(query);
            resultSet.next();
            UUID = resultSet.getString("uuid");
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return UUID;
    }

    public void ValidateInstances(String portalUser, int serviceId) {
        if(!ebabled_class) return;
        if(null == portalUser || portalUser.equals("") || serviceId < 0) return;
        if(!db_connect() ) return;
        // Update status for expired services
        try {
            String query="update allocated_services set alloc_state='EXPIRED' where alloc_expts <= now() and portal_user=? and srv_id=? and (alloc_state!='EXPIRED');";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setString(1, portalUser);
            preparedStatement.setInt(2, serviceId);
            preparedStatement.executeUpdate();
        } catch(SQLException e) {
            _log.error(e.toString());
        }
        // Update statuses for other services using AccessInfo
        if(allocInfo != null)
          for(int i=0; i<allocInfo.length; i++) {
            // The array loops up to the max allocable services (maxAllowedAllocations)
            // allocInfo may be null for some array elements
            if(allocInfo[i] != null && allocInfo[i].accInfo != null) {
              AllocInfo alloc = allocInfo[i];
              // ISSUE: a service may have more than one access; service status may be affected
              // by the ping output of the last access record in the array
              for(int j=0; j<alloc.accInfo.length; j++) {
                AccessInfo access = alloc.accInfo[j];
                if(access.ip.equals("")) 
                     alloc.allocState="SUBMITTED"; // How long the service may stay in this status? 
                else if(pingService(access.ip)) 
                     alloc.allocState="RUNNING";
                else alloc.allocState="UNKNOWN";   // How long the service may stay in this status?
                // Now update the db status accordingly
                try {
                  String query="update allocated_services set alloc_state=? where srv_uuid=?;";
                  preparedStatement = connect.prepareStatement(query);
                  preparedStatement.setString(1, alloc.allocState);
                  preparedStatement.setString(2, alloc.srvUUID);
                  preparedStatement.executeUpdate();
                } catch(SQLException e) {
                  _log.error(e.toString());
                }
              }
            }
          }
        db_closeall();
    }

    public class AccessInfo {
        String ip       = "";
        String workgroup= "";
        String username = "";
        String password = "";
        String port     = "";
        String proto    = ""; 
        
        public AccessInfo(String ip, String proto, String port, String workgroup, String username, String password) {
            this.ip       = ip;
            this.workgroup= workgroup;
            this.username = username;
            this.password = password;
            this.port     = port;
            this.proto    = proto; 
        }

        public String getIP()        { return ip;        }
        public String getWorkGroup() { return workgroup; }
        public String getUserName()  { return username;  }
        public String getPassword()  { return password;  }
        public String getPort()      { return port;      }
        public String getProto()     { return proto;     }
    };

    public class AllocInfo {
        Date         allocTs;
        Date         allocExpTs;
        String       allocState;
        int          allocId;
        String       srvUUID;
        AccessInfo[] accInfo=null;

        public Date         getAllocTs()    { return allocTs;    }
        public Date         getAllocExpTs() { return allocExpTs; }
        public String       getAllocState() { return allocState; }
        public int          getAllocId()    { return allocId;    }
        public String       getSrvUUID()    { return srvUUID;    }
        public AccessInfo[] getAccInfo()    { return accInfo;    }

        public AllocInfo() {
            allocTs    = null;
            allocExpTs = null;
            allocState = null;
            srvUUID    = "";
            allocId    = -1;
            accInfo    = null;
        }
        public AllocInfo(Date allocTs, Date allocExpTs, String allocState, int allocId, String srvUUID) {
            this.allocTs    = allocTs;
            this.allocExpTs = allocExpTs;
            this.allocState = allocState;
            this.allocId    = allocId; 
            this.srvUUID    = srvUUID;
            this.accInfo    = null; // Access info are determined querying cloudmgr service
        }
        public void setInfoValues(Date allocTs, Date allocExpTs, String allocState, int allocId, String srvUUID) {
            this.allocTs    = allocTs;
            this.allocExpTs = allocExpTs;
            this.allocState = allocState;
            this.allocId    = allocId;
            this.srvUUID    = srvUUID;
            this.accInfo    = null; // Access info are determined querying cloudmgr service
        }
        public String dump() {
            return "["+allocId+"] - '"   + srvUUID  + "'"
                   + LS + "allocTs   : " + allocTs
                   + LS + "allocExpTs: " + allocExpTs
                   + LS + "allocState: " + allocState
                   + LS + dumpAccessInfo()
                   ;
        }
        public String dumpAccessInfo() {
            String accInfoStr="";
            if(accInfo != null && accInfo.length > 0) 
                for(int i=0; i<accInfo.length; i++) 
                   accInfoStr +=(      "  Access Info ("+i+")"
                                 +LS + "    ip       : '" + accInfo[i].ip        + "'"
                                 +LS + "    workgroup: '" + accInfo[i].workgroup + "'"
                                 +LS + "    username : '" + accInfo[i].username  + "'"
                                 +LS + "    password : '" + accInfo[i].password  + "'"
                                 +LS + "    port     : '" + accInfo[i].port      + "'"
                                 +LS + "    proto    : '" + accInfo[i].proto     + "'"
                                );
            else   accInfoStr="No access info available yet";
            return accInfoStr;
        }

        public void buildAccessInfoFromXML(String xmlInfo) {
          Document accInfoDoc=null;
          if(xmlInfo != null && !xmlInfo.equals("")) {
            _log.info("AccessInfo parsing XML: '"+xmlInfo+"'");
            try {
              DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
              DocumentBuilder builder=factory.newDocumentBuilder();
              InputSource is=new InputSource(new StringReader(xmlInfo));
              accInfoDoc=builder.parse(is);
              if(accInfoDoc != null) {
                  NodeList nodeList=accInfoDoc.getElementsByTagName("accessinfo");
                  accInfo = new AccessInfo[nodeList.getLength()];
                  for (int i=0; i < nodeList.getLength(); ++i) {
                    Node node=nodeList.item(i);
                    accInfo[i] = new AccessInfo(node.getAttributes().getNamedItem("ip"       ).getNodeValue() // ip 
                                               ,node.getAttributes().getNamedItem("proto"    ).getNodeValue() // proto
                                               ,node.getAttributes().getNamedItem("port"     ).getNodeValue() // port
                                               ,node.getAttributes().getNamedItem("workgroup").getNodeValue() // workgroup
                                               ,node.getAttributes().getNamedItem("username" ).getNodeValue() // username 
                                               ,node.getAttributes().getNamedItem("password" ).getNodeValue() // password
                                              );
                    // Now take care of Guacamole' NoAuthConfig if needed
                    if(isNoAuthConfigEnabled()) {
                        String noAuthConfigXMLAccessInfoName = node.getAttributes().getNamedItem("proto"    ).getNodeValue()
                                                              + "_" 
                                                              + getSrvUUID();
                                                             // Old fashioned way to call resource removed since guacamole 0.9.5
                                                             // which does not accept any longer long resource names 
                                                             //  node.getAttributes().getNamedItem("proto"    ).getNodeValue() // protocol
                                                             //+ "://"                                                         // ://
                                                             //+ node.getAttributes().getNamedItem("username" ).getNodeValue() // username
                                                             //+ "@"                                                           // @
                                                             //+ node.getAttributes().getNamedItem("ip"       ).getNodeValue() // ip address
                                                             //+ ":"                                                           // :
                                                             //+ node.getAttributes().getNamedItem("port"     ).getNodeValue() // port
                                                             ;
                        Node noAuthConfigXMLNode = noAuthConfigXML.GetConfigNode(noAuthConfigXMLAccessInfoName);
                        if(null == noAuthConfigXMLNode) {
                             _log.info("Node '"+noAuthConfigXMLAccessInfoName+"' does not exists; inserting in Guacamole' NoAuthConfig");
                             noAuthConfigXML.AddNewConfig( noAuthConfigXMLAccessInfoName                                // NoAuthConfig name
                                                         ,node.getAttributes().getNamedItem("proto"    ).getNodeValue() // protocol
                                                         ,node.getAttributes().getNamedItem("ip"       ).getNodeValue() // ip address
                                                         ,node.getAttributes().getNamedItem("port"     ).getNodeValue() // port
                                                         ,node.getAttributes().getNamedItem("username" ).getNodeValue() // username
                                                         ,node.getAttributes().getNamedItem("password" ).getNodeValue() // password
                                                         ,node.getAttributes().getNamedItem("workgroup").getNodeValue() // workgroup
                                                        );
                             noAuthConfigXML.Save(noAuthConfigPath);
                        }
                        else _log.info("Node '"+noAuthConfigXMLAccessInfoName+"' already exists in Guacamole' NoAuthConfig");
                    }
                  }
              }
              else {
                  _log.error("No AccessInfo document!");
              }
            } catch(Exception e) {
            System.out.println(LS+"Exception!"
                              +LS+"--------------------------"
                              +LS+e.toString());
            }
          }
        }
        
        // ...
    };
    AllocInfo[] allocInfo = null;

    public boolean hasAllocations(String portalUser) {
        if(!ebabled_class) return false;
        if(null == allocInfo) getAllocationInfo(portalUser);
        return (allocInfo!=null && allocInfo.length>0)?true:false;
    }
    public int  getNumAllocations() { return (allocInfo==null?-1:allocatedServices); }
    public void getAllocationInfo(String portalUser) { getAllocationInfo(portalUser, getServiceId()); }
    public void getAllocationInfo(String portalUser,int serviceId) {
        if(!ebabled_class) return;
        if(null == portalUser || portalUser.equals("") || serviceId < 0) return;
        AllocInfo[] allocInfo = null;
        // Get maximum number of allowed allocation for this service
        getMaxAllowedAllocations(serviceId);
        if(!db_connect() ) return;
        try {
          // Prepare the allocInfo array
          allocInfo = new AllocInfo[maxAllowedAllocations];
          String query="select  alloc_ts, alloc_expts, alloc_state, alloc_id, srv_uuid from allocated_services where srv_id = ? and portal_user = ? and alloc_state!='EXPIRED' and now() < alloc_expts;";
          preparedStatement = connect.prepareStatement(query);
          preparedStatement.setInt   (1,serviceId);
          preparedStatement.setString(2,portalUser);
          resultSet=preparedStatement.executeQuery();
          int i=0;
          while(resultSet.next() && i<maxAllowedAllocations) {
            allocInfo[i] = new AllocInfo( resultSet.getDate  ("alloc_ts")
                                         ,resultSet.getDate  ("alloc_expts")
                                         ,resultSet.getString("alloc_state")
                                         ,resultSet.getInt   ("alloc_id")
                                         ,resultSet.getString("srv_uuid")
                                        ); 
            // Now query cloudMgr to get access info
            String allocInfoXmlStr=cloudmgr.vmAccess(resultSet.getString("srv_uuid"));
            _log.info(LS+"AccessInfoXML('"+resultSet.getString("srv_uuid")+"'): '"+allocInfoXmlStr);
            // XML String must be parsed and accInfo array generated accordingly
            allocInfo[i].buildAccessInfoFromXML(allocInfoXmlStr);
            // Increase counter
            i++;
          }
          allocatedServices = i;
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        this.allocInfo = allocInfo;
        // Validate user VM list (flag expired isntances)
        ValidateInstances(portalUser, serviceId);
    }
    public String dumpAllocations() {
         String allocInfoStr = "";
         if(ebabled_class && allocatedServices > 0)
           for(int i=0; i<allocatedServices; i++) 
                  allocInfoStr += LS + allocInfo[i].dump();
         return allocInfoStr;
    }

    public void allocService(String portalUser,String srvUUID) {
         if(!ebabled_class) return;
         if(null == portalUser || portalUser.equals("")) return;
         int serviceId       = getServiceId();
         int serviceDuration = getServiceDuration(serviceId);
         if(!db_connect() ) return;
         try {
             String query="insert into allocated_services (srv_id,srv_uuid,portal_user,alloc_ts,alloc_expts,alloc_state) values (?,?,?,now(),date_add(now(), interval ? second),'SCHEDULED');";
             preparedStatement = connect.prepareStatement(query);
             preparedStatement.setInt   (1,serviceId);
             preparedStatement.setString(2,srvUUID);
             preparedStatement.setString(3,portalUser);
             preparedStatement.setInt   (4,serviceDuration);
             preparedStatement.executeUpdate();
         } catch (SQLException e) {
             _log.error(e.toString());
         }
         db_closeall();
         // Reload allocInfo
         getAllocationInfo(portalUser);
    }

    //
    // NoAuthConfig
    // 
    
    // Instantiate the guacamole noauth config XML
    NoAuthConfigXML noAuthConfigXML = null;
    boolean noAuthConfigEnabled     = false;
    String  noAuthConfigPath        = "";

    public boolean isNoAuthConfigEnabled() { return noAuthConfigEnabled; }

    public void initNoAuthConfigXML(String NoAuthConfigXMLPath) {
         if(!ebabled_class) return;
         noAuthConfigXML = new NoAuthConfigXML(_log,NoAuthConfigXMLPath);
         if(null != noAuthConfigXML) {
             noAuthConfigEnabled = true;
             noAuthConfigPath    = NoAuthConfigXMLPath;
             _log.info("[NoAuthConfig]" + LS +  noAuthConfigXML.dump());
         }
         else {
             noAuthConfigEnabled = false;
             _log.info("[NoAuthConfig]" + LS +  "NoAuthConfigXML not available; Guacamole settings will be not supported");
         }
    }
} // iservices 
