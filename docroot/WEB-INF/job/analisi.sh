#!/bin/bash


echo "------------------------ANALISI.sh------------------"


TYPE_EXPERIMENT=$1




#se il tipo di esperimento è RAA il primo parametro sarà 0

if [ $TYPE_EXPERIMENT -eq 0 ]
 then
 echo "RAA EXPERIMENT"

MIN_C=$2
MAX_C=$3

cd analysis/RAA/Part2



echo "PARAM: MIN = $MIN_C MAX = $MAX_C"

root -b  -q ' AnalyseTreeForRAAStudents.C++("MasterClassesTree_LHC10h_Run139036.root","PbPb","kFALSE",'${MIN_C}','${MAX_C}')'

echo "------------------------FINISH ANALISI.sh RAA------------------"

tar cvf alice_output.tar nTracksTPCvsCent_$MIN_C-$MAX_C.eps RAABaseOutput.root nTracksTPC_$MIN_C-$MAX_C.eps


echo "------------------------TAR RAA OK------------------"

fi


#se il tipo di esperimento è Pt il primo parametro sarà 1
if [ $TYPE_EXPERIMENT -eq  1 ]
 then
 echo "Pt EXPERIMENT"
 
TYPE_ANALISYS=$2 
NUM_FILE=$3


cd analysis/PT


echo "PARAM: NUM_FILE = $NUM_FILE  TYPE_ANALISYS = $TYPE_ANALISYS"

root -b -q 'AnalysisPt.C('${NUM_FILE}','${TYPE_ANALISYS}')'

echo "------------------------FINISH ANALISI.sh PT------------------"
cd PtAnalysis/macros
tar cvf alice_output.tar files.txt output.eps results.root

echo "------------------------TAR PT OK------------------"

fi