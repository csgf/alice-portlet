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

@author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(INFN)
****************************************************************************/
package it.infn.ct;

// Import generic java libraries
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

// Importing portlet libraries
import javax.portlet.*;
import javax.servlet.http.HttpServletRequest;

// Importing liferay libraries
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.Group;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

// Importing GridEngine Job libraries
import it.infn.ct.GridEngine.Job.*;
import it.infn.ct.GridEngine.JobResubmission.GEJobDescription;
import it.infn.ct.GridEngine.Job.MultiInfrastructureJobSubmission;
import it.infn.ct.GridEngine.UsersTracking.UsersTrackingDBInterface;
import java.util.*;

// JSON
import org.json.simple.JSONValue;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * @author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(COMETA)
 */
    // Preferences class
    public class Preferences {

	// Instantiate the logger object
	AppLogger _log = new AppLogger(Preferences.class);

        public String LS = System.getProperty("line.separator");
        HashMap<String,String> prefValues = null;
        String prefNames[] = {
          "GridOperation"
         ,"cloudMgrHost"
         ,"proxyFile"
         ,"portalSSHKey"
         ,"eTokenHost"
         ,"eTokenPort"
         ,"eTokenMd5Sum"
         ,"eTokenVO"
         ,"eTokenVOGroup"
         ,"eTokenProxyRenewal"
         ,"alephGroupName"
         ,"guacamole_dir"
         ,"guacamole_noauthxml"
         ,"guacamole_page"
         ,"iservices_dbname"
         ,"iservices_dbhost"
         ,"iservices_dbport"
         ,"iservices_dbuser"
         ,"iservices_dbpass"
         ,"iservices_srvname"
         ,"cloudprovider_dbname"
         ,"cloudprovider_dbhost"
         ,"cloudprovider_dbport"
         ,"cloudprovider_dbuser"
         ,"cloudprovider_dbpass"
        };
        PortletPreferences pPrefs = null;
        public String[] getPrefNames() { return prefNames; }

        Preferences() {
            prefValues = new HashMap<String, String>();
            for(int i=0; i<prefNames.length; i++)
              prefValues.put( "", prefNames[i]);
        }

        PortletPreferences getPortletPreferences() { return pPrefs; }

        public void setPrefValue(String prefName, String prefValue) {
            if(prefValues != null)
               prefValues.put(prefName,prefValue);
        }
        public String getPrefValue(String prefName) {
            if(prefValues != null)
               return prefValues.get(prefName);
            else return "";
        }
        public String getPrefName(int ithName) {
            return prefNames[ithName];
        }
        public String tabify(boolean editableFlag) {
            String prefTable = "";
            String prefValue = "";
            if(prefValues != null)
               for(int i=0; i<prefNames.length; i++) {
                   if(editableFlag)
                        prefValue = "<input id=\"pref_input\" type=\"text\" name=\""+prefNames[i]+"\" value=\""+prefValues.get(prefNames[i])+"\"/></td";
                   else prefValue = prefValues.get(prefNames[i]);
                   prefTable+="<tr><td>"+prefNames[i]+"</td><td>"+prefValue+"</td></tr>"+LS;
               }
            return prefTable;
        }
        public String dump() {
            String prefDump = "";
            if(prefValues != null)
               for(int i=0; i<prefNames.length; i++)
                   prefDump+=prefNames[i]+" - "+prefValues.get(prefNames[i]) + LS;
            return prefDump;
        }
        public String json() {
            String prefJSON = "";
            String comma    = "";
            if(prefValues != null)
               for(int i=0; i<prefNames.length; i++) {
                   if (i == 0 )
                        comma = "";
                   else comma = ",";
                   prefJSON+= comma + " \"" + prefNames[i]+"\" : \""+ prefValues.get(prefNames[i]) + "\" ";
                }
            return "{ "+ prefJSON + " }";
        }
        // Set portlet preferences
        public void setPortletPrefs(ActionRequest request) {
            if(request != null) {
                this.pPrefs = request.getPreferences();
                setPortletPrefs(pPrefs);
            }
        }
        public void setPortletPrefs(RenderRequest request) {
            if(request != null) {
                this.pPrefs = request.getPreferences();
                setPortletPrefs(pPrefs);
            }
        }
        public void setPortletPrefs(ResourceRequest request) {
            if(request != null) {
                this.pPrefs = request.getPreferences();
                setPortletPrefs(pPrefs);
            }
        }
        public void setPortletPrefs(PortletPreferences pPrefs) {
            if(pPrefs != null) {
              this.pPrefs = pPrefs;
              setPortletPrefs();
            } else _log.error("Unable to set portlet preferences from null portlet preference object" + LS);
        }
        public void setPortletPrefs() {
            String report = LS;
            if(pPrefs != null) {
                for(int i=0; i<prefNames.length; i++)
                    try {
                        report += "====PREF["+prefNames[i]+"]===>"+ prefNames[i] + " = " + prefValues.get(prefNames[i]) + LS;
                        pPrefs.setValue(prefNames[i],prefValues.get(prefNames[i]));
                        pPrefs.store();
                    } catch(Exception e) {
                        _log.error("Unable to set portlet preferences: '"+e.toString()+"'");
                    }
                _log.info(report);
            } else _log.error("Unable to set portlet preferences from null portlet preference object" + LS);
        }
        // Get portlet preferences
        public void getPortletPrefs(ActionRequest request) {
            getPortletPrefs(request.getPreferences());
        }
        public void getPortletPrefs(RenderRequest request) {
            getPortletPrefs(request.getPreferences());
        }
        public void getPortletPrefs(ResourceRequest request) {
            getPortletPrefs(request.getPreferences());
        }
        public void getPortletPrefs(PortletPreferences pPrefs) {
            if(pPrefs != null) {
              for(int i=0; i<prefNames.length; i++)
                setPrefValue(prefNames[i], pPrefs.getValue(prefNames[i],""));
            } else _log.error("Unable to get portlet preferences from null portlet preference object" + LS);
        }
    }