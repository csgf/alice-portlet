#!/bin/bash

export MYPROXY_SERVER=myproxy.cineca.it
export X509_CERT_DIR=/etc/grid-security/certificates


# simple URL encode function
urlencode() {
 echo "${1}" | sed s/' '/'%20'/g | sed s/':'/'%3A'/g | sed s/"'"/'%27'/g 
}

# calc duration
duration() {
  DURATION=$1

  SEC=$((DURATION%60))
  MIN=$(((DURATION/60)%60))
  HOR=$(((DURATION/(60*60))%24))
  DYS=$(((DURATION/(60*60*24))%31))
  MTS=$(((DURATION/(60*60*24*31))%12))

    echo $MTS Months
    echo $DYS Days
    echo $HOR Hours
    echo $MIN Minutes
    echo $SEC Seconds
}



TYPE_EXPERIMENT=$1
PARAM1=$2
PARAM2=$3
PROXY_FILE=$(basename "${4}")
PORTAL_SSHPUBKEY=$(basename "${5}")
PORTAL_HOST="${6}"
PORTAL_USER="${7}"
PORTAL_USERFIRSTNAME="${8}"
PORTAL_USERLASTNAME="${9}"
PORTAL_USRMAIL="${10}"
CLOUDMGR_URL="${11}"
VMUUID="${12}"
PORTAL_NAME="${13}"
VMDURATION="${14}"
JOB_ID="${15}"
VMIPADDR=$(ifconfig | grep 'inet addr:' | head -n 1 | awk '{ print $2 }' | awk -F':' '{ print $2 }')
EXECUSR=adminuser
EXECHST=localhost
VM_OUTPUT=alice.output
VMLOOPCTRL=/tmp/vmloop.ctrl
VMLOOPCOUNT=/tmp/vmloop.count
VMDURATIONSTR=$(duration $VMDURATION)


echo "--------------------------------------------"
echo "This is the execution of ALICE job execution"
echo "--------------------------------------------"

echo "PROXY_FILE       = ${PROXY_FILE}"
echo "PORTAL_SSHPUBKEY = ${PORTAL_SSHPUBKEY}"
echo "PORTAL_HOST      = ${PORTAL_HOST}"
echo "VM IP ADDR       = ${VMIPADDR}"
echo "PORTAL_USER      = ${PORTAL_USER}"
echo "PORTAL_USRFNAME  = ${PORTAL_USERFIRSTNAME}"
echo "PORTAL_USRLNAME  = ${PORTAL_USERLASTNAME}"
echo "PORTAL_USRMAIL   = ${PORTAL_USRMAIL}"
echo "PORTAL_NAME      = ${PORTAL_NAME}"
echo "CLOUDMGR         = ${CLOUDMGR_URL}"
echo "VMUUID           = ${VMUUID}"

echo "VMDURATION       = ${VMDURATION}"
echo "JOB_ID           = ${JOB_ID}"
echo "EXECUSR          = ${EXECUSR}"
echo "EXECHST          = ${EXECHST}"
echo


CURL_CMD="curl"
CURL_OPT="-f"
CURL_CRT="--cert $PROXY_FILE --cacert $PROXY_FILE --capath $X509_CERT_DIR"

#
# Setup PORTAL_SSHPUBKEY
#
cat $PORTAL_SSHPUBKEY >> $HOME/.ssh/authorized_keys
cat $PORTAL_SSHPUBKEY >> /home/$EXECUSR/.ssh/authorized_keys



#
# Notify portlet info to cloudmgr service (only user VM case)
#
if [ $TYPE_EXPERIMENT -eq  2 ]; then
  for ((i=0; i<5; i++)); do
    echo "Notifying to cloudmgr service ... (Attempt #$((i+1)))"
    CLOUDMGR_QUERY="$CLOUDMGR_URL/register?portal_host=$PORTAL_HOST&portal_name=$(urlencode $PORTAL_NAME)&portal_user=$PORTAL_USER&vm_name=alice&vm_ipaddr=$VMIPADDR&vm_uuid=$VMUUID&portal_jobid=$(urlencode $JOB_ID)"
    $CURL_CMD $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY
    RES=$?
    if [ $RES -eq 0 ]; then
        echo "Notify to cloudmgr service successfully accomplished"
        break;
    else
        sleep 10 # Wait a while before to do a new attempt
    fi
  done
  if [ $RES -ne 0 ]; then
    echo "An error occurred contacting cloudmgr service"
    echo "QUERY:"
    echo "$CURL_CMD  $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY"
    # The job will terminate; prepare the output
    tar cvf alice_output.tar .
    exit 0 
  else
    echo "Notification successfully accomplished"
  fi
fi

#
# Updating CRLs 
#
#printf  "Updating CRLs ... "
#/usr/sbin/fetch-crl >/dev/null
#echo "done"


echo "Listing $PWD directory"
ls -lrt

#CASO VM
VMLIFET=$VMDURATION
if [ $TYPE_EXPERIMENT -eq  2 ]; then
  #
  # If no algorithm is specified only ALEPHVM will be instantiated
  #

  # Create bash_history and the output directory
  ssh $EXECUSR@$EXECHST "touch .bash_history"
  ssh $EXECUSR@$EXECHST "mkdir ${VM_OUTPUT}"

  # Sending information to the user
  echo "VM instantiated; sending login information to the user"
  RANDPASS=$(date +%s | md5sum | base64 | head -c 12 ; echo)
  echo "Generated random password for user $EXECUSR is $RANDPASS"
  echo $RANDPASS | passwd --stdin $EXECUSR 
  printf "%s\n%s\n" "$RANDPASS" "$RANDPASS" | vncpasswd /home/adminuser/.vnc/passwd

  # Store VM access information (SSH)
  CLOUDMGR_QUERY="$CLOUDMGR_URL/access?vm_uuid=$VMUUID&proto=ssh&port=22&workgroup=&username=adminuser&password=$RANDPASS"
  $CURL_CMD $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY 
  RES=$?
  if [ $RES -eq 0 ]; then
      echo "Access information to cloudmgr service, successfully sent"
  else
      echo "WARNING: Could not add SSH access information to cloudmgr service"
      echo "$CURL_CMD $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY"
  fi

  # Store VM access information (VNC)
  CLOUDMGR_QUERY="$CLOUDMGR_URL/access?vm_uuid=$VMUUID&proto=vnc&port=5901&workgroup=&username=aliceuser&password=$RANDPASS"
  $CURL_CMD $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY
  RES=$?
  if [ $RES -eq 0 ]; then
      echo "Access information to cloudmgr service, successfully sent"
  else
      echo "WARNING: Could not add VNC access information to cloudmgr service"
      echo "$CURL_CMD $CURL_OPT $CURL_CRT $CLOUDMGR_QUERY"
  fi

  # Prepare email to send to the user
  cat > VMINFO.txt << EOF
<p>Dear <b>${PORTAL_USERFIRSTNAME} ${PORTAL_USERLASTNAME} (${PORTAL_USER})</b>,</p>

Welcome to the ALICE Virtual Machine.<br/>
 
<p>You can connect this machine executing:<br/>
    <b>ssh ${EXECUSR}@${VMIPADDR} and the password is: '${RANDPASS}'</b>
</p>
<p>You may also access to its desktop environment through the VNC session:<br/>
    <b>vnc://${VMIPADDR}:5901 and the password is: '${RANDPASS}'</b>
</p>
<p>Please be informed that your virtual service will be available for the next:<br/> ${VMDURATIONSTR}. After this time the resource will be released automatically by the system.
<p>You can save any file you like just putting it or its alias into the ${VM_OUTPUT} folder
</p>
EOF
  # Send generated email to the user
  CLOUDMGR_QUERY="$CLOUDMGR_URL/notify"
  POST_PARAMS="email_from=noreply@cloudmgr.com&email_to=$PORTAL_USRMAIL&email_subj=ALICE($VMUUID)"
  #curl --cert $PROXY_FILE --cacert $PROXY_FILE --capath /etc/grid-security/certificates -d "$POST_PARAMS" -d "email_body=$(cat VMINFO.txt)" "${CLOUDMGR_QUERY}"
  $CURL_CMD $CURL_OPT $CURL_CRT -d "$POST_PARAMS" -d "email_body=$(cat VMINFO.txt)" "${CLOUDMGR_QUERY}"
  RES=$?
  if [ $RES -ne 0 ]; then
      echo "WARNING: Could not notify access credentials to the user"
      echo "$CURL_CMD $CURL_OPT $CURL_CRT -d \"$POST_PARAMS\" \"email_body=$(cat VMINFO.txt)\" \"${CLOUDMGR_QUERY}\""
  else
      echo "The VM has been successfully notified to the user ($PORTAL_USRMAIL)"
  fi   

  # Wait for VM expiration
  echo "Entering in sleep mode for $VMLIFET seconds ..."
  touch $VMLOOPCTRL
  count=0
  while [ -f $VMLOOPCTRL ]; do
      sleep 1
      count=$((count+1))
      if [ $count -ge $VMLIFET ]; then
          rm -f $VMLOOPCTRL
      fi 
      echo $count"/"$VMLIFET > $VMLOOPCOUNT
  done
  echo "Sleep time expired; closing the VM ..."
  # The use of timer may be used for a second expiration and 
  # a possible new notification before to kill the VM

  # Further notification cannot be done because the proxy could be expired; anyhow
  # cloudmgr host could retrieve a newer

  # Listing ...
  echo
  echo "Listing again $PWD directory"
  ls -lrt
  echo
  echo "Listing the $EXECUSR home directory"
  ssh $EXECUSR@$EXECHST "/bin/ls -lrt"

  # At the end of the job the bash history will be send back to the user
  ssh $EXECUSR@$EXECHST "cat <<EOF > README.txt
#---------------------------------
# Aleph VM output (README.txt)
#---------------------------------
This is the description about the output of the alice virtual machine.
The output just consists of the folder ${VM_OUTPUT} that users may use to save their own files before the VM expiration.
EOF"
  ssh $EXECUSR@$EXECHST "tar cvf alice_output.tar README.txt ${VM_OUTPUT}"  
  scp $EXECUSR@$EXECHST:alice_output.tar .

#CASO JOB
else



echo "...................Start Analisys.................."

echo "TYPE EXPERIMENT:$TYPE_EXPERIMENT ----PARAM: $PARAM1 $PARAM2"

cp analisi.sh /home/adminuser/

su - adminuser -c 'sh analisi.sh '${TYPE_EXPERIMENT}' '${PARAM1}' '${PARAM2}' '


if [ $TYPE_EXPERIMENT -eq  0 ]
 then

cp /home/adminuser/analysis/RAA/Part2/alice_output.tar .

fi

if [ $TYPE_EXPERIMENT -eq  1 ]
 then

cp /home/adminuser/analysis/PT/PtAnalysis/macros/alice_output.tar .

fi

echo "...................FInish Analisys.................."

fi


 
 










