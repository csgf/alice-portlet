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
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * CloudPovider class used to manage OCCI cloud endpoints for GridEngine
 * Job submission
 */
class CloudProvider {

    // Instantiate the logger object
    AppLogger _log = new AppLogger(CloudProvider.class);
    // Line separator
    public static final String LS = System.getProperty("line.separator");

    // DB settings
    String cloudprovider_dbname = "";
    String cloudprovider_dbhost = "";
    String cloudprovider_dbport = "";
    String cloudprovider_dbuser = "";
    String cloudprovider_dbpass = "";

    // Database variables
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;
    private String connectionURL = null;

    // Class activation flag; used by all methods
    boolean ebabled_class = false;

    String  proto;
    String  name;
    String  host;
    int     port;
    Map params = null;

    public boolean isEnabled() { return ebabled_class; }

    public CloudProvider(String cloudprovider_dbname
                        ,String cloudprovider_dbhost
                        ,String cloudprovider_dbport
                        ,String cloudprovider_dbuser
                        ,String cloudprovider_dbpass) {
        this.cloudprovider_dbname = cloudprovider_dbname;
        this.cloudprovider_dbhost = cloudprovider_dbhost;
        this.cloudprovider_dbport = cloudprovider_dbport;
        this.cloudprovider_dbuser = cloudprovider_dbuser;
        this.cloudprovider_dbpass = cloudprovider_dbpass;

        connectionURL="jdbc:mysql://" + cloudprovider_dbhost
                     +":"             + cloudprovider_dbport
                     +"/"             + cloudprovider_dbname
                     +"?user="        + cloudprovider_dbuser
                     +"&password="    + cloudprovider_dbpass;
        _log.info("connectionURL: '"+connectionURL+"'");

        if(!test_connection())
             _log.error("Unable to connect cloudprovider database; cloudprovider will be not available");
        else {
            _log.info("Successfully connected to iservices database; iservices will be available");
            ebabled_class = true;
        }
    }

    public CloudProvider(String name, String host, int port, String proto) {
        this.name  = name;
        this.host  = host;
        this.port  = port;
        this.proto = proto;
        // Initialize the rOCCI params map
        params = new HashMap();
    }
        
    // Add a single parameter to the cloud provider object
    public void addParam(String paramName, String paramValue) {
        if(params == null)
           params = new HashMap();
           params.put(paramName,paramValue);
    }

    // Add a set of params to the cloud provider object
    public void addParams(String params[][]) {
        if(params != null)
            for (int i=0; i<params.length; i++)
                addParam(params[i][0],params[i][1]);
    }

    // Build the cloud provider endpoint
    public String endPoint() {
        String endPoint = proto + "://"
                        + host  + ":"
                        + port  + "/?";
        int i = 0;
        Set set = params.keySet();
        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            String paramName = o.toString();
            String param = paramName + "=" + params.get(paramName);
            if (i == 0 ) i++;
            else param = "&"+param;
            endPoint+=param;
        }
        return endPoint;
    }

    public int geOperationId=-1;
    List<Integer> cloud_provider_ids = new ArrayList<Integer>();
 
    //
    // Database methods 
    //
    public boolean test_connection() { return db_connect(); }
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

    public int getProviderList(int geOperationId) {
        int numCloudProviders = 0;
        String serviceDesc = "";
        if(!ebabled_class) return -1;
        if(!db_connect() ) return -1;
        if(geOperationId <= 0) return -1;
        this.geOperationId = geOperationId;
        cloud_provider_ids.clear();
        try {
            String query="select cp.id from cloud_provider_app cpa,cloud_provider cp where cpa.enabled is true and cp.enabled is true and ge_operation_id=? and cp.id=cpa.cloud_provider_id;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,geOperationId);
            resultSet=preparedStatement.executeQuery();
            while(resultSet.next()) {
              cloud_provider_ids.add(resultSet.getInt("cp.id"));
              numCloudProviders++;
            }
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return numCloudProviders;
    }

    public class CloudProviderInfo {
        public String name;
        public String host;
        public int    port;
        public String proto;

        public CloudProviderInfo(String name, String host, int port, String proto) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.proto= proto;
        }
    };

    public CloudProviderInfo getCloudProviderInfo(int CloudProviderId) {
        CloudProviderInfo cpi = null;
        if(!ebabled_class) return null;
        if(CloudProviderId <= 0) return null;
        if(!db_connect() ) return null; 
        try {
            String query="select name, address, port, proto from cloud_provider where id = ? and enabled is true;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,CloudProviderId);
            resultSet=preparedStatement.executeQuery();
            while(resultSet.next()) {
                cpi = new CloudProviderInfo(resultSet.getString("name")
                                           ,resultSet.getString("address")
                                           ,resultSet.getInt("port")
                                           ,resultSet.getString("proto")
                                          );   
            }
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return cpi;
    }

    public String[][] getCloudProviderParams(int geOperationId, int CloudProviderId) {
        String cpParams[][] = null;
        int numParams = 0;
        if(!ebabled_class) return null;
        if(geOperationId <= 0) return null; 
        if(CloudProviderId <= 0) return null;
        if(!db_connect() ) return null;
        String query = "";
        try {
            // Get param count first
            query="select count(*) from cloud_provider_params where ge_operation_id = ? and cloud_provider_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,geOperationId);
            preparedStatement.setInt(2,CloudProviderId);
            resultSet=preparedStatement.executeQuery();
            while(resultSet.next()) {
                numParams = resultSet.getInt("count(*)"); 
            }
            cpParams = new String[numParams][2];
            // Get params 
            query="select param_name, param_value from cloud_provider_params where ge_operation_id = ? and cloud_provider_id = ?;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,geOperationId);
            preparedStatement.setInt(2,CloudProviderId);
            resultSet=preparedStatement.executeQuery();
            int i=0;
            while(resultSet.next()) {
                cpParams[i][0] = resultSet.getString("param_name");
                cpParams[i][1] = resultSet.getString("param_value");
                i++;
            }   
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return cpParams;  
    }

    public String[] getResourcesList() {
       String[] resourcesList = null;
       if(cloud_provider_ids.size() > 0) {
           resourcesList = new String[cloud_provider_ids.size()];
           // Now process the list
           Iterator iter = cloud_provider_ids.iterator();
           int i=0;
           while (iter.hasNext()) {
               int CloudProviderId=(Integer)iter.next();
               CloudProviderInfo cpi = getCloudProviderInfo(CloudProviderId);
               CloudProvider cp = new CloudProvider(cpi.name
                                                   ,cpi.host
                                                   ,cpi.port
                                                   ,cpi.proto);
               String params[][] = getCloudProviderParams(geOperationId,CloudProviderId);
               // Add OCCI parameters to the cloud provider object
               cp.addParams(params);
               String rOCCIURL = cp.endPoint();  _log.info("OCCI Endpoint: '" + rOCCIURL + "'");
               resourcesList[i] = rOCCIURL;
               i++;
           }
       }
       else _log.error("getResourcesList called with an empty resource list");
       return resourcesList;
    }

   //
   // Database methods for Infrastructure handling
   //
   public class Resource {
       int resource_id=-1;
       String name="";
       String address="";
       int port=-1;
       String proto="";
       boolean enabled=false;

       public Resource(int resource_id, String name, String address, int port, String proto, boolean enabled) {
          this.resource_id = resource_id;
          this.name = name;
          this.address = address;
          this.port = port;
          this.proto = proto;
          this.enabled = enabled;
       }
 
       HashMap<String,String> params = new HashMap<String,String>();

       public String getParam(String param_name) {
           return params.get(param_name);
       }

       public String getResourceEndpoint() {
           String endPoint = proto    + "://"
                           + address  + ":"
                           + port     + "/?";
           int i = 0;
           for (String param_name: params.keySet()) {
		String param_value = params.get(param_name);
                String param = param_name + "=" + param_value;
                if (i == 0 ) i++;
                else param = "&"+param;
                endPoint+=param;
           }
           return endPoint;         
       }
   };
   public class Infrastructure {
       int     infrastructure_id=-1;
       String  name="";
       String  adaptor="";
       boolean enabled=false; 

       public Infrastructure(int infrastructure_id, String name, String adaptor, boolean enabled) {
           this.infrastructure_id = infrastructure_id;
           this.name              = name;
           this.adaptor           = adaptor;
           this.enabled           = enabled;
       }

       HashMap<String,String> params = new HashMap<String,String>();
       List<Resource> resourcesList = new ArrayList<Resource>();

       public String[] resourceList() {
           String[] resList = new String[resourcesList.size()];
           for(int i=0; i<resourcesList.size(); i++) {
               resList[i] = resourcesList.get(i).getResourceEndpoint();
           }
           return resList;
       }

       public String getParam(String param_name) {
           return params.get(param_name);
       }
   };
   public List<Infrastructure> infrastructuresList = new ArrayList<Infrastructure>();

   // Biulds the classes above starting form the GridEngine application id
   public int AppInfrastructures(int geApplicationId) {
       if(!ebabled_class) return -1;
       if(geApplicationId <= 0) return -1;
       if(!db_connect() ) return -1;
       this.geOperationId = geOperationId;
       String query="";
       try {
            query="select i.id, i.name, i.adaptor, i.enabled and ia.enabled enabled from infrastructure_apps ia, infrastructures i where ge_operation_id = ? and i.id=ia.infrastructure_id;";
            preparedStatement = connect.prepareStatement(query);
            preparedStatement.setInt(1,geApplicationId);
            resultSet=preparedStatement.executeQuery();
            while(resultSet.next()) {
                Infrastructure infrastructure = new Infrastructure( resultSet.getInt("i.id")
                                                                   ,resultSet.getString("i.name")
                                                                   ,resultSet.getString("i.adaptor")
                                                                   ,resultSet.getBoolean("enabled"));
                infrastructuresList.add(infrastructure);
            }
            // Now process infrastructures for params
            for(int i=0; i<infrastructuresList.size(); i++) {
                Infrastructure infra4params = (Infrastructure) infrastructuresList.get(i);
                query="select param_name, param_value from infrastructure_params where infrastructure_id = ?;";
                preparedStatement = connect.prepareStatement(query);
                preparedStatement.setInt(1,infra4params.infrastructure_id);
                resultSet=preparedStatement.executeQuery();
                while(resultSet.next()) {
                   String infra_param_name  = resultSet.getString("param_name" );
                   String infra_param_value = resultSet.getString("param_value");
                   infra4params.params.put(infra_param_name,infra_param_value);
                }            
            }
            // Now process infrastructures for app resources
            for(int j=0; j<infrastructuresList.size(); j++) {
                Infrastructure infra4resources = (Infrastructure) infrastructuresList.get(j);
                query="select cp.id, cp.name, cp.address, cp.port, cp.proto, cp.enabled cp_enabled, ir.enabled ir_enabled  from cloud_provider cp, infrastructure_resources ir where cp.id=ir.cloud_provider_id and ir.infrastructure_id = ?;";
                preparedStatement = connect.prepareStatement(query);
                preparedStatement.setInt(1,infra4resources.infrastructure_id);
                resultSet=preparedStatement.executeQuery();
                while(resultSet.next()) {
                   Resource infraResource = new Resource(resultSet.getInt("cp.id")
                                                        ,resultSet.getString("cp.name")
                                                        ,resultSet.getString("cp.address")
                                                        ,resultSet.getInt("cp.port")
                                                        ,resultSet.getString("cp.proto")
                                                        ,resultSet.getBoolean("cp_enabled")
                                                        &resultSet.getBoolean("ir_enabled"));
                   infra4resources.resourcesList.add(infraResource);
                }
            }
            // Now process infrastructure resource parameters
            _log.info("Now adding resource parameters"); 
            for(int h=0; h<infrastructuresList.size(); h++) {
                Infrastructure infra4resparams = (Infrastructure) infrastructuresList.get(h);
                _log.info("  Infrastructure: " + infra4resparams.name);
                for(int k=0; k<infra4resparams.resourcesList.size(); k++) {
                    Resource resource4params = infra4resparams.resourcesList.get(k);
                    _log.info("    Now adding resource parameters for " + resource4params.name);
                    query="select param_name, param_value from cloud_provider_params where ge_operation_id=? and cloud_provider_id = ?;";
                    preparedStatement = connect.prepareStatement(query);
                    preparedStatement.setInt(1,geOperationId);
                    preparedStatement.setInt(2,resource4params.resource_id);
                    resultSet=preparedStatement.executeQuery();
                    while(resultSet.next()) {
                        String res_param_name  = resultSet.getString("param_name" );
                        String res_param_value = resultSet.getString("param_value");
                        resource4params.params.put(res_param_name,res_param_value);
                        _log.info("    (param_name = "+res_param_name+" - param_value = "+res_param_value+")"); 
                    }
                }
            }
        } catch (SQLException e) {
            _log.error(e.toString());
        }
        db_closeall();
        return infrastructuresList.size();
   }
};
