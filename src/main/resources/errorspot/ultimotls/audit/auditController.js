/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
var auditControllerModule = angular.module('auditControllerModule', []);

auditControllerModule.filter('pagination', function () {
    return function (input, start)
    {
        if (!input || !input.length) {
            return;
        };
        //start = +start; 
        start = parseInt(start, 10);
        return input.slice(start);
    };
});

auditControllerModule.controller('DataRetrieve', ['$scope', '$log', '$http', 'auditSearch', 'initPromise', 'queryEnv', 'treemapSaver','resetTimerService','logoutService',
    function ($scope, $log, $http, auditSearch, initPromise, queryEnv, treemapSaver,resetTimerService, logoutService) {
        //Initialize scope data 
        $scope.rowsOptions = [{rows: 5}, {rows: 10}, {rows: 25}, {rows: 50}, {rows: 100}];
        $scope.rowNumber = $scope.rowsOptions[2];
        $scope.predicate = 'timestamp.$date';
        $scope.replayQueryHolder = "";
        //Replay Page Options
        $scope.replayOptions = [{type: "REST"}, {type: "FILE"}, /*{type: "WS"},*/ {type: "FTP"}, {type: "JMS"}];
        $scope.replayType = $scope.replayOptions[0];
        //For Replay Data Page
        $scope.pageSize = 20;
        $scope.replayCurPage = 0;
        $scope.batchChecker = false;
        $scope.treemapSaver = treemapSaver;
        $scope.headerCounter = 0;
        $scope.rowID = null;
        //Toggle Feature to close Custom or Name Value fields
        $(document).ready(function(){
            $("#collapseCustom").click(function(){
                $(".collapseCustom").collapse('toggle');
            });
            $("#collapseNameValue").click(function(){
                $(".collapseNameValue").collapse('toggle');
            });
        });
        //flag and function to toggle between doSearch and doAdvanceSearch when choosing rowNumber
        var searchFlag = true;
        $scope.searchOn = function(bool){
            searchFlag = bool;
        };
        //Flag and variable for keyword used in Advance Search
        var keywordFlag = false;
        //Flag for objectID 
        var objectIDFlag = false;
        //check if initPromise from resolve has data.
        if (initPromise && initPromise.data) {
            var queryFromResolve = initPromise.config.url;
            $scope.searchCriteria = queryFromResolve.substring(queryFromResolve.indexOf(',')+1, queryFromResolve.lastIndexOf('}') - 1);
            $scope.data = initPromise.data;
            $scope.treemapSaver.auditData = $scope.data;
        };
        clearError = function(){ //onKeyPress error message will clear
            $scope.inputError = "";
        };
        $scope.customField = [{}], $scope.nameValueField = [{}];
        $scope.customFieldLength = 1, $scope.nameValueFieldLength = 1;
        $scope.basicSearchButton = function (query,dbType) {
            $scope.dbTypeSetter = dbType;
            $scope.searchOn(true);
            if (/:/.test(query)) {
                try {
                    JSON.parse(query);
                }
                catch (err) {
                    $scope.inputError = "Input should be valid JSON. eg. {\"transactionId\":\"BBQ1234\"} ";
                    return;
                }
            }
            var searchPromise = auditSearch.doSearch(query, $scope.rowNumber, dbType);
            $scope.inputError = "";
            searchPromise.then(function (response, status, header, config) {
                if(status===401){
                    logoutService.logout();
                }
                var extractedURL = response.config.url, pos1=extractedURL.indexOf("="), pos2=extractedURL.indexOf("&");
                var extractedQuery = extractedURL.slice(pos1+1,pos2);
                $scope.replayQueryHolder = extractedQuery;//Used for replay services
                $scope.data = response.data;
                $scope.treemapSaver.auditData = $scope.data;
            });
            document.getElementById("replaySelectAll").checked = false;
        };
        //Function for Custom Field
        $scope.addNewCustom = function () {
            if($scope.customField[$scope.customFieldLength-1].name && $scope.customField[$scope.customFieldLength-1].value ){
                $scope.customField.push({});
                $scope.errorWarning = "";
                $scope.customFieldLength = $scope.customFieldLength + 1;
            }
            else{
                $scope.errorWarning = "Both name and value must be enter before creating a new field";
            }
        };
        $scope.removeCustom = function (index) {
            if($scope.customFieldLength - 1 === 0){
                return false;
            };
            $scope.customField.splice(index,1);
            $scope.customFieldLength = $scope.customFieldLength - 1;
            
        };
        $scope.numberOfPagesCustom = function () {
            return Math.ceil($scope.customFieldLength / $scope.pageSize);
        };
        function checkCustomField(customFieldQuery){
            var checkCustomFieldFlag = false;
            if(!customFieldQuery[0]){
                return false;
            };
            if(customFieldQuery[0].name && customFieldQuery[0].value){
                checkCustomFieldFlag = true;
                return checkCustomFieldFlag;
            };
            $scope.errorWarning = "Both name and value must be enter before search can be performed";
            return checkCustomFieldFlag;
        };
        function appendCustomField(){
            $scope.customFieldString = "";
            for(var i = 0; i< $scope.customFieldLength; i++){
                $scope.customFieldString = $scope.customFieldString+"\"customFields."+$scope.customField[i].name+"\":\""+$scope.customField[i].value+"\",";
            }
            return $scope.customFieldString;
        };
        //Function for Name Value Field
        $scope.addNewNameValue = function () {
            if($scope.nameValueField[$scope.nameValueFieldLength-1].name && $scope.nameValueField[$scope.nameValueFieldLength-1].value ){
                $scope.nameValueField.push({});
                $scope.errorWarning = "";
                $scope.nameValueFieldLength = $scope.nameValueFieldLength + 1;
            }
            else{
                $scope.errorWarning = "Both name and value must be enter before creating a new field";
            }
        };
        $scope.removeNameValue = function (index) {
            if($scope.nameValueFieldLength - 1 === 0){
                return false;
            }
            $scope.nameValueField.splice(index,1);
            $scope.nameValueFieldLength = $scope.nameValueFieldLength - 1;
        };
        $scope.numberOfPagesNameValue = function () {
            return Math.ceil($scope.nameValueFieldLength / $scope.pageSize);
        };
        function checkNameValueField(nameValueFieldQuery){
            var checkNameValueField = false;
            if(!nameValueFieldQuery[0]){
                return false;
            }
            if(nameValueFieldQuery[0].name && nameValueFieldQuery[0].value){
                checkNameValueField = true;
                return checkNameValueField;
            }
            $scope.errorWarning = "Both name and value must be enter before search can be performed";
            return checkNameValueField;
        };
        function appendNameValueField(){
            $scope.nameValueFieldString = "";
            for(var i = 0; i<$scope.nameValueFieldLength; i++){
                $scope.nameValueFieldString = $scope.nameValueFieldString+"\""+$scope.nameValueField[i].name+"\":\""+$scope.nameValueField[i].value+"\",";
            }
            return $scope.nameValueFieldString;
        };
        function checkObj(advanceSearch) {
            /* function to validate the existence of each key in the object to get the number of valid keys. */
            var checkObjFlag = false;
            if(!advanceSearch){
                return false;
            }
            else if(advanceSearch.objectID){
                objectIDFlag = true;
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.keyword) {
                keywordFlag = true;
                checkObjFlag = true;
            }
            else if (advanceSearch.application) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.interface) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.hostname) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.txDomain) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.txType) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.txID) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.severity) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else if(advanceSearch.errorType) {
                keywordFlag = false;
                checkObjFlag = true;
            }
            else {
                checkObjFlag = false;
                keywordFlag = false;
            }
            return checkObjFlag; keywordFlag, objectIDFlag;
        };
        function appendFields(advanceSearch){
            var string = "";
            if (advanceSearch.objectID){
                var appendApp = "\"_id\":{\"$oid\":\""+advanceSearch.objectID+"\"},";
                string = appendApp;
                return string;
            }
            if (advanceSearch.application) {
                var appendApp = "\"application\":\""+advanceSearch.application.toLowerCase()+"\",";
                string = appendApp;
            }
            if(advanceSearch.interface) {
                var appendInterface = "\"interface1\":\""+advanceSearch.interface+"\",";
                string = string+appendInterface;
            }
            if(advanceSearch.hostname) {
                var appendHostname = "\"hostname\":\""+advanceSearch.hostname+"\",";
                string = string+appendHostname;
            }
            if(advanceSearch.txDomain) {
                var appendTxDomain = "\"transactionDomain\":\""+advanceSearch.txDomain.toLowerCase()+"\",";
                string = string+appendTxDomain;
            }
            if(advanceSearch.txType) {
                var appendTxType = "\"transactionType\":\""+advanceSearch.txType.toLowerCase()+"\",";
                string = string+appendTxType;
            }
            if(advanceSearch.txID) {
                var appendTxID = "\"transactionId\":\""+advanceSearch.txID+"\",";
                string = string+appendTxID;
            }
            if(advanceSearch.severity) {
                var appendSeverity = "\"severity\":\""+advanceSearch.severity.toLowerCase()+"\",";
                string = string+appendSeverity;
            }
            if(advanceSearch.errorType) {
                var appendErrorType = "\"errorType\":\""+advanceSearch.errorType.toLowerCase()+"\",";
                string = string+appendErrorType;
            }
            return string;
        };
        ////ADVANCE SEARCH FUNCTION///////////
        $scope.doAdvanceSearch = function (toDate, fromDate, dbType) {
            //Setters
            $scope.dbTypeSetter = dbType;
            $scope.searchOn(false);
            document.getElementById("replaySelectAll").checked = false;
            //URL PARAMETERS
            var getURL = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/SearchService";
            var urlParam = "&searchtype=advanced&count&pagesize="+$scope.rowNumber.rows+"&searchdb="+dbType;
            //INITIAL QUERIES
            var dateQuery = "", query = "", customQuery = "", nameValueQuery="", envQuery = "\"envid\":\""+queryEnv.getEnv().name+"\",";
            var doAdvanceSearch = false;
            //INITIALIZE FLAGS
            var advanceSearchObjectflag = checkObj($scope.advanceSearch);
            var advanceCustomFieldObjectFlag = checkCustomField($scope.customField);
            var advanceNameValueFieldObjectFlag = checkNameValueField($scope.nameValueField);
            //CHECK FLAGS AND SET QUERIES ACCORDINGLY
            if(advanceCustomFieldObjectFlag){
                customQuery = appendCustomField();
                doAdvanceSearch = true;
            };
            if(advanceNameValueFieldObjectFlag){
                nameValueQuery = appendNameValueField();
                doAdvanceSearch = true;
            };
            if(toDate || fromDate){
                if(toDate && fromDate){
                    from = new Date(fromDate).toISOString();
                    to = new Date(toDate).toISOString(); //figure out how to add one day
                    dateQuery = "'timestamp':{'$gte':{'$date':'"+from+"'},'$lt':{'$date':'"+to+"'}},";
                    doAdvanceSearch = true;
                }
                else{
                    $scope.errorWarning = "A valid date must be entered for BOTH fields";
                }
            };
            if (dbType === "payload" && !advanceSearchObjectflag){
                $scope.errorWarning = "Keyword must be entered for Payload Search";
                return;
            };
            if (advanceSearchObjectflag){
                query = appendFields($scope.advanceSearch); //removes last comma in the JSON query
                doAdvanceSearch = true;
                var keyPhrase = $scope.advanceSearch.keyword;
                if (keywordFlag || dbType === "payload"){
                    urlParam = "&searchtype=advanced&count&pagesize="+$scope.rowNumber.rows+"&searchdb="+dbType+"&searchkeyword="+keyPhrase;
                };
                if(dbType === "payload" && keyPhrase === (""||undefined)){
                    $scope.errorWarning = "Keyword must be entered for Payload Search";
                    return;
                };
            };
            //GENERATE FINAL QUERY
            var finalAdvanceSearchQuery = "?filter={\"$and\":[{"+(envQuery+query+customQuery+nameValueQuery+dateQuery).slice(0,-1)+"}]}";
            if(objectIDFlag){
                finalAdvanceSearchQuery = "?filter={\"$and\":[{"+(envQuery+query).slice(0,-1)+"}]}";
            }
            $scope.replayQueryHolder = finalAdvanceSearchQuery; // for replay services
            //PERFORM GET CALL
            if(doAdvanceSearch){
                $scope.errorWarning = "";
                var advanceSearchUrl = getURL+finalAdvanceSearchQuery+urlParam;
                $http.get(advanceSearchUrl, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function (response,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        $scope.data = response;
                        $scope.treemapSaver.auditData = $scope.data;
                        $scope.errorWarning = "";
                    }).error(function(data,status,header,config){
                        if(status === 0){
                            $scope.errorWarning = "Call Timed Out";
                        }
                        if(status === 401){
                            logoutService.logout();
                        }
                    });
                $scope.predicate = 'timestamp.$date'; //by defualt it will order results by date
            }
            else{
                $scope.errorWarning = "No fields have been entered";
            }
        };
        //First, Previous, Next, Last are button function for Pagination to render new view
        $scope.goToFirst = function(){
            var firstLink = $scope.data._links.first.href;
            if (firstLink === null || firstLink === undefined) {
                alert("Row(s) has not been queried");
            }
            else {
                var firstUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+firstLink;
                try{
                    $http.get(firstUrl, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function (response,status, header, config) {
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        $scope.data = response;
                        $scope.treemapSaver.auditData = $scope.data;
                    });
                }
                catch(err){
                    console.log(err);
                }
            }
        };
        $scope.goToPrevious = function () {
            var previousLink = $scope.data._links.previous.href;
            if (previousLink === undefined || previousLink === null) {
                alert("No previous rows available");
            }
            else {
                var previousUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+previousLink;
                $http.get(previousUrl, {timeout:TLS_SERVER_TIMEOUT})
                .success(function (response,status, header, config) {
                    var auth_token_valid_until = header()['auth-token-valid-until'];
                    resetTimerService.set(auth_token_valid_until);    
                    $scope.data = response;
                    $scope.treemapSaver.auditData = $scope.data;
                });
            }
        };
        $scope.goToNext = function () {
            var nextLink = $scope.data._links.next.href;
            if (nextLink === undefined || nextLink === null) {
                alert("No more rows available");
            }
            else {
                var nextUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+nextLink;
                $http.get(nextUrl, {timeout:TLS_SERVER_TIMEOUT})
                .success(function (response,status, header, config) {
                    var auth_token_valid_until = header()['auth-token-valid-until'];
                    resetTimerService.set(auth_token_valid_until);
                    $scope.data = response;
                    $scope.treemapSaver.auditData = $scope.data;
                });
            }
        };
        $scope.goToLast = function () {
            var lastLink = $scope.data._links.last.href;
            var lastUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+lastLink;
            $http.get(lastUrl, {timeout:TLS_SERVER_TIMEOUT})
            .success(function (response,status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.data = response;
                $scope.treemapSaver.auditData = $scope.data;
            });
        };
        $scope.rowSelected = function(toDate, fromDate){//toggle between Search and AdvanceSearch
            if($scope.dbTypeSetter){ //if the row number is selected and a dbType is set run the search function
                if(searchFlag){
                    $scope.basicSearchButton($scope.searchCriteria, $scope.dbTypeSetter);
                }
                else{
                    $scope.doAdvanceSearch(toDate, fromDate, $scope.dbTypeSetter);
                }
            }
            else{
                return false;
            }           
        };
        //Click event on Rows from Audit Data to be passed to the Slider Window
        $scope.rowClick = function(rowData){
            $scope.sliderWindowData = rowData;
            $scope.rowID = rowData['_id']['$oid'];
            $scope.batchChecker = false;
            $scope.replayResponseRest = " ";
            $scope.replayResponseFile = " ";
            $scope.replayResponseWs = " ";
            $scope.replayResponseFTP = " ";
        };
        //makes a http call for related transactionId
        $scope.relatedTransaction = function(transactionID){
            var urlParam = "&searchtype=advanced&count&pagesize="+$scope.rowNumber.rows+"&searchdb=audit";
            var getData = "{\"transactionId\":\""+transactionID+"\"}"; //needs end URL Parameters
            var getURL = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/SearchService?filter=";
            $http.get(getURL+getData+urlParam,{timeout:TLS_SERVER_TIMEOUT})
            .success(function(response,status, header, config){
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.relatedTransactionData = response._embedded['rh:doc'];
            });
        };
        //From relatedTransaction a click function will open a new Modal page and populated new data
        $scope.relatedSearch = function(rowData){
            $scope.relatedSearchData = rowData;
        };
        $scope.callPayload = function(data){ //from Database Page datalocation makes a call
            var dataLocationId = data;
            var payloadUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/PayloadService?id=";
            $http.get(payloadUrl+dataLocationId, {timeout:TLS_SERVER_TIMEOUT})
            .success(function (response,status, header, config){ 
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.payloadPageData = response;
            });
        };
        $scope.restReplay = {};
        var replayPostUrl = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/ReplayService";
        var replayPostUrlBatch = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/BatchReplayService";
        $scope.runRestService = function(){//only takes JSON files not 
            $scope.replayResponseRest= " ";
            if($scope.batchChecker === false){
                var headerType = null;
                var headerVal = null;
                var methodVal = document.getElementById("replayDropDownMethod").value;
                var contentVal = document.getElementById("replayDropDownApplication").value;
                var headerHolder = null;
                var auditID = $scope.rowID;
                var batchVals = $scope.batchValues();
            
                
                if(methodVal === "other")methodVal = document.getElementById("methodValue").value;
                if(contentVal === "other")contentVal = document.getElementById("contentType").value;
                var restPayload = null;
                restPayload = '"type":"REST", "endpoint":"'+$scope.restReplay.endpointUrl+'", "method":"'+
                    methodVal+'", "content-type":"'+contentVal+'", "restHeaders":['+headerHolder+']';
                    headerHolder = '{"type":"'+headerType+'", "value":"'+headerVal+'"}';
                    
                if($scope.restReplay.header === undefined || $scope.restReplay.header === null){
                    headerType = "";
                    headerVal = "";
                    restPayload = '"type":"REST", "endpoint":"'+$scope.restReplay.endpointUrl+'", "method":"'+
                        methodVal+'", "content-type":"'+contentVal+'", "auditID":"'+auditID+'", "replayedBy":"'+batchVals[1]+'"';
                }else{
                    headerType = $scope.restReplay.header.type;
                    headerVal = $scope.restReplay.header.value;
                    headerHolder = '{"type":"'+headerType+'", "value":"'+headerVal+'"}';
                    if($scope.headerCounter > 0){
                        for(var z = 0; z < $scope.headerCounter; z++){
                            var tempType = document.getElementById("headerType" + (z)).value;
                            var tempVal = document.getElementById("headerValue" + (z)).value;
                            if(tempType === "")tempType = "Authorization";
                            headerHolder += ', {"type":"'+tempType+'", "value":"'+tempVal+'"}';
                        }
                    }
                    restPayload = '"type":"REST", '+
                            '"endpoint":"'+$scope.restReplay.endpointUrl+'", '+
                            '"method":"'+ methodVal + '", '+
                            '"content-type":"'+contentVal+'", '+
                            '"restHeaders":['+headerHolder+'], '+
                            '"auditID":"'+auditID+'", '+
                            '"replayedBy":"'+batchVals[1]+'"';
                }
                
                var multipartPayload = "Content-Type: multipart/mixed; boundary=boundaryREST\n"+
                        "--boundaryREST\n" +
                        "Content-Type: application/json;\n\n" +
                        "{"+restPayload+"}\n\n" + 
                        "--boundaryREST\n" +
                        "Content-Type: text/plain; charset: utf-8;\n\n" + 
                        $scope.payloadPageData+
                        "\n\n--boundaryREST--";
                
                console.log(multipartPayload);
                if($scope.restReplay.endpointUrl !== undefined && methodVal !== "" && contentVal !== ""){
                    $http.post(replayPostUrl, multipartPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseRest= "Rest Replay Success";
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseRest = "Error: Could Not Connect";
                            //$scope.replayResponseRest = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
                
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                
                var headerType = null;
                var headerVal = null;
                var methodVal = document.getElementById("replayDropDownMethod").value;
                var contentVal = document.getElementById("replayDropDownApplication").value;
                
                if(methodVal === "other")methodVal = document.getElementById("methodValue").value;
                if(contentVal === "other")contentVal = document.getElementById("contentType").value;
                var restPayload = null;
                
                if($scope.restReplay.header === undefined || $scope.restReplay.header === null){
                    headerType = "";
                    headerVal = "";
                    restPayload = '"type":"REST", "endpoint":"'+$scope.restReplay.endpointUrl+'", "method":"'+
                        methodVal+'", "content-type":"'+contentVal+'"';
                }else{
                    headerType = $scope.restReplay.header.type;
                    headerVal = $scope.restReplay.header.value;
                    headerHolder = '{"type":"'+headerType+'", "value":"'+headerVal+'"}';
                    if($scope.headerCounter > 0){
                        for(var z = 0; z < $scope.headerCounter; z++){
                            var tempType = document.getElementById("headerType" + (z)).value;
                            var tempVal = document.getElementById("headerValue" + (z)).value;
                            if(tempType === "")tempType = "Authorization";
                            headerHolder += ', {"type":"'+tempType+'", "value":"'+tempVal+'"}';
                        }
                    }
                    restPayload = '"type":"REST", '+
                    '"endpoint":"'+$scope.restReplay.endpointUrl+'", '+
                    '"method":"'+ methodVal+'", '+
                    '"content-type":"'+contentVal+'", '+
                    '"restHeaders":['+headerHolder+']';
                }
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "status":"new", "replayDestinationInfo": { '+restPayload+' },'+
                                    '"auditID": ['+auditIDs+']}';
                            console.log(batchPayload);
                if($scope.restReplay.endpointUrl !== undefined && methodVal !== "" && contentVal !== ""){
                    $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseRest = "Success: " + d;
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseRest = "Error: Could Not Connect";
                            //$scope.replayResponseRest = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }    
        };
        $scope.fileReplay = {};
        $scope.runFileService = function(){
            $scope.replayResponseFile =  " ";
            if($scope.batchChecker === false){
                var auditID = $scope.rowID;
                var batchVals = $scope.batchValues();
                var fileName = document.getElementById("fileName").value;
                var fileExt = document.getElementById("fileDropDownExt").value;
                if(fileExt === "other"){
                    fileExt = document.getElementById("fileType").value;
                    if(fileExt.indexOf('.') === -1)
                    {
                      fileExt = "." + document.getElementById("fileType").value;
                    }
                }
                
                var filePayload = null; 
                if(fileName === ""){
                    filePayload = '"type":"FILE", "fileLocation":"'+$scope.fileReplay.location+'", '+
                        '"fileType":"'+fileExt+'", "auditID":"'+auditID+'", "replayedBy":"'+batchVals[1]+'"';
                }else{
                    filePayload = '"type":"FILE", "fileLocation":"'+$scope.fileReplay.location+'", "fileName":"'+fileName+'", '+
                        '"fileType":"'+fileExt+'", "auditID":"'+auditID+'", "replayedBy":"'+batchVals[1]+'"';
                }
                var multipartPayload = "Content-Type: multipart/mixed; boundary=boundaryFILE\n"+
                    "--boundaryFILE\n" +
                    "Content-Type: application/json;\n\n" +
                    "{"+filePayload+"}\n\n" + 
                    "--boundaryFILE\n" +
                    "Content-Type: text/plain; charset: utf-8;\n\n" + 
                    $scope.payloadPageData+
                    "\n\n--boundaryFILE--";
                console.log(multipartPayload);
                if($scope.fileReplay.location !== undefined && fileName !== undefined && fileExt !== ""){
                    $http.post(replayPostUrl, multipartPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFile = "File Replay Success";
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFile = "Error: Could Not Connect";
                            //$scope.replayResponseFile =  "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
                
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var fileName = document.getElementById("fileName").value;
                var fileExt = document.getElementById("fileDropDownExt").value;
                if(fileExt === "other"){
                    fileExt = document.getElementById("fileType").value;
                    if(fileExt.indexOf('.') === -1)
                    {
                      fileExt = "." + document.getElementById("fileType").value;
                    }
                }
                
                var filePayloadBatch = null;
                
                if(fileName === ""){
                    filePayloadBatch = '"type":"FILE", "fileLocation":"'+$scope.fileReplay.location+'", '+
                        '"fileType":"'+fileExt+'"';
                }else{
                    filePayloadBatch = '"type":"FILE", "fileLocation":"'+$scope.fileReplay.location+'", "fileName":"'+fileName+'", '+
                        '"fileType":"'+fileExt+'"';
                }
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "status":"new", "replayDestinationInfo": { '+filePayloadBatch+' },'+
                                    '"auditID": ['+auditIDs+']}';
                console.log(batchPayload);
                if($scope.fileReplay.location !== undefined && fileName !== undefined && fileExt !== ""){
                    $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFile =  "Success: " + d;
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFile =  "Error: Could Not Connect";
                            //$scope.replayResponseFile =  "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
        };
        $scope.webServiceReplay = {};
        $scope.runWebService = function(){
            $scope.replayResponseWs = " ";
            if($scope.batchChecker === false){
                var auditID = $scope.rowID;
                var batchVals = $scope.batchValues();
                var webServicePayload = '"type":"WS", '+
                        '"wsdl":"'+$scope.webServiceReplay.wsdl+'", '+
                        '"operation":"'+$scope.webServiceReplay.operation+'",'+
                        '"soapaction":"'+$scope.webServiceReplay.soapAction+'", '+
                        '"binding":"'+$scope.webServiceReplay.binding+'", '+
                        '"auditID":"'+auditID+'", '+
                        '"replayedBy":"'+batchVals[1]+'"';
                var multipartPayload = "Content-Type: multipart/mixed; boundary=boundaryWS\n"+
                    "--boundaryWS\n" +
                    "Content-Type: application/json;\n\n" +
                    "{"+webServicePayload+"}\n\n" + 
                    "--boundaryWS\n" +
                    "Content-Type: text/plain; charset: utf-8;\n\n" + 
                    $scope.payloadPageData+
                    "\n\n--boundaryWS--";
                console.log(multipartPayload);
                if($scope.webServiceReplay.wsdl !== undefined && $scope.webServiceReplay.operation !== undefined && $scope.webServiceReplay.binding !== undefined){
                    $http.post(replayPostUrl, multipartPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseWs = "Web Service Replay Success";
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseWs = "Error: Could Not Connect";
//                            $scope.replayResponseWs = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var webServicePayloadBatch = '"type":"WS", '+
                        '"wsdl":"'+$scope.webServiceReplay.wsdl+'", '+
                        '"operation":"'+$scope.webServiceReplay.operation+'",' + 
                        '"soapaction":"'+$scope.webServiceReplay.soapAction+'", '+
                        '"binding":"'+$scope.webServiceReplay.binding+'"';
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'", "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "status":"new", "replayDestinationInfo": { '+webServicePayloadBatch+' },'+
                                    '"auditID": ['+auditIDs+']}';
                console.log(batchPayload);
                if($scope.webServiceReplay.wsdl !== undefined && $scope.webServiceReplay.operation !== undefined && $scope.webServiceReplay.binding !== undefined){
                    $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseWs = "Success: " + d;
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseWs = "Error: Could Not Connect";
//                            $scope.replayResponseWs = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
        };
        $scope.ftpServiceReplay = {};
        $scope.runFTPService = function(){
            $scope.replayResponseFTP = " ";
            if($scope.batchChecker === false){
                var auditID = $scope.rowID;
                var batchVals = $scope.batchValues();
                var ftpPayload = '"type":"FTP", '+
                        '"host":"'+$scope.ftpServiceReplay.host+'", '+
                        '"port":"'+$scope.ftpServiceReplay.port+'", '+
                        '"username":"'+$scope.ftpServiceReplay.username+'", '+
                        '"password":"'+$scope.ftpServiceReplay.password+'", '+
                        '"location":"'+$scope.ftpServiceReplay.location+'", '+
                        '"fileType":"'+$scope.ftpServiceReplay.fileType+'", '+
                        '"auditID":"'+auditID+'", '+
                        '"replayedBy":"'+batchVals[1]+'"';
                var multipartPayload = "Content-Type: multipart/mixed; boundary=boundaryFTP\n"+
                    "--boundaryFTP\n" +
                    "Content-Type: application/json;\n\n" +
                    "{"+ftpPayload+"}\n\n" + 
                    "--boundaryFTP\n" +
                    "Content-Type: text/plain; charset: utf-8;\n\n" + 
                    $scope.payloadPageData+
                    "\n\n--boundaryFTP--";   
                if($scope.ftpServiceReplay.host !== undefined && $scope.ftpServiceReplay.port !== undefined && 
                        $scope.ftpServiceReplay.username !== undefined && $scope.ftpServiceReplay.password !== undefined &&
                        $scope.ftpServiceReplay.location !== undefined && $scope.ftpServiceReplay.fileType !== undefined){
                    $http.post(replayPostUrl, multipartPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFTP = "FTP Replay Success";
                        }).error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFTP = "Error: Could Not Connect";
//                            $scope.replayResponseFTP = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var ftpPayloadBatch = '"type":"FTP", '+
                        '"host":"'+$scope.ftpServiceReplay.host+'", '+
                        '"port":"'+$scope.ftpServiceReplay.port+'", '+
                        '"username":"'+$scope.ftpServiceReplay.username+'", '+
                        '"password":"'+$scope.ftpServiceReplay.password+'", '+
                        '"location":"'+$scope.ftpServiceReplay.location+'", '+
                        '"fileType":"'+$scope.ftpServiceReplay.fileType+'"';
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "status":"new", "replayDestinationInfo": { '+ftpPayloadBatch+' },'+
                        '"auditID": ['+auditIDs+']}';
                if($scope.ftpServiceReplay.host !== undefined && $scope.ftpServiceReplay.port !== undefined && 
                        $scope.ftpServiceReplay.username !== undefined && $scope.ftpServiceReplay.password !== undefined &&
                        $scope.ftpServiceReplay.location !== undefined && $scope.ftpServiceReplay.fileType !== undefined){
                    $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFTP = "Success: " + d;
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseFTP = "Error: Could Not Connect";
//                            $scope.replayResponseFTP = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
        };
        $scope.jmsReplay = {};
        $scope.runJMSService = function(){
            $scope.replayResponseJMS = " ";
            if($scope.batchChecker === false){
                var auditID = $scope.rowID;
                var batchVals = $scope.batchValues();
                var serverType = document.getElementById("jmsServerType").value;
                var deliveryMode = $scope.jmsReplay.deliveryMode;
                var jmsPayload = '"type":"JMS", '+
                        '"jmsServerType":"' + serverType + '", '+
                        '"destinationName":"' + $scope.jmsReplay.destinationName + '",' +
                        '"destinationType":"' + $scope.jmsReplay.destinationType + '", '+
                        '"connectionFactory":"' + $scope.jmsReplay.connectionFactory + '", ' + 
                        '"host":"' + $scope.jmsReplay.host + '", ' +
                        '"port":"' + $scope.jmsReplay.port + '", ' +
                        '"username":"' + $scope.jmsReplay.username + '", ' + 
                        '"password":"' + $scope.jmsReplay.password + '", ' + 
                        '"deliveryMode":"' + deliveryMode + '"';
                
                if(serverType === "Weblogic"){
                    jmsPayload += ', "initalContextFactory":"' + $scope.jmsReplay.initalContextFactory + '" ';
                }
                
                jmsPayload += ', "auditID":"'+auditID+'", '+
                        '"replayedBy":"'+batchVals[1]+'"';
                
                var multipartPayload = "Content-Type: multipart/mixed; boundary=boundaryJMS\n"+
                    "--boundaryJMS\n" +
                    "Content-Type: application/json;\n\n" +
                    "{"+jmsPayload+"}\n\n" + 
                    "--boundaryJMS\n" +
                    "Content-Type: text/plain; charset: utf-8;\n\n" + 
                    $scope.payloadPageData+
                    "\n\n--boundaryJMS--";   
            console.log(multipartPayload);
                if(serverType !== undefined && 
                        $scope.jmsReplay.destinationName !== undefined && 
                        $scope.jmsReplay.destinationType !== undefined && 
                        $scope.jmsReplay.connectionFactory !== undefined &&
                        $scope.jmsReplay.host !== undefined && 
                        $scope.jmsReplay.port !== undefined && 
                        $scope.jmsReplay.username !== undefined && 
                        //$scope.jmsReplay.password !== undefined && 
                        deliveryMode !== ""){
                    $http.post(replayPostUrl, multipartPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseJMS = "JMS Replay Success";
                        }).error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseJMS = "Error: Could Not Connect";
//                            $scope.replayResponseFTP = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var serverType = document.getElementById("jmsServerType").value;
                var deliveryMode = $scope.jmsReplay.deliveryMode;
                var jmsPayloadBatch = '"type":"JMS", '+
                        '"jmsServerType":"' + serverType + '", '+
                        '"destinationName":"' + $scope.jmsReplay.destinationName + '",' +
                        '"destinationType":"' + $scope.jmsReplay.destinationType + '", '+
                        '"connectionFactory":"' + $scope.jmsReplay.connectionFactory + '", ' + 
                        '"host":"' + $scope.jmsReplay.host + '", ' +
                        '"port":"' + $scope.jmsReplay.port + '", ' +
                        '"username":"' + $scope.jmsReplay.username + '", ' + 
                        '"password":"' + $scope.jmsReplay.password + '", ' + 
                        '"deliveryMode":"' + deliveryMode + '"';
                
                if(serverType === "Weblogic"){
                    jmsPayloadBatch += ', "initalContextFactory":"' + $scope.jmsReplay.initalContextFactory + '"';
                }
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "status":"new", "replayDestinationInfo": { '+jmsPayloadBatch+' },'+
                        '"auditID": ['+auditIDs+']}';
                console.log(batchPayload);
                if(serverType !== undefined && 
                        $scope.jmsReplay.destinationName !== undefined && 
                        $scope.jmsReplay.destinationType !== undefined && 
                        $scope.jmsReplay.connectionFactory !== undefined &&
                        $scope.jmsReplay.host !== undefined && 
                        $scope.jmsReplay.port !== undefined && 
                        $scope.jmsReplay.username !== undefined && 
                        //$scope.jmsReplay.password !== undefined && 
                        deliveryMode !== ""){
                    $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseJMS = "Success: " + d;
                        })
                        .error(function(d,status, header, config){
                            var auth_token_valid_until = header()['auth-token-valid-until'];
                            resetTimerService.set(auth_token_valid_until);
                            $scope.replayResponseJMS = "Error: Could Not Connect";
//                            $scope.replayResponseFTP = "Error: " + d["http status code"] + ": " + d["message"];
                        });
                }
            }
        };
        $scope.changeReplay = function(){
            $scope.batchChecker = true;
            if($scope.treemapSaver.checkboxChecked !== undefined){
                $("#replayPage").css("top","15%").addClass("col-sm-offset-3").removeClass("col-sm-offset-6");
                $scope.replayResponseRest = " ";
                $scope.replayResponseFile = " ";
                $scope.replayResponseWs = " ";
                $scope.replayResponseFTP = " ";
                $scope.replayResponseJMS = " ";
            };
        };
        $scope.replayRowClicked = function(d){
            for(var key in d){
                if(d[key].$date){
                    var temp = new Date(d[key].$date);
                    d[key] = temp.toGMTString();
                }
            }
            $scope.singleSelectedReplayData = d;
        };
        $scope.replayedData = function(d){
            $scope.replaySelected = d.replayInfo;
            $scope.replaySelectedLength = $scope.replaySelected.length;
        };
        $scope.numberOfPagesReplay = function () {
            return Math.ceil($scope.replaySelectedLength / $scope.pageSize);
        };
        $scope.resetReplayCurPage = function(){
            $scope.replayCurPage = 0;
        };
        $scope.replayButtonChecker = function(){
            var checkboxes = document.getElementsByName('auditCheckbox');
            $scope.treemapSaver.checkboxChecked = undefined;
            for(var i=0, n=checkboxes.length;i<n;i++) {
                if(checkboxes[i].checked){
                    $scope.treemapSaver.checkboxChecked = true;
                    document.getElementById("replayButton").style.opacity = 1;
                    document.getElementById("replayButton").disabled = false;
                    break;
                }
                else{
                    $scope.treemapSaver.checkboxChecked = undefined;
                    document.getElementById("replayButton").style.opacity = .5;
                    document.getElementById("replayButton").disabled = true;
                }
            };
        };
        $scope.addHeaders = function(){
            $("#labelDiv").clone(false).prop("id","labelDiv" + $scope.headerCounter).css("opacity","0").appendTo("#restHeaderDiv");
            $("#headerTypeDiv").clone(false).prop("id","headerType" + $scope.headerCounter).appendTo("#restHeaderDiv");
            $("#headerValueDiv").clone(false).prop("id","headerValue" + $scope.headerCounter).addClass("col-sm-5").removeClass("col-sm-4").appendTo("#restHeaderDiv");
            $scope.headerCounter++;
        };
        $scope.checkSelected = function(){
            var methodVal = document.getElementById("replayDropDownMethod");
            var contentVal = document.getElementById("replayDropDownApplication");
            var contentValText = document.getElementById("contentType");
            var methodValText = document.getElementById("methodValue");
            
            if(methodVal.value === "other"){
                methodValText.style.display = "inline";
            }
            else{
                methodValText.style.display = "none";
            }
            
            if(contentVal.value === "other"){
                contentValText.style.display = "inline";
            }
            else{
                contentValText.style.display = "none";
            }
        };
        $scope.checkSelectedFile = function(){
            var extVal = document.getElementById("fileDropDownExt");
            var extValText = document.getElementById("fileType");
            
            if(extVal.value === "other"){
                extValText.style.display = "inline";
            }
            else{
                extValText.style.display = "none";
            }
        };
        $scope.checkSelectedJMS = function(){
            var extVal = document.getElementById("jmsServerType");
            var factoryDiv = document.getElementById("jmsInitialContextFactoryDiv");
            
            if(extVal.value === "Weblogic"){
                factoryDiv.style.visibility = "visible";
            }
            else{
                factoryDiv.style.visibility = "hidden";
            }
        };
        $scope.batchValues = function(){
            var timestamp = new Date().toISOString();
            var username = treemapSaver.nameSaver;
            var checkboxes = document.getElementsByName('auditCheckbox');
            var auditIDs = [];
            var auditData = $scope.treemapSaver.auditData._embedded['rh:doc'];
            for(var i=0, n=checkboxes.length;i<n;i++) {
                if(checkboxes[i].checked){
                    auditIDs.push(auditData[i]._id.$oid);
                };
            };
            var batchVals = [timestamp, username, auditIDs];
            return batchVals;
        };
        $scope.pullAuditIDs = function(batchVals){
            var auditIDs = null;
            for(var z = 0; z < batchVals.length; z++){
                if(z > 0){
                    auditIDs += ',"'+batchVals[z]+'"';
                }
                else{
                    auditIDs = '"'+batchVals[z]+'"';
                }
            };
            return auditIDs;
        };
        $scope.changeReplayBack = function(){
            $("#replayPage").css("top","50%").addClass("col-sm-offset-6").removeClass("col-sm-offset-3");
            $scope.replayResponseRest = " ";
            $scope.replayResponseFile = " ";
            $scope.replayResponseWs = " ";
            $scope.replayResponseFTP = " ";
            $scope.batchChecker = false;
        };
        $scope.checkAll = function(source){
            var allbox = document.getElementById('replaySelectAll');
            var checkboxes = document.getElementsByName('auditCheckbox');
            for(var i=0, n=checkboxes.length;i<n;i++) {
                if(allbox.checked){
                    checkboxes[i].checked = true;
                }
                else{
                    checkboxes[i].checked = false;
                }
            };
        };
    }]);
