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
        }
        //start = +start; 
        start = parseInt(start, 10);
        return input.slice(start);
    };
});

auditControllerModule.controller('DataRetrieve', ['$scope', '$log', '$http', 'auditSearch', 'initPromise', 'queryEnv', 'treemapSaver','resetTimerService',
    function ($scope, $log, $http, auditSearch, initPromise, queryEnv, treemapSaver,resetTimerService) {
        //Initialize scope data 
        $scope.rowsOptions = [{rows: 5}, {rows: 10}, {rows: 25}, {rows: 50}, {rows: 100}];
        $scope.rowNumber = $scope.rowsOptions[2];
        $scope.predicate = 'timestamp.$date';
        $scope.replayQueryHolder = "";
        //Replay Page Options
        $scope.replayOptions = [{type: "REST"}, {type: "FILE"}, {type: "WS"}, {type: "FTP"}];
        $scope.replayType = $scope.replayOptions[0];
        //For Custom Field
        $scope.curCustomPage = 0, $scope.curNameValuePage = 0;
        $scope.pageSize = 2;
        $scope.batchChecker = false;
        $scope.treemapSaver = treemapSaver;
        $scope.headerCounter = 0;
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
        //check if initPromise from resolve has data.
        if (initPromise && initPromise.data) {
            var queryFromResolve = initPromise.config.url;
            $scope.searchCriteria = queryFromResolve.substring(queryFromResolve.indexOf(',')+1, queryFromResolve.lastIndexOf('}') - 1);
            $scope.data = initPromise.data;
        }
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
            searchPromise.then(function (response) {
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
            }
            //var currentCustomIndex = index + ($scope.curCustomPage*2);  //for pagination
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
            }
            if(customFieldQuery[0].name && customFieldQuery[0].value){
                checkCustomFieldFlag = true;
                return checkCustomFieldFlag;
            }
            $scope.errorWarning = "Both name and value must be enter before search can be performed";
            return checkCustomFieldFlag;
        }
        function appendCustomField(){
            $scope.customFieldString = "";
            for(var i = 0; i< $scope.customFieldLength; i++){
                $scope.customFieldString = $scope.customFieldString+"\"customFields."+$scope.customField[i].name+"\":\""+$scope.customField[i].value+"\",";
            }
            return $scope.customFieldString;
        }
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
            //var curNameValueIndex = index + ($scope.curNameValuePage*2)   //for pagination
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
        }
        function appendNameValueField(){
            $scope.nameValueFieldString = "";
            for(var i = 0; i<$scope.nameValueFieldLength; i++){
                $scope.nameValueFieldString = $scope.nameValueFieldString+"\""+$scope.nameValueField[i].name+"\":\""+$scope.nameValueField[i].value+"\",";
            }
            return $scope.nameValueFieldString;
        }
        
        function checkObj(advanceSearch) {
            /* function to validate the existence of each key in the object to get the number of valid keys. */
            var checkObjFlag = false;
            if(!advanceSearch){
                return false;
            }
            if(advanceSearch.keyword) {
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
            return checkObjFlag; keywordFlag;
        };
        function appendFields(advanceSearch){
            var string = "";
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
            }
            if(advanceNameValueFieldObjectFlag){
                nameValueQuery = appendNameValueField();
                doAdvanceSearch = true;
            }
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
            }
            if (dbType === "payload" && !advanceSearchObjectflag){
                $scope.errorWarning = "Keyword must be entered for Payload Search";
                return;
            }
            if (advanceSearchObjectflag){
                query = appendFields($scope.advanceSearch); //removes last comma in the JSON query
                doAdvanceSearch = true;
                var keyPhrase = $scope.advanceSearch.keyword;
                if (keywordFlag || dbType === "payload"){
                    urlParam = "&searchtype=advanced&count&pagesize="+$scope.rowNumber.rows+"&searchdb="+dbType+"&searchkeyword="+keyPhrase;
                }
                if(dbType === "payload" && keyPhrase === (""||undefined)){
                  
                };
            };
            //GENERATE FINAL QUERY
            var finalAdvanceSearchQuery = "?filter={\"$and\":[{"+(envQuery+query+customQuery+nameValueQuery+dateQuery).slice(0,-1)+"}]}";
            $scope.replayQueryHolder = finalAdvanceSearchQuery // for replay services
            //PERFORM GET CALL
            if(doAdvanceSearch){
                $scope.errorWarning = "";
                var advanceSearchUrl = getURL+finalAdvanceSearchQuery+urlParam;
                $http.get(advanceSearchUrl, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function (response,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        $scope.data = response;
                        $scope.errorWarning = "";
                    }).error(function(d){
                        $scope.errorWarning = "Call Timed Out";
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
            $scope.batchChecker = false;
            document.getElementById("replayResponseRest").innerHTML = " ";
            document.getElementById("replayResponseFile").innerHTML = " ";
            document.getElementById("replayResponseWs").innerHTML = " ";
            document.getElementById("replayResponseFTP").innerHTML = " ";
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
                    if($scope.relatedTransactionData.length === 1){//need a service to check for duplicate values and single returns
                        console.log($scope.relatedTransactionData._id.$oid);
                        
                    }
            });
        };
        //From relatedTransaction a click function will open a new Modal page and populated new data
        $scope.relatedSearch = function(rowData){
            $scope.relatedSearchData = rowData;
        };
        $scope.callPayload = function(data){ //from Database Page datalocation makes a call
            var dataLocationId = data;
            console.log(dataLocationId);
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
        var replayPostUrlBatch = TLS_PROTOCOL+"://"+TLS_SERVER+":"+TLS_PORT+"/_logic/ReplayService?batch=true";
        $scope.runRestService = function(){//only takes JSON files not 
            if($scope.batchChecker === false){
                var headerType = null;
                var headerVal = null;
                var methodVal = document.getElementById("replayDropDownMethod").value;
                var contentVal = document.getElementById("replayDropDownApplication").value;
                var headerHolder = null;
                
                if(methodVal === "other")methodVal = document.getElementById("methodValue").value;
                if(contentVal === "other")contentVal = document.getElementById("contentType").value;
                
                if($scope.restReplay.header === undefined || $scope.restReplay.header === null){
                    headerType = "Authorization";
                    headerVal = "";
                }else{
                    headerType = $scope.restReplay.header.type;
                    headerVal = $scope.restReplay.header.value;
                }
                
                headerHolder = '"type"="'+headerType+'", "value"="'+headerVal+'"';
                if($scope.headerCounter > 0){
                    for(var z = 0; z < $scope.headerCounter; z++){
                        var tempType = document.getElementById("headerType" + (z)).value;
                        var tempVal = document.getElementById("headerValue" + (z)).value;
                        console.log(tempType, tempVal);
                        if(tempType === "")tempType = "Authorization";
                        headerHolder += ', "type"="'+tempType+'", "value"="'+headerVal+'"';
                        console.log(tempType);
                    }
                }
                
                console.log(headerType);
                var restPayload = 'type=REST~, endpoint='+$scope.restReplay.endpointUrl+'~, method='+
                    methodVal+'~, content-type='+contentVal+'~, payload='+$scope.payloadPageData+
                    '~, header=['+headerHolder+']';
            $http.post(replayPostUrl, restPayload, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseRest").innerHTML = "Rest Replay Success";
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseRest").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                
                var headerType = null;
                var headerVal = null;
                var methodVal = document.getElementById("replayDropDownMethod").value;
                var contentVal = document.getElementById("replayDropDownApplication").value;
                
                if(methodVal === "other")methodVal = document.getElementById("methodValue").value;
                if(contentVal === "other")contentVal = document.getElementById("contentType").value;
                
                if($scope.restReplay.header === undefined){
                    headerType = "Authorization";
                    headerVal = "";
                }else{
                    headerType = $scope.restReplay.header.type;
                    headerVal = $scope.restReplay.header.value;
                }
                
                var restPayload = '"type": "REST", "endpoint": "'+$scope.restReplay.endpointUrl+'", '+
                        '"method": "'+methodVal+'","contentType": "'+contentVal+'",'+
                        '"headers":{ "'+headerType+'":"'+headerVal+'"}';

                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "replayDestinationInfo": { '+restPayload+' },'+
                                    '"auditID": ['+auditIDs+']}';
                console.log(batchPayload);
                $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseRest").innerHTML = "Success: " + d;
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseRest").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }
            
        };
        $scope.fileReplay = {};
        $scope.runFileService = function(){ //how do i set a file location
            
            if($scope.batchChecker === false){
                var filePayload = "type=FILE~, file-location="+$scope.fileReplay.location+"~, payload="+$scope.payloadPageData.payload+"";
                $http.post(replayPostUrl, filePayload, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFile").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var filePayloadBatch = '"type":"FILE", "file-location":"'+$scope.fileReplay.location+'"';
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "replayDestinationInfo": { '+filePayloadBatch+' },'+
                                    '"auditID": ['+auditIDs+']}';
                $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFile").innerHTML = "Success: " + d;
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFile").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }
            
        };
        $scope.webServiceReplay = {};
        $scope.runWebService = function(){
            
            if($scope.batchChecker === false){
                var webServicePayload = "type=WS~, wsdl="+$scope.webServiceReplay.wsdl+"~, operation="+$scope.webServiceReplay.operation+
                    "~,  soapaction="+$scope.webServiceReplay.soapAction+"~, binding="+$scope.webServiceReplay.binding+"~, payload="+
                    $scope.payloadPageData.payload;
                $http.post(replayPostUrl, webServicePayload, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseWs").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var webServicePayloadBatch = '"type":"WS", "wsdl":"'+$scope.webServiceReplay.wsdl+'", "operation":"'+$scope.webServiceReplay.operation+'",' + 
                    '"soapaction":"'+$scope.webServiceReplay.soapAction+'", "binding":"'+$scope.webServiceReplay.binding+'"';
                
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "replayDestinationInfo": { '+webServicePayloadBatch+' },'+
                                    '"auditID": ['+auditIDs+']}';
                $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseWs").innerHTML = "Success: " + d;
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseWs").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }
            
        };
        $scope.ftpServiceReplay = {};
        $scope.runFTPService = function(){
            var checkRest = $scope.checkChecked();
            if($scope.batchChecker === false){
                var ftpPayload = "type=FTP~, host="+$scope.ftpServiceReplay.host+"~, username="+$scope.ftpServiceReplay.username+"~, password="+
                    $scope.ftpServiceReplay.password+"~, location="+$scope.ftpServiceReplay.location+"~, fileType="+$scope.ftpServiceReplay.fileType+
                    "~, header=[\"type\"=\""+$scope.ftpServiceReplay.headerType+"\",\"value\"=\""+
                    $scope.ftpServiceReplay.headerValue+"\"]";
                $http.post(replayPostUrl, ftpPayload, {timeout:TLS_SERVER_TIMEOUT})
                    .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFTP").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }
            else{
                var batchVals = $scope.batchValues();
                var auditIDs = $scope.pullAuditIDs(batchVals[2]);
                var ftpPayloadBatch = '"type":"FTP", "host":"'+$scope.ftpServiceReplay.host+'", "username":"'+$scope.ftpServiceReplay.username+'",'+
                    '"password":"'+$scope.ftpServiceReplay.password+'", "location":"'+$scope.ftpServiceReplay.location+'", "fileType":"'+$scope.ftpServiceReplay.fileType+'",' + 
                    '"payload":"'+$scope.replayQueryHolder+'", "header":"["type":\"'+$scope.ftpServiceReplay.headerType+'","value":"'+$scope.ftpServiceReplay.headerValue+'"]"';
                var batchPayload = '{  "replaySavedTimestamp":"'+batchVals[0]+'",  "replayedBy":"'+batchVals[1]+'", '+
                        '"batchProcessedTimestamp":"", "replayDestinationInfo": { '+ftpPayloadBatch+' },'+
                                    '"auditID": ['+auditIDs+']}';
                $http.post(replayPostUrlBatch, batchPayload, {timeout:TLS_SERVER_TIMEOUT})
                        .success(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFTP").innerHTML = "Success: " + d;
                        console.log(d);
                    }).error(function(d,status, header, config){
                        var auth_token_valid_until = header()['auth-token-valid-until'];
                        resetTimerService.set(auth_token_valid_until);
                        document.getElementById("replayResponseFTP").innerHTML = "Error: " + d["http status code"] + ": " + d["message"];
                        console.log(d);
                    });
            }
        };
        $scope.changeReplay = function(){
            console.log($scope.batchChecker);
            $scope.batchChecker = true;
            console.log($scope.batchChecker);
            if($scope.treemapSaver.checkboxChecked !== undefined){
                $("#replayPage").css("top","15%").addClass("col-sm-offset-3").removeClass("col-sm-offset-6");
                document.getElementById("replayResponseRest").innerHTML = " ";
                document.getElementById("replayResponseFile").innerHTML = " ";
                document.getElementById("replayResponseWs").innerHTML = " ";
                document.getElementById("replayResponseFTP").innerHTML = " ";
            }
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
            }
        };
        $scope.checkChecked = function(){
            var isChecked = false;
            var checkboxes = document.getElementsByName('auditCheckbox');
            var auditData = $scope.treemapSaver.auditData._embedded['rh:doc'];
            for(var i=0, n=checkboxes.length;i<n;i++) {
                if(checkboxes[i].checked){
                    isChecked = true;
                }
            }
            return isChecked;
        };
        $scope.addHeaders = function(){
            $("#headerType").clone(false).prop("id","headerType" + $scope.headerCounter).css("left", "16.5%").appendTo("#restHeaderDiv");
            $("#headerValue").clone(false).prop("id","headerValue" + $scope.headerCounter).css("left", "16.5%").appendTo("#restHeaderDiv");
            $("#deleteHeader").clone(false).prop("id","deleteHeader" + $scope.headerCounter).css("display","inline").appendTo("#restHeaderDiv");
            
            $scope.headerCounter++;
        };
//        $scope.deleteHeaders = function(){
//            $scope.headerCounter--;
//            console.log("here");
//            $("#headerType" + $scope.headerCounter).remove();
//            $("#headerValue" + $scope.headerCounter).remove();
//            $("#deleteHeader" + $scope.headerCounter).remove();
//            
//        };
        $scope.checkSelected = function(){
            var methodVal = document.getElementById("replayDropDownMethod");
            var contentVal = document.getElementById("replayDropDownApplication");
            var contentValText = document.getElementById("contentType");
            var methodValText = document.getElementById("methodValue");
            
            if(methodVal.value === "other"){
                methodValText.style.display = "inline";
            }else{
                methodValText.style.display = "none";
            }
            if(contentVal.value === "other"){
                contentValText.style.display = "inline";
            }else{
                contentValText.style.display = "none";
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
                    console.log(auditData[i]._id.$oid);
                    auditIDs.push(auditData[i]._id.$oid);
                }
            }
            console.log(auditIDs);
            var batchVals = [timestamp, username, auditIDs];
            return batchVals;
        };
        $scope.pullAuditIDs = function(batchVals){
            var auditIDs = null;
            for(var z = 0; z < batchVals.length; z++){
                if(z > 0){
                    auditIDs += ',"'+batchVals[z]+'"';
                }else{
                    auditIDs = '"'+batchVals[z]+'"';
                }
            }
            return auditIDs;
        };
        $scope.changeReplayBack = function(){
            $("#replayPage").css("top","50%").addClass("col-sm-offset-6").removeClass("col-sm-offset-3");
            document.getElementById("replayResponseRest").innerHTML = " ";
            document.getElementById("replayResponseFile").innerHTML = " ";
            document.getElementById("replayResponseWs").innerHTML = " ";
            document.getElementById("replayResponseFTP").innerHTML = " ";
            $scope.batchChecker = false;
        };
        $scope.checkAll = function(source){
            var allbox = document.getElementById('replaySelectAll');
            var checkboxes = document.getElementsByName('auditCheckbox');
            for(var i=0, n=checkboxes.length;i<n;i++) {
                if(allbox.checked){
                    checkboxes[i].checked = true;
                }else{
                    checkboxes[i].checked = false;
                }
              
            }
        };
    }]);
