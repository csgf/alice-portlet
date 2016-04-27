<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jstl/core_rt" prefix="c"%>
<%@page import="javax.portlet.*"%>
<%@taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>

<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<jsp:useBean id="portalHost"        class="java.lang.String"     scope="request"/>
<jsp:useBean id="portletPage"       class="java.lang.String"     scope="request"/>

<jsp:useBean id="typeExperiment" class="java.lang.String" scope="request"/>
<jsp:useBean id="serviceDesc"       class="java.lang.String"     scope="request"/>
<jsp:useBean id="guacamole_page"    class="java.lang.String"     scope="request"/>
<jsp:useBean id="isAlephVMEnabled"  class="java.lang.Boolean"    scope="request"/>

<portlet:defineObjects />
<%          PortletPreferences prefs = renderRequest.getPreferences();
            String tabNames = "Analize,VM Login";

            String tabs1 = ParamUtil.getString(request, "tabs1", "Analize");
            PortletURL url = renderResponse.createRenderURL();
            pageContext.setAttribute("tabs1", tabs1);
            
            
            System.out.println("TYPEEXP:--->"+typeExperiment);
            System.out.println("serviceDesc--->"+serviceDesc);
            System.out.println("portalHost--->"+portalHost);
            
            
                      
%>

<script type="text/javascript">
    
    function submitVM() {
        
        
        alert("<%=portalHost%>/web/guest/alice/-/alice_portlet/json" + "?");
        
        alert("<%=portalHost%>/web/guest/alice/-/alice_portlet/json" + '?' + $.param({ command: 'submit' }));
        if(confirm("Are you sure to submit the ALICE VM?") == true) {
          $.ajax({
              type: "GET",
              cache: false,
              crossDomain: true,
              dataType: "json",
              url:  '<%=portalHost%>/web/guest/alice/-/alice_portlet/json' + '?' + $.param({ command: 'submit' }),
             // url:  '<%=portalHost%>/<%=portletPage%>/-/aleph/json' + '?' + $.param({ command: 'submit' }),
              success: function(data){
                  if(data.commandRes == "OK")
                    alert("An instance of ALICE VM is being started; you will be notified by email as soon as the machine will be available. This operation may require some time to complete.");
                  else alert("FAILED: "+data.commandInfo);
              },
              error: function (xhr, ajaxOptions, thrownError) {
                  console.log(xhr.status);
                  console.log(thrownError);
                  console.log(xhr.responseText);
                  console.log(xhr);
              }
          }); // ajax
        } 
    }
    
    $.getJSON('<%=portalHost%>/web/guest/alice/-/alice_portlet/json?command=allocinfo', function(json) {
   // $.getJSON('<%=portalHost%>/<%=portletPage%>/-/aleph/json?command=allocinfo', function(json) {
        var s="";
        for(i=0; i<json.allocInfo.length; i++){
            var id = json.allocInfo[i].allocId;
            if (json.allocInfo[i].allocState == "RUNNING"){
            var status =  "<img src=<%=renderRequest.getContextPath()%>/images/vm-run.png width=48px height=48px>";
                }
               else 
                   {
                   var status =  "<img src=<%=renderRequest.getContextPath()%>/images/standby.png width=48px height=48px>";
                   }
            var srvUUID = json.allocInfo[i].srvUUID;
            var data1= json.allocInfo[i].allocTs;
            var data2= json.allocInfo[i].allocExpTs;
            //ssh url 
            // http://90.147.74.76:8080/guacamole-0.9.1/client.xhtml?id=c%2Fssh%3A%2F%2Falephusr%4090.147.74.85%3A22
            // Nota che id e' la stringa che si costruisce dai dati di accesso: <proto>://<username>@<IP>:<port>
            if(json.allocInfo[i].accInfo != null && json.allocInfo[i].accInfo.length > 0){
        s += "<tr><td><a class='view-more-info'><img src=<%=renderRequest.getContextPath()%>/images/moreinfo.png ></a></td><td>"  + srvUUID +  "</td><td>" + data1 +"</td><td class='status'>" + status + "</td>";
                var url = ""; 
                for(j=0; j<json.allocInfo[i].accInfo.length; j++){
                    var ip= json.allocInfo[i].accInfo[j].ip;
                    var workgroup =  json.allocInfo[i].accInfo[j].workgroup;
                    var username = json.allocInfo[i].accInfo[j].username;
                    var password = json.allocInfo[i].accInfo[j].password ;
                    var port = json.allocInfo[i].accInfo[j].port ;
                    var proto = json.allocInfo[i].accInfo[j].proto;
                    if (proto == 'ssh')
                        url += "<a href=<%=portalHost%>/<%=guacamole_page%>/#/client/c/ssh_" + srvUUID + " target=\"_blank\"><img src=<%=renderRequest.getContextPath()%>/images/ssh.png></a>";
                    if(proto == 'vnc')
                        url += "<a href=<%=portalHost%>/<%=guacamole_page%>/#/client/c/vnc_" + srvUUID + " target=\"_blank\"><img src=<%=renderRequest.getContextPath()%>/images/vnc.png></a>";
                }
                s += "<td>"+ url +"</td>";
                s += "</tr>";
                s += "<tr class=moreinfo><td colspan=5>Additional information<br>ip:" + ip  + "<br>expiration date:" + data2 +"<br>username:" + username+ "</td></tr>";
             }
             else 
             { s += "<tr><td></td><td>"  + srvUUID +  "</td><td>" + data1 +"</td><td class='status'>" + status + "</td><td></td></tr>";}
        }
        if (s !=null  && s!=""){ 
            $('.tr').append(s);
        }
        else {
            s = "<tr><td></td><td></td><td></td><td></td><td></td></tr>";
            $('.tr').append(s);
        }
        $('.view-more-info').click(function(){
                 $(this).closest('tr').next('tr').toggle();
        });
    }); 
    
    
     $(document).ready(function() {
    	
        $(".tabs-menu a").click(function(event) {
            event.preventDefault();
            $(this).parent().addClass("current");
            $(this).parent().siblings().removeClass("current");
            var tab = $(this).attr("href");
            $(".tab-content").not(tab).css("display", "none");
            $(tab).fadeIn();
        });
    
    
        $('#typeAnalisi_pt').change(function(){
            var x=document.getElementById("typeAnalisi_pt"); 
            var str = x.options[x.selectedIndex].text;
            //alert(str);
            var numFile_pt=document.getElementById("numFile_pt");
            if(str=="PbPb 3.5 TeV"){
            
                numFile_pt.value="1";
                numFile_pt.options[2].style.display='block';

            }
            else{
                numFile_pt.value="1";
                numFile_pt.options[2].style.display='none'; 
            }
        
        });
        
        
        
        <% if(typeExperiment.equals("0"))
            {%>
               $('#tab-1').css( "display", "block" );
               $('#tab-2').css( "display", "none" );   
               $('#liTAB1').addClass("current");
               $('#liTAB2').removeClass("current");
               
           <%}if(typeExperiment.equals("1")) {%>
                
               $('#tab-1').css( "display", "none" );
               $('#tab-2').css( "display", "block" ); 
               $('#liTAB1').removeClass("current");
               $('#liTAB2').addClass("current");
               
            <% } %>
        

    });   

 function submitPT(){
 	 alert("Analysis job successfully submitted; click on MyJobs to check its status");
 }
 
 function submitRAA(){
 	
 	var min_c;
 	var max_c;
 	
 	//alert("MIN: "+document.getElementById("min_c").value);
 	
 	if(document.getElementById("min_c").value == "")
 	   	min_c="NULL";
 	else
 	   	min_c=parseInt(document.getElementById("min_c").value);
 	   	
 	if(document.getElementById("max_c").value == "")
 	   	max_c="NULL";
 	else   	
 	   	max_c=parseInt(document.getElementById("max_c").value);   
	
	//alert(min_c+"  "+max_c);
	 
	 
	 
	 if(min_c=="NULL" || max_c=="NULL" || min_c>100 || max_c>100 || min_c<0 || max_c<0 ){
    	alert("Warning : You have chosen not valid values");
	}
	else{
    	if(min_c >= max_c)
    	{
        	alert("Warning : You have chosen a mininum value greater than the maximum !");
        
    	}
    	else{

     		alert("Analysis job successfully submitted; click on MyJobs to check its status");
     		document.forms["search_formRAA"].submit();
     
			}  
}
	 
	 
	
 }
    
    
    
    
    
</script>




<portlet:defineObjects />

<liferay-ui:tabs
names="<%= tabNames%>"
url="<%= url.toString()%>"
    />


<c:choose>
    <c:when test="${tabs1 == 'Analize'}" > 

        <div id="tabs-container">
            <ul class="tabs-menu">
                
               
                
                <li id="liTAB1"class="current"><a href="#tab-1">RAA2</a></li>
                <li id="liTAB2"><a href="#tab-2">Pt Analysis</a></li>
             
                
            </ul>
            <div class="tab">
                <div id="tab-1" class="tab-content" >
                    <form id="search_formRAA" action="<portlet:actionURL portletMode="view"><portlet:param name="PortletStatus" value="ACTION_RAA_ANALISI"/></portlet:actionURL>" method="post">    



                        <table class="tableAnalysis" >

                            <tr>
                                <td colspan="2"> <p>Welcome to RAA Analysis!</p>

                                    <p>In this frame you should set the centrality range for RAA analysis, then start the analysis using the start button below.</p>
                                    <hr>  </td>
                            </tr>


                            <tr>
                                <td>

                                    <label> Minimum Centrality:  <input id="min_c" type="number" required="true"  value="0" min="0" max="100" step="1" name="min_centrality"  oninput="validate(this)" /> </label>
                                    <br>
                                </td>

                                <td>

                                    <label> Maximum Centrality:  <input id="max_c" type="number" required="true" value="5" min="0" max="100" step="1" name="max_centrality" oninput="validate(this)"/> </label>
                                    <br>
                                </td>

                            </tr>   



                            <tr>
                                <td colspan="2">
                                    <input type="button" value="Start Analysis"  onclick="submitRAA()" />
                                </td>
                            </tr>

                        </table>  



                    </form> 
                </div>

                <div id="tab-2" class="tab-content" >
                    <form id="search_formPT" action="<portlet:actionURL portletMode="view"><portlet:param name="PortletStatus" value="ACTION_PT_ANALISI"/></portlet:actionURL>" method="post">    

                        <table class="tableAnalysis">
                            <tr>
                                <td colspan="2"> <p>Welcome to PT Analysis!</p>

                                    <p>In this frame you should select the dataset to use (pp or PbPb) and
                                        the number of files to be processed, then start the analysis using the start button below.</p>
                                    <hr>  </td>
                            </tr>
                            <tr>
                                <td> Type Analysis <select id="typeAnalisi_pt" name="typeAnalisi_pt" >
                                        <option value="pp">pp 3.5 TeV</option> <!--la Pp può avere al max 2 file -->
                                        <option value="PbPb">PbPb 3.5 TeV</option> <!--la PbPb può avere al max 3 file -->
                                    </select> </td>
                                <td> Number of files
                                    <select id="numFile_pt" name="numFile_pt" >
                                        <option value="1">1</option>
                                        <option value="2">2</option>
                                        <option value="3" style="display:none">3</option>


                                    </select> </td>

                            </tr>
                            <tr>
                                <td colspan="2"> <input type="submit"  name="buttonAnalize" value="Start Analysis"  onclick="submitPT()"/> </td>
                            </tr>
                        </table>


                    </form>
                </div>
            </div>
        </div>
    </c:when>
    
    <c:when test="${tabs1 == 'VM Login'}" > 
        <p><%=serviceDesc%></p>
        <% if(isAlephVMEnabled) { %>
        
        <p>By clicking on 'Start new VM' button you will create a new ALICE virtual machine that you can access for about 72 hours. You will be notified via e-mail about the necessary credentials to access the newly instatiated VM. You cannot start more than one ALICE instance until the ALICE VM expires.</p>
        <a href="#" onclick="submitVM()"><img src="<%=renderRequest.getContextPath()%>/images/login_vm.png"></a>
        <h3>- VM INFO -</h3>
        <table id="vminfo" class="gradient-style">
            <thead>
                <tr>
                    <th></th> 
                     <th>
                        Server UUID
                    </th>
                    <th>
                        StartTime
                    </th>
                    <th>
                        Status
                    </th>
                    <th>
                        Connection
                    </th>

                </tr>
            </thead>
            <tbody class="tr">
            </tbody>
        </table>
        
        <% } else {%>
            <p>It seems that you don't have the rights to execute an ALICE Virtual Machine. If you are interested in accessing the ALICE VM, please contact the site <a href="mailto:credentials@ct.infn.it"><b>administrator</b></a>.</p>
        <a href="#"><img src="<%=renderRequest.getContextPath()%>/images/no-login.png"></a>
            <% } %>
        </c:when>  
    
    
    <c:otherwise>
        No tabs
    </c:otherwise>
</c:choose>

