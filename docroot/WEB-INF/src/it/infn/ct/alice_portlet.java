package it.infn.ct;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import it.infn.ct.GridEngine.Job.InfrastructureInfo;
import it.infn.ct.GridEngine.Job.MultiInfrastructureJobSubmission;
import javax.portlet.GenericPortlet;
import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderResponse;
import javax.portlet.PortletException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import javax.portlet.*;
import org.json.simple.JSONValue;

/**
 * alice_portlet Portlet Class
 */
public class alice_portlet extends GenericPortlet {
    
    
    AppLogger _log = new AppLogger(alice_portlet.class);

    private enum Actions {

        ACTION_INPUT,
        ACTION_PT_ANALISI,
        ACTION_RAA_ANALISI,
        ACTION_VM
        // Only one action is possible by this portlet 
    }

    private enum Views {

        VIEW_INPUT   // Only one view is possible by this portlet 
    }
    
    
    public String       userName   = "unknown";   // Portal username
    public String       firstName  = "unknown";   // Portal user first name
    public String       lastName   = "unknown";   // Portal user last name
    public String       userMail   = "unknown";   // Portal user mail
    public String       portalName = "localhost"; // Name of the hosting portal
    public String       appServerPath;            // This variable stores the absolute path of the Web applications

    int typeExperiment=0; 
    
    // Other misc valuse
    // (!) Pay attention that altough the use of the LS variable
    //     the replaceAll("\n","") has to be used
    public static final String LS = System.getProperty("line.separator");

    // Users must have separated inputSandbox files
    // these file will be generated into /tmp directory
    // and prefixed with the format <timestamp>_<user>_*
    // The timestamp format is:
    public static final String tsFormat = "yyyyMMddHHmmss";

    // Portlet parameters 
    int     aliceGridOperation;       // GridEngine Grid operation
    String  portalHost;               // Used to build JSPs AJAX calls
    String  portletPage;              // Used to build JSPs AJAX calls
    String  cloudmgrHost;             // CloudMgr URL
    String  proxyFile;                // Proxy full pathname
    String  portalSSHKey;             // Full path to portal' SSH public key
    String  eTokenHost;               // eTokenServer host
    String  eTokenPort;               // eTokenServer port
    String  eTokenMd5Sum;             // eTokenServer Md5Sum
    String  eTokenVO;                 // eTokenServer VO name
    String  eTokenVOGroup;            // eTokenServer VO Group name
    String  eTokenProxyRenewal;       // eTokenProxyRenewal flag
    String  AliceGroupName;           // This is the necessary Liferay' Group to enable ALEPH VM initialization
    String  guacamole_dir;            // Guacamole directory
    String  guacamole_noauthxml;      // Guacamole noauthxml file
    String  guacamole_page;           // Guacamole main page name (version)
    String  service_description;      // iService long description
    boolean isAlephVMEnabled = false; // This flag tells if the user' has the right to instantiate ALEPH VMs or not
    boolean initialized = true;       // The portlet is just initialized?
    
    
    
    
    // Preferences class
     /*public class Preferences {
        public String LS = System.getProperty("line.separator");
        HashMap<String,String> prefValues = null; 
        String prefNames[] = {
          "GridOperation"
         ,"cloudmgrHost"      
         ,"proxyFile" 
         ,"portalSSHKey"       
         ,"eTokenHost"        
         ,"eTokenPort"          
         ,"eTokenMd5Sum"        
         ,"eTokenVO"            
         ,"eTokenVOGroup"       
         ,"eTokenProxyRenewal" 
         ,"AliceGroupName"     
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
    } */
    Preferences prefs = new Preferences();
    CloudProvider cloudProvider = null;
     // iservices object
    iservices iSrv = null;
    
    @Override
    public void init()
    throws PortletException
    {
        _log.info("Calling init()");
        // Load default values from WEBINF/portlet.xml
        //
        //  <init-param>
        //     <name>view-template</name>
        //     <value>/view.jsp</value>
        // </init-param>
        String GridOp      = getInitParameter("GridOperation"      ); prefs.setPrefValue("GridOperation",GridOp);
        aliceGridOperation = Integer.parseInt(GridOp               );
        String CAPath      = getInitParameter("CAPath"             ); prefs.setPrefValue("CAPath",CAPath);
        proxyFile          = getInitParameter("ProxyFile"          ); prefs.setPrefValue("proxyFile"          ,proxyFile          );
        portalSSHKey       = getInitParameter("portalSSHKey"       ); prefs.setPrefValue("portalSSHKey"       ,portalSSHKey       );
        cloudmgrHost       = getInitParameter("CloudMgr"           ); prefs.setPrefValue("cloudmgrHost"           ,cloudmgrHost       );
        AliceGroupName     = getInitParameter("AliceGroupName"     ); prefs.setPrefValue("AliceGroupName"     ,AliceGroupName     );
        eTokenHost         = getInitParameter("eTokenHost"         ); prefs.setPrefValue("eTokenHost"         ,eTokenHost         );
        eTokenPort         = getInitParameter("eTokenPort"         ); prefs.setPrefValue("eTokenPort"         ,eTokenPort         );
        eTokenMd5Sum       = getInitParameter("eTokenMd5Sum"       ); prefs.setPrefValue("eTokenMd5Sum"       ,eTokenMd5Sum       );
        eTokenVO           = getInitParameter("eTokenVO"           ); prefs.setPrefValue("eTokenVO"           ,eTokenVO           );
        eTokenVOGroup      = getInitParameter("eTokenVOGroup"      ); prefs.setPrefValue("eTokenVOGroup"      ,eTokenVOGroup      );
        eTokenProxyRenewal = getInitParameter("eTokenProxyRenewal" ); prefs.setPrefValue("eTokenProxyRenewal" ,eTokenProxyRenewal );
        guacamole_dir      = getInitParameter("guacamole_dir"      ); prefs.setPrefValue("guacamole_dir"      ,guacamole_dir      );
        guacamole_noauthxml= getInitParameter("guacamole_noauthxml"); prefs.setPrefValue("guacamole_noauthxml",guacamole_noauthxml);
        guacamole_page     = getInitParameter("guacamole_page"     ); prefs.setPrefValue("guacamole_page"     ,guacamole_page     );
        // iservices params
        String iservices_dbname  = getInitParameter("iservices_dbname" ); prefs.setPrefValue("iservices_dbname" ,iservices_dbname );
        String iservices_dbhost  = getInitParameter("iservices_dbhost" ); prefs.setPrefValue("iservices_dbhost" ,iservices_dbhost );
        String iservices_dbport  = getInitParameter("iservices_dbport" ); prefs.setPrefValue("iservices_dbport" ,iservices_dbport );
        String iservices_dbuser  = getInitParameter("iservices_dbuser" ); prefs.setPrefValue("iservices_dbuser" ,iservices_dbuser );
        String iservices_dbpass  = getInitParameter("iservices_dbpass" ); prefs.setPrefValue("iservices_dbpass" ,iservices_dbpass );
        String iservices_srvname = getInitParameter("iservices_srvname"); prefs.setPrefValue("iservices_srvname",iservices_srvname);
        // cloudprovider params */
        String cloudprovider_dbname  = getInitParameter("cloudprovider_dbname" ); prefs.setPrefValue("cloudprovider_dbname" ,cloudprovider_dbname );
        String cloudprovider_dbhost  = getInitParameter("cloudprovider_dbhost" ); prefs.setPrefValue("cloudprovider_dbhost" ,cloudprovider_dbhost );
        String cloudprovider_dbport  = getInitParameter("cloudprovider_dbport" ); prefs.setPrefValue("cloudprovider_dbport" ,cloudprovider_dbport );
        String cloudprovider_dbuser  = getInitParameter("cloudprovider_dbuser" ); prefs.setPrefValue("cloudprovider_dbuser" ,cloudprovider_dbuser );
        String cloudprovider_dbpass  = getInitParameter("cloudprovider_dbpass" ); prefs.setPrefValue("cloudprovider_dbpass" ,cloudprovider_dbpass );
       
        // Show init-parameters to log files
        _log.info(
             LS + "Init parameters  "
           + LS + "---------------------------------------"
           + LS + "Grid operation: '" + GridOp        + "'"
           + LS + "CloudMgr host : '" + cloudmgrHost  + "'"
           + LS + "AliceGroupName: '" + AliceGroupName+ "'"
           + LS + "Aleph proxy   : '" + proxyFile     + "'"
           + LS + "portalSSHKey  : '" + portalSSHKey  + "'"
           + LS + "CA path       : '" + CAPath        + "'"
           + LS + "---------------------------------------"
           + LS + "[eTokenServer ]"
           + LS + "eTokenHost        : '" + eTokenHost         + "'"
           + LS + "eTokenPort        : '" + eTokenPort         + "'"
           + LS + "eTokenMd5Sum      : '" + eTokenMd5Sum       + "'"
           + LS + "eTokenVO          : '" + eTokenVO           + "'"
           + LS + "eTokenVOGroup     : '" + eTokenVOGroup      + "'"
           + LS + "eTokenProxyRenewal: '" + eTokenProxyRenewal + "'"
           + LS + "---------------------------------------"
           + LS + "[iservices]"
           + LS + "iservices_dbname : '" + iservices_dbname  + "'"
           + LS + "iservices_dbhost : '" + iservices_dbhost  + "'"
           + LS + "iservices_dbport : '" + iservices_dbport  + "'"
           + LS + "iservices_dbuser : '" + iservices_dbuser  + "'"
           + LS + "iservices_dbpass : '" + iservices_dbpass  + "'"
           + LS + "iservices_srvname: '" + iservices_srvname + "'"
           + LS + "---------------------------------------" 
           + LS + "[cloudprovider]"
           + LS + "cloudprovider_dbname : '" + cloudprovider_dbname  + "'"
           + LS + "cloudprovider_dbhost : '" + cloudprovider_dbhost  + "'"
           + LS + "cloudprovider_dbport : '" + cloudprovider_dbport  + "'"
           + LS + "cloudprovider_dbuser : '" + cloudprovider_dbuser  + "'"
           + LS + "cloudprovider_dbpass : '" + cloudprovider_dbpass  + "'"
           + LS + "---------------------------------------"
           + LS + "[guacamole]"
           + LS + "guacamole_dir      : '" + guacamole_dir       + "'"
           + LS + "guacamole_noauthxml: '" + guacamole_noauthxml + "'"
           + LS + "guacamole_page     : '" + guacamole_page      + "'"
           + LS + "---------------------------------------"
           + LS + "Preferences:"
           + LS + prefs.dump()
                 );

        // init iservices object
        iSrv = new iservices( iservices_srvname
                             ,iservices_dbname
                             ,iservices_dbhost
                             ,iservices_dbport
                             ,iservices_dbuser
                             ,iservices_dbpass
                            );
        if(iSrv.isEnabled()) {
            _log.info("Id for service '"+iservices_srvname+"' = "+iSrv.getServiceId());
            iSrv.initCloudMgr(proxyFile,CAPath,cloudmgrHost,eTokenHost,eTokenPort,
                              eTokenMd5Sum,eTokenVO,eTokenVOGroup,eTokenProxyRenewal.equals("true"));
            if(iSrv.isCloudMgrEnabled()) {
                   iSrv.initNoAuthConfigXML(guacamole_dir + java.io.File.separator + guacamole_noauthxml);
                   _log.info("CloudMgr is enabled");
            } else _log.info("CloudMgr not enabled");
        }
        // init cloudprovider object
        cloudProvider = new CloudProvider(cloudprovider_dbname
                                         ,cloudprovider_dbhost
                                         ,cloudprovider_dbport
                                         ,cloudprovider_dbuser
                                         ,cloudprovider_dbpass);
        if(cloudProvider.isEnabled())
             _log.info("Cloudprovider is enabled");
        else _log.info("Cloudprovider not enabled");
        
        
    } // init

    
    
    public void getPortletInfo(ActionRequest actionRequest,ResourceRequest resourceRequest,RenderRequest renderRequest) {
       
        
        System.out.println("aliceGridOperation-->"+aliceGridOperation);
        System.out.println("proxyFile-->"+proxyFile);
        System.out.println("portalSSHKey-->"+portalSSHKey);
        System.out.println("cloudmgrHost-->"+cloudmgrHost);
        System.out.println("AliceGroupName-->"+AliceGroupName);
        System.out.println("eTokenHost-->"+eTokenHost);
        System.out.println("eTokenPort-->"+eTokenPort);
        System.out.println("eTokenMd5Sum-->"+eTokenMd5Sum);
        System.out.println("eTokenVO-->"+eTokenVO);
        System.out.println("eTokenVOGroup-->"+eTokenVOGroup);
        System.out.println("eTokenProxyRenewal-->"+eTokenProxyRenewal);
        System.out.println("guacamole_dir-->"+guacamole_dir);
        System.out.println("guacamole_noauthxml-->"+guacamole_noauthxml);
        
        
        
        
        
       
        
        String portletInfo = LS + "------------------------------"
                           + LS + " portletInfo                  "
                           + LS + "------------------------------"
                           + LS;
        ThemeDisplay   themeDisplay   = null;
        User           user           = null;
        PortletSession portletSession = null;
        Company        company        = null;

        try {
            // Get request specific values
            if (null != actionRequest) {
                 _log.info("Caso1");
                themeDisplay = (ThemeDisplay)actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
                portletSession = actionRequest.getPortletSession();
                company = PortalUtil.getCompany(actionRequest);
                if(initialized) { prefs.setPortletPrefs(actionRequest); initialized = false; }
                else prefs.getPortletPrefs(actionRequest);
            } else if(null != resourceRequest) {
                _log.info("Caso2");
                themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(WebKeys.THEME_DISPLAY);
                portletSession = resourceRequest.getPortletSession();
                company = PortalUtil.getCompany(resourceRequest);
                if(initialized) { prefs.setPortletPrefs(resourceRequest); initialized = false; }
                else prefs.getPortletPrefs(resourceRequest);
            } else if(null != renderRequest) {
                _log.info("Caso3");
                themeDisplay = (ThemeDisplay)renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
                portletSession = renderRequest.getPortletSession();
                company = PortalUtil.getCompany(renderRequest);
                portalHost=themeDisplay.getPortalURL();
                if(initialized) {
                     _log.info("Caso4");
                    prefs.setPortletPrefs(renderRequest); initialized = false; }
                else prefs.getPortletPrefs(renderRequest);
            } else _log.error("Method getPortletInfo called with no valid requests!");
            
            // Retrieves portalHost, username & mail
            portalHost = themeDisplay.getPortalURL();
            portletPage= themeDisplay.getLayout().getName(themeDisplay.getLocale());
            user       = themeDisplay.getUser();
            userName   = user.getScreenName();
            firstName  = user.getFirstName();
            lastName   = user.getLastName();
            userMail   = user.getEmailAddress();
            portletInfo += ( "portalHost : '" + portalHost  + "'" + LS
                           + "portletPage: '" + portletPage + "'" + LS
                           + "User name  : '" + userName    + "'" + LS
                           + "First name : '" + firstName   + "'" + LS
                           + "Last name  : '" + lastName    + "'" + LS
                           + "User mail  : '" + userMail    + "'" + LS );
            // Retrieve user groups
            portletInfo+= "User's groups" + LS;
            long[] groups = user.getUserGroupIds();
            isAlephVMEnabled = false;
            for(int i=0; i<groups.length; i++) { 
                String groupName = UserGroupLocalServiceUtil.getUserGroup(groups[i]).getName();
                // Enable user to instantiate ALEPH VM 
                if(groupName.equalsIgnoreCase(AliceGroupName)) {
                    isAlephVMEnabled = true; // Enable user to instantiate ALEPH VM
                    portletInfo += "* Enabling ALEPH VM flag *";
                    // break # disabled so _log will contain all user groups
                }
                portletInfo += "  group: '" + UserGroupLocalServiceUtil.getUserGroup(groups[i]).getName() + "'" + LS;
            }
            // Retrieve user roles
            List<Role> roles = (List<Role>) RoleServiceUtil.getUserRoles(user.getUserId());
            portletInfo += "User's roles" + LS;
            for (Role role : roles)
                portletInfo += role.getName() + " -> "+ role.getRoleId() + LS;
            // Retrieves the application pathname
            PortletContext portletContext = portletSession.getPortletContext();
            appServerPath                 = portletContext.getRealPath("/");
            portletInfo += "App server path: '" + appServerPath + "'" + LS;
            // Retrieves portal name
            portalName = company.getName();

            // 
            // Update values from preferences (they could be changed)
            //
//            aliceGridOperation = Integer.parseInt(prefs.getPrefValue("GridOperation"));
//            proxyFile          = prefs.getPrefValue("proxyFile"          );
//            portalSSHKey       = prefs.getPrefValue("portalSSHKey"       );
//            cloudmgrHost       = prefs.getPrefValue("cloudmgrHost"       );
//            AliceGroupName     = prefs.getPrefValue("AliceGroupName"     );
//            eTokenHost         = prefs.getPrefValue("eTokenHost"         );
//            eTokenPort         = prefs.getPrefValue("eTokenPort"         );
//            eTokenMd5Sum       = prefs.getPrefValue("eTokenMd5Sum"       );
//            eTokenVO           = prefs.getPrefValue("eTokenVO"           );
//            eTokenVOGroup      = prefs.getPrefValue("eTokenVOGroup"      );
//            eTokenProxyRenewal = prefs.getPrefValue("eTokenProxyRenewal" );
//            guacamole_dir      = prefs.getPrefValue("guacamole_dir"      );
//            guacamole_noauthxml= prefs.getPrefValue("guacamole_noauthxml");
            // Show taken info
            
            System.out.println("PROXYFILE--->"+proxyFile+" CloudMng--->"+cloudmgrHost);

            
            portletInfo+= "portalSSHKey" +portalSSHKey+ LS;
            portletInfo += "------------------------------" + LS;
            _log.info(portletInfo);
        } catch (PortalException e) {
            _log.error("PortletInfo: got PortalException on actionRequest"+e.toString());
        } catch (SystemException e) {
            _log.error("PortletInfo: got SystemException on actionRequest"+e.toString());
        }
        // User services name
        String serviceInfo = "";
        iSrv.getAllocationInfo(userName);
        service_description = iSrv.getServiceShDesc();
        serviceInfo +=  LS + "--------------------------------------------------------"
                      + LS + "Allocation info for service: '" + iSrv.getServiceName() + "'"
                      + LS + "Service long description   : '" + service_description   + "'";
        if(0 >= iSrv.getNumAllocations())
             serviceInfo += LS + " No available service allocation for user '" + userName + "'";
        else serviceInfo += LS + iSrv.dumpAllocations();
        serviceInfo += LS + "--------------------------------------------------------";
        _log.info(serviceInfo);
    }

    
    
    

    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {


         getPortletInfo(request,null,null);

        // Determine the current portlet mode and forward this state to the response
        // Accordingly to JSRs168/286 the standard portlet modes are:
        // VIEW, EDIT, HELP
        // Supported values are referred in portlet.xml under the '<supports>' tag
        PortletMode mode = request.getPortletMode();
        response.setPortletMode(mode);
        //  _log.info("portletMode : '"+mode+"'");


        if (mode.equals(PortletMode.VIEW)) {

            // Retrieve the portlet action value, assigning a default value
            // if not specified in the Request (PortletStatus)
            String actionStatus = request.getParameter("PortletStatus");
            // Assigns the default ACTION mode

            
             //Uguale a 0 siamo nel caso RAA, uguale a 1 PtAnalisi 

            switch (Actions.valueOf(actionStatus)) {
                
                
                case ACTION_INPUT:
                   // _log.info("Got action: 'ACTION_INPUT'");
                    // Create the appInput object
                    //App_Input appInput = new App_Input();
                    // Assign the correct view
                    System.out.println("PROXYFILE--->"+proxyFile+" CloudMng--->"+cloudmgrHost);
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    break;

                case ACTION_PT_ANALISI:
                    System.out.println("Got action: 'ACTION_PT_ANALISI'");
                    // Create the appInput object
                    //App_Input appInput = new App_Input();
                    // Assign the correct view
                    String typeAnalisi_pt=request.getParameter("typeAnalisi_pt").toString();
                    
                    if (typeAnalisi_pt.equals("pp"))
                        typeAnalisi_pt="1";
                    else
                        typeAnalisi_pt="2";
                    String numFile_pt=request.getParameter("numFile_pt").toString();
                    
                    System.out.println("TYPE--->"+typeAnalisi_pt+" NUM_FILE-->"+numFile_pt);
                    System.out.println("PROXYFILE--->"+proxyFile+" CloudMng--->"+cloudmgrHost);

                    typeExperiment=1;
                    
                    occiSubmit(userName,firstName,lastName,portalName,typeExperiment,typeAnalisi_pt,numFile_pt);
                    
                   // response.setRenderParameter("typeExperiment",String.valueOf(typeExperiment));
                    
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    break;
                case ACTION_RAA_ANALISI:
                    System.out.println("Got action: 'ACTION_RAA_ANALISI'");
                    // Create the appInput object
                    //App_Input appInput = new App_Input();
                    // Assign the correct view
                    String min_centrality=request.getParameter("min_centrality").toString();
                    String max_centrality=request.getParameter("max_centrality").toString();
                    
                    System.out.println("MIN--->"+min_centrality+" MAX-->"+max_centrality);
                    
                    typeExperiment=0;
                    
                    occiSubmit(userName,firstName,lastName,portalName,typeExperiment,min_centrality,max_centrality);
                    
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    break; 
                    
                    case ACTION_VM:
                    System.out.println("Got action: 'ACTION_VM'");
                    // Create the appInput object
                    //App_Input appInput = new App_Input();
                    // Assign the correct view
                   
                    
                    occiSubmit(userName,firstName,lastName,portalName,2,null,null);
                    
                    typeExperiment=2;
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    break; 


            }




        }
    }

    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        response.setContentType("text/html");
        
 
        
        _log.info("Calling doView()");
        
        // Get portlet common information
        getPortletInfo(null,null,request);
        
        // currentView comes from the processAction; unless such method
        // is not called before (example: page shown with no user action)
        // VIEW_MAIN will be selected as default view
        String currentView=request.getParameter("PortletStatus");
        if(currentView==null) currentView="VIEW_INPUT";
        
        
        switch(Views.valueOf(currentView)) {
            // The following code is responsible to call the proper jsp file
            // that will provide the correct portlet interface
            case VIEW_INPUT: {
               // _log.info("VIEW_MAIN Selected ...");
 //               request.setAttribute("param"           , param             );
                request.setAttribute("portalHost"      , portalHost        );
                request.setAttribute("portletPage"     , portletPage       );  
                request.setAttribute("iSrv"            ,iSrv               );
                request.setAttribute("serviceDesc"     ,service_description);
                request.setAttribute("guacamole_page"  ,guacamole_page     );
                request.setAttribute(  "typeExperiment", String.valueOf(typeExperiment));
                request.setAttribute("isAlephVMEnabled",isAlephVMEnabled   );
                PortletRequestDispatcher dispatcher=getPortletContext().getRequestDispatcher("/alice_portlet_view.jsp");
                dispatcher.include(request, response);
            }
        
        }
        
        
        
        
        
    }

    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        response.setContentType("text/html");
        PortletRequestDispatcher dispatcher =
                getPortletContext().getRequestDispatcher("/alice_portlet_edit.jsp");
        dispatcher.include(request, response);
    }

    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {

        response.setContentType("text/html");
        PortletRequestDispatcher dispatcher =
                getPortletContext().getRequestDispatcher("alice_portlet_help.jsp");
        dispatcher.include(request, response);
    }
    
    
    private enum serveCommands {
        none          // Unhandled command
       ,submit        // Submit the job
       ,allocinfo     // Retrieve allocation info
       ,notify        // Notify to portlet
         // back to default preference values
       ,test          // Tester command
    };
    
    
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) 
    throws PortletException, IOException {
        _log.info("Calling serverResource()");

        // Get portlet common information
        getPortletInfo(null,request,null);

        // Retrieve portletPreferences (not used yet)
        // PortletPreferences portletPreferences = (PortletPreferences) request.getPreferences();

        Map obj=new LinkedHashMap();                                         // Used to prepare JSON output 
        String commandParameters = "";                                       // Used to log commands input parameter
        String commandValue = (String) request.getParameter("command");      // Received command
        if(commandValue == null) commandValue="none";                        // Set unhandled command if no command received

        // Switch among possible commands received from the js
        switch(serveCommands.valueOf(commandValue)) {
            case test:
                // Retrieve command parameters
                String testParamValue1 = request.getParameter("testParam1");
                String testParamValue2 = request.getParameter("testParam2");
                // Prepare log parameter output
                obj.put("testParamValue1",testParamValue1);
                obj.put("testParamValue2",testParamValue2);
                commandParameters = "testParamValue1: '" + testParamValue1 + "'" 
                            + LS  + "testParamValue2: '" + testParamValue2 + "'";
                // Do something with parameters
                // ...
                // Prepare JSON ouptut
                obj.put("commandRes","OK");
                obj.put("testParamValue1",testParamValue1);
                obj.put("testParamValue2",testParamValue2);
            break;

            

            

            case submit:
                 // Retrieve command parameters
                // String alephfile = request.getParameter("aleph_file");
                 //String alephAlg  = request.getParameter("aleph_alg");
//                 _log.info(      "Submit:" 
//                          + LS + "=======" 
//                          + LS + "Aleph file:      '" + alephfile + "'"
//                          + LS + "Aleph algorithm: '" + alephAlg  + "'"
//                          );
                 // Services may be installed only having iSrv class enabled
                 if(!iSrv.isEnabled()) {
                   obj.put("commandRes" ,"KO");
                   obj.put("commandInfo","Service: '"+iSrv.getServiceName()+"' cannot be instantiated because iservice class is not available. Please contact the administrator.");
                 }
                 // In case of new service check for its maximum allowed allocations number
                 else if( iSrv.getNumAllocations() == iSrv.getMaxAllowedAllocations()) {
                   obj.put("commandRes" ,"KO");
                   obj.put("commandInfo","Reached maximum allowed allocations for service: '"+iSrv.getServiceName()+"'");
                 }
                 // Otherwise submit the job
                 else {                       
                    occiSubmit(userName,firstName,lastName,portalName,2,null,null); 
                   obj.put("commandRes","OK");
                 }
            break;

            case allocinfo:
                 if(iSrv.isEnabled()) {
                   LinkedList allocList = new LinkedList();
                   for(int i=0; i<iSrv.getNumAllocations(); i++) {
                       // Get values from iservices
                       iservices.AllocInfo alliV[] = iSrv.allocInfo;
                       // Put values into JSON elemen
                       Map allocEntry = new LinkedHashMap();
                       allocEntry.put("allocTs"   ,""+alliV[i].getAllocTs   ());
                       allocEntry.put("allocExpTs",""+alliV[i].getAllocExpTs());
                       allocEntry.put("allocState",   alliV[i].getAllocState());
                       allocEntry.put("allocId"   ,   alliV[i].getAllocId   ());
                       allocEntry.put("srvUUID"   ,   alliV[i].getSrvUUID   ());
                       // accInfo
                       LinkedList accessList = new LinkedList();
                       if(   iSrv.allocInfo[i].accInfo != null 
                          && iSrv.allocInfo[i].accInfo.length > 0) {
                           for(int j=0; j<iSrv.allocInfo[i].accInfo.length; j++) {
                               // Get values
                               iservices.AccessInfo[] acciV = iSrv.allocInfo[i].getAccInfo();
                               // Put values
                               Map accessEntry = new LinkedHashMap();
                               accessEntry.put("ip"       ,acciV[j].getIP       ());
                               accessEntry.put("workgroup",acciV[j].getWorkGroup());
                               accessEntry.put("username" ,acciV[j].getUserName ());
                               accessEntry.put("password" ,acciV[j].getPassword ());
                               accessEntry.put("port"     ,acciV[j].getPort     ());
                               accessEntry.put("proto"    ,acciV[j].getProto    ());
                               accessList.add(accessEntry);
                           }
                       }  
                       allocEntry.put("accInfo", accessList);
                       allocList.add(allocEntry);
                   }
                   obj.put("commandRes","OK");
                   obj.put("allocInfo",allocList);
                 }
                 else {
                   _log.info("Ignoring allocinfo command since iservices class is not enabled");
                   obj.put("commandRes","KO");
                 }
            break;

            case notify:
                 // Something has to be notified to the portlet
                 String sender   = request.getParameter("sender");
                 String keyname  = request.getParameter("keyname");
                 String keyvalue = request.getParameter("keyvalue");
                 if (keyname.equalsIgnoreCase("ipaddr")) {
                   _log.info("Retrieving notification from '"+sender+"': "+keyname+"='"+keyvalue+"'");
                 }
                 else _log.info("Ignored notification message: '"+keyname+"='"+keyvalue+"' from: '"+sender+"'");
            break;

            // default condition does not work since null commands are generating an
            // exception on request.getParameter('command') call
            // the warning below could be replaced by a dedicated catch condition 
            default:
                 _log.warn("Unhandled command: '"+commandValue+"'");
                 return;
        } // swicth

        // Set the content type
        response.setContentType("application/json");

        // Prepare JSON Object
        obj.put("commandValue",commandValue);
        response.getPortletOutputStream().write(JSONValue.toJSONString(obj).getBytes());

        // Show taken parameters
        _log.info(
             LS + "Command: '" + commandValue +"'"
           + LS + "------------------------------"
           + LS + "Parameters:                   "
           + LS + "------------------------------"
           + LS + commandParameters
           + LS + "------------------------------"
           + LS + "JSON output:                  "
           + LS + "------------------------------"
           + LS + JSONValue.toJSONString(obj));
    }
    
    
    
    
     public int occiSubmit(String username, String firstName, String lastName, String portalname,int typeExperiment, String param1, String param2) {
        // Retrieve the full  path to the job directory
         
         System.out.println("aliceGridOperation-->"+aliceGridOperation);
        System.out.println("proxyFile-->"+proxyFile);
        System.out.println("portalSSHKey-->"+portalSSHKey);
        System.out.println("cloudmgrHost-->"+cloudmgrHost);
        System.out.println("AliceGroupName-->"+AliceGroupName);
        System.out.println("eTokenHost-->"+eTokenHost);
        System.out.println("eTokenPort-->"+eTokenPort);
        System.out.println("eTokenMd5Sum-->"+eTokenMd5Sum);
        System.out.println("eTokenVO-->"+eTokenVO);
        System.out.println("eTokenVOGroup-->"+eTokenVOGroup);
        System.out.println("eTokenProxyRenewal-->"+eTokenProxyRenewal);
        System.out.println("guacamole_dir-->"+guacamole_dir);
        System.out.println("guacamole_noauthxml-->"+guacamole_noauthxml);
         
         
         
        String jobPath = appServerPath+"WEB-INF/job/";
        String rOCCIResourcesList[] = null;

        Boolean useCloudProviderInfrastructure = false; 
        InfrastructureInfo infrastructure;  
        MultiInfrastructureJobSubmission mijs = new MultiInfrastructureJobSubmission();
         
        String GE_JobId=null;
        if(typeExperiment==0)
            GE_JobId="alice:RAA";
        if(typeExperiment==1)
            GE_JobId="alice:Pt";
        if(typeExperiment==2)
            GE_JobId="alice:";
        
        
        if(    cloudProvider != null
            && cloudProvider.isEnabled() 
            && cloudProvider.getProviderList(aliceGridOperation) > 0) {
            if(!useCloudProviderInfrastructure) {
                _log.info("Submission aided by CloudProvider - Using the resourceList method (no Infrastructures)");
                rOCCIResourcesList = cloudProvider.getResourcesList();
                // Retrieve preference settings first
//                eTokenHost         = prefs.getPrefValue("eTokenHost"         );
//                eTokenPort         = prefs.getPrefValue("eTokenPort"         );
//                eTokenMd5Sum       = prefs.getPrefValue("eTokenMd5Sum"       );
//                eTokenVO           = prefs.getPrefValue("eTokenVO"           );
//                eTokenVOGroup      = prefs.getPrefValue("eTokenVOGroup"      );
//                eTokenProxyRenewal = prefs.getPrefValue("eTokenProxyRenewal" );
                // Prepare the GridEngine' InfrastructureInfo object
                infrastructure = new InfrastructureInfo( "GridCT"           // Infrastruture name
                                                        ,"rocci"             // Adaptor
                                                        ,""                  //
                                                        ,rOCCIResourcesList  // Resources list
                                                        ,eTokenHost          // eTokenServer host
                                                        ,eTokenPort          // eTokenServer port
                                                        ,eTokenMd5Sum        // eToken id (md5sum)
                                                        ,eTokenVO            // VO
                                                        ,eTokenVOGroup       // VO.group.role
                                                        ,true                // ProxyRFC
                                                       );
                
                
                
                
                
                // Add infrastructure to an array of infrastructures and add them to MultiInfrastructureJobSubmission object
                mijs.addInfrastructure(infrastructure);
            }
            
        }
        /*else {
          // Setup CloudProvider object for nebula-server-01
          CloudProvider cp1 = new CloudProvider("nebula-server-01"
                                               ,"nebula-server-01.ct.infn.it"
                                               ,9000
                                               ,"rocci"
                                              );
          // Assign OCCI parameters to the CloudProvider object
          String params1[][] = {
              { "resource"          , "compute"                   }
             ,{ "action"            , "create"                    }
             ,{ "attributes_title"  , "aleph2k"                   }
             ,{ "mixin_os_tpl"      , "uuid_aleph2000_vm_71"      }
             ,{ "mixin_resource_tpl", "small"                     }
             ,{ "auth"              , "x509"                      }
                 
          // ,{ "publickey_file"    , jobPath + "/home/.ssh/id_dsa.pub" } // (!) UNUSED; GE uses: $HOME/.ssh/id_rsa.pub
          // ,{ "privatekey_file"   , jobPath + "home/.ssh/id_dsa"     } // (!) UNUSED; GE uses: $HOME/.ssh/id_rsa
          };
          // Add OCCI parameters to the cloud provider object
          cp1.addParams(params1);

          
          //*****HO COMMENTATO QUESTA parte PER PROVARE I PARAMETRI MANDATI DA GIUSEPPE
          // Setup CloudProvider object for stack-server-01
//          CloudProvider cp2 = new CloudProvider("stack-server-01"
//                                               ,"stack-server-01.ct.infn.it"
//                                               ,8787
//                                               ,"rocci"
//                                              );
//          // Assign OCCI parameters to the CloudProvider object
//          String params2[][] = {
//              { "resource"          , "compute"                              }
//             ,{ "action"            , "create"                               }
//             ,{ "attributes_title"  , "aleph2k"                              }
//             ,{ "mixin_os_tpl"      , "c3484114-9c67-44ff-a3da-ea9e6058fe3b" }
//             ,{ "mixin_resource_tpl", "m1-large"                             }
//             ,{ "auth"              , "x509"                                 }
//          };
          //******FINE COMMENTO
          
          CloudProvider cp2 = new CloudProvider("stack-server-01"
                                               ,"stack-server-01.ct.infn.it"
                                               ,8787
                                               ,"rOCCI"
                                              );
          // Assign OCCI parameters to the CloudProvider object
          String params2[][] = {
              { "resource"          , "compute"                              }
             ,{ "action"            , "create"                               }
             ,{ "attributes_title"  , "aleph2k"                              }
             ,{ "mixin_os_tpl"      , "f52962a3-6f32-433a-ac55-9b4e9908172d" }
             ,{ "mixin_resource_tpl", "aleph"                             }
             ,{ "auth"              , "x509"                                 }
          };
          
          
          
          
          

          // Add OCCI parameters to the cloud provider object
          cp2.addParams(params2);


          // Retrieve from cloud provider objects the OCCI endpoints
          String rOCCIURL1 = cp1.endPoint();  _log.info("OCCI Endpoint1: '" + rOCCIURL1 + "'");
          String rOCCIURL2 = cp2.endPoint();  _log.info("OCCI Endpoint2: '" + rOCCIURL2 + "'");

          // Prepare the ROCCI resource list
          String resList[] = { //rOCCIURL1
                            rOCCIURL2
                             };
          rOCCIResourcesList = resList;

          // Prepare the GridEngine' InfrastructureInfo object
          infrastructure = new InfrastructureInfo( "GridCT"                          // Infrastruture name
                                                 ,"rocci"                           // Adaptor
                                                 ,""                                //
                                                 ,rOCCIResourcesList                // Resources list
                                                 ,"etokenserver.ct.infn.it"         // eTokenServer host
                                                 ,"8082"                            // eTokenServer port
                                                 ,"bc779e33367eaad7882b9dfaa83a432c"// eToken id (md5sum)
                                                 ,"fedcloud.egi.eu"                 // VO
                                                 ,"fedcloud.egi.eu"                 // VO.group.role
                                                 ,true                              // ProxyRFC
                                                 );
          // Add infrastructure to an array of infrastructures and add them to MultiInfrastructureJobSubmission object
          mijs.addInfrastructure(infrastructure);
        } */

        // Cloud job requires a valid proxy to operate
        // The eTokenserver proxy will be included into the inputSandbox
        
        iSrv.getRobotProxyFile("etokenserver.ct.infn.it"
                              ,"8082"
                              ,"bc779e33367eaad7882b9dfaa83a432c"
                              ,"fedcloud.egi.eu"
                              ,"fedcloud.egi.eu"
                              ,"true"
                              ,proxyFile);

//        // Build the job identifier
         
         
        
        
        
//        String[] alephfile_path = { "" };
//        String alephfileName = "";
        int vmduration = -1;
//        if (null != alephfile) {
//          alephfile_path = alephfile.split("/");            
//          alephfileName = alephfile_path[alephfile_path.length-1];
//        }
        String vmuuid = "";
//        String GE_JobId = "aleph: ";
       if(isAlephVMEnabled && typeExperiment==2) {
//             String moreInfo = "";
//             if(!alephfileName.equals(""))
//                 moreInfo = "'"+alephfile+"'"; 
           
           System.out.println("CASO VM SINGOLA");
             vmuuid     = iSrv.getUUID();
             vmduration = iSrv.getServiceDuration(); 
             GE_JobId =GE_JobId+ "VM--"+ vmuuid + " ";
             
             mijs.setExecutable("alice.sh");                              // Executable
            mijs.setArguments(  typeExperiment + " " +
                                param1 + " "+
                                param2+ " "+
                            proxyFile                          + " "   //   proxy certificate file (having full path)
                         +portalSSHKey                       + " "   //   portal public SSH key (having full path)
                         +portalHost                         + " "   //   portlal host (needed to get notify) 
                         +username                           + " "   //   portal username
                    +"\""+firstName                          + "\" " //   portal user first name
                   +"\""+lastName                           + "\" " //   portal user last name
                        +userMail                           + " "   //   portal user email address
                         +cloudmgrHost                       + " "   //   cloudmgr contacting URL
                    +"\""+vmuuid                             + "\" " //   VM UUID
//                         +(null==alephAlg?"\"\"":alephAlg)   + " "   //   ALEPH analisys application (MITQCD,6LEP,...)
                    +"\""+portalname                         + "\" " //   portal name
                         +vmduration                         + " "   //   VM duration in seconds
                    +"\""+GE_JobId                           + "\" " //   the job identifier
                         ); 
        mijs.setJobOutput("stdout.txt");                             // std-output
        mijs.setJobError("stderr.txt");                              // std-error
        mijs.setOutputPath("/tmp/");                                 // Output path
        mijs.setInputFiles(jobPath+"alice.sh" + "," +              // Aleph pilot script
                           proxyFile          + "," +                // proxy file full path
                           portalSSHKey                              // portal public ssh key
                          );                                         // InputSandbox
        mijs.setOutputFiles("alice_output.tar");  
             
             
             
             
             
        }
//        else GE_JobId = "'" + alephfileName + "'";
//        
//        // Set job properties
       
       else{
           
           System.out.println("CASO JOB");
       
        mijs.setExecutable("alice.sh");                              // Executable
        mijs.setArguments(  typeExperiment + " "                                         // Arguments
                            +param1 + " "
                            +param2+ " "
//         
                            +proxyFile                          + " "   //   proxy certificate file (having full path)
                         +portalSSHKey                       + " "   //   portal public SSH key (having full path)
                         +portalHost                         + " "   //   portlal host (needed to get notify) 
                         +username                           + " "   //   portal username
                    +"\""+firstName                          + "\" " //   portal user first name
                   +"\""+lastName                           + "\" " //   portal user last name
                        +userMail                           + " "   //   portal user email address
                         +cloudmgrHost                       + " "   //   cloudmgr contacting URL
                    +"\""+vmuuid                             + "\" " //   VM UUID
//                         +(null==alephAlg?"\"\"":alephAlg)   + " "   //   ALEPH analisys application (MITQCD,6LEP,...)
                    +"\""+portalname                         + "\" " //   portal name
                         +vmduration                         + " "   //   VM duration in seconds
                    +"\""+GE_JobId                           + "\" " //   the job identifier
                         ); 
        mijs.setJobOutput("stdout.txt");                             // std-output
        mijs.setJobError("stderr.txt");                              // std-error
        mijs.setOutputPath("/tmp/");                                 // Output path
        mijs.setInputFiles(jobPath+"alice.sh" + "," + jobPath+"analisi.sh"+ ","+               // Aleph pilot script
                           proxyFile          + "," +                // proxy file full path
                           portalSSHKey                              // portal public ssh key
                          );                                         // InputSandbox
        mijs.setOutputFiles("alice_output.tar");                     // OutputSandbox
        
       }
//          
//        // Determine the host IP address
       String   portalIPAddress="";                
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipAddr=addr.getAddress();
            portalIPAddress= ""+(short)(ipAddr[0]&0xff)
                           +":"+(short)(ipAddr[1]&0xff)
                           +":"+(short)(ipAddr[2]&0xff)
                           +":"+(short)(ipAddr[3]&0xff);
        }
        catch(Exception e) {
            _log.error("Unable to get the portal IP address");
        } 

        _log.info("ARGOMENTI--->"+mijs.getArguments());
        // Submit the job     
        // Submission uses addInfrastructure method; this call is no longer necessary
        // mijs.submitJobAsync(infrastructure, username, portalIPAddress, alephGridOperation, GE_JobId);
        mijs.submitJobAsync(username, portalIPAddress, aliceGridOperation, GE_JobId);

        // Remove proxy temporary file
        // temp.delete(); Cannot remove here the file, job submission fails
        
        // Interactive job execution (iservices)
        if(isAlephVMEnabled && typeExperiment==2) {
            iSrv.allocService(username,vmuuid);
            iSrv.dumpAllocations();
        }
        return 0;
    }
   
    
}
