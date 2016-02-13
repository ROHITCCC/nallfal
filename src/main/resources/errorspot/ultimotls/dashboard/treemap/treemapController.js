/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var treemapControllerModule = angular.module('treemapControllerModule', ['ultimotls', 'auditControllerModule', 'ngRoute']);

treemapControllerModule.controller('treemapController', ['$scope', 'mongoAggregateService', 'treemapSaver', 'auditQuery','queryEnv','timeService','queryFilter',
    function($scope, mongoAggregateService, treemapSaver, auditQuery, queryEnv, timeService, queryFilter){
    $scope.toDate = null;
    $scope.fromDate = null;
    $scope.valid = "";
    $scope.timeOptions = [{"time":.25, "description":"15 minutes"},{"time":.5, "description":"30 minutes"},
                          {"time":1,"description":"1 hour"},{"time":24, "description":"24 hours"},
                          {"time":48,"description":"48 hours"}, {"time":"Calender", "description":"Custom"}];
    $scope.timeSelected = $scope.timeOptions[2];
    if(!$scope.env){
        $scope.env = queryEnv.getEnv();
    }
    $scope.treemapSaver = treemapSaver;
    $scope.treemapSaver.dropdownClicked = true;
    $scope.auditQuery = auditQuery;
    $scope.$on("envChangeBroadcast", function(){//Listens for Environment Change
        $scope.env = queryEnv.getEnv();
        $scope.fromEnvChange($scope.timeSelected);
    });
    $scope.$on("newFilterAppended", function(){
        $scope.env = queryEnv.getEnv();
        $scope.newFilter = queryFilter.appendQuery();
        var newDataQuery = "[{\"$match\":{\"$and\":[{\"timestamp\":{\"$gte\":{\"$date\":\""+$scope.fromDate+"\"},\"$lt\":{\"$date\":\""+
                            $scope.toDate +"\"}}},{\"$and\":[{\"severity\":{\"$ne\": null}},{\"severity\":{\"$exists\":true,\"$ne\":\"\"}},{"+$scope.newFilter+"\"envid\":\""+
                            $scope.env.name+"\"}]}]}},{\"$group\":{\"_id\":{\"interface1\":\"$interface1\",\"application\":\"$application\"},"+
                            "\"count\":{\"$sum\":1}}},{\"$group\":{\"_id\":{\"application\":\"$_id.application\"},\"data\":{\"$addToSet\":{\"name\":\"$_id.interface1\""+
                            ",\"size\":\"$count\"}}}},{\"$project\":{\"_id\":1,\"name\":\"$_id.application\",\"children\":\"$data\"}}]";
        $scope.treemapPromise = mongoAggregateService.callHttp(newDataQuery);
    });
    calculateTime = function(timeSelected){
        timeObj = {};
        var currentDateTime = new Date();
        timeObj.fromDate = new Date(currentDateTime - (timeSelected*60*60*1000)).toISOString();
        timeObj.toDate = new Date(currentDateTime).toISOString();
        return timeObj;
    };
    if($scope.treemapSaver.wordLength === undefined)$scope.treemapSaver.wordLength = [];
    if(!$scope.toDate){
        var currentDateTime = new Date();
        if(typeof $scope.treemapSaver.dropdownVal !== 'undefined'){ //checks whether or not the slider value holder in the service exists yet
            for(var i =0; i< $scope.timeOptions.length; i++){
                if ($scope.treemapSaver.dropdownVal === $scope.timeOptions[i].time){
                    $scope.timeSelected = $scope.timeOptions[i]; 
                }
            }; 
            $scope.fromDate = new Date(currentDateTime - ($scope.treemapSaver.dropdownVal*60*60*1000)).toISOString();  //changes the time accordingly
        }
        else{
            $scope.fromDate = new Date(currentDateTime - 3600000).toISOString(); //Current minus 2 hours           
        }
        $scope.toDate = new Date(currentDateTime).toISOString();
        timeService.setTime($scope.fromDate, $scope.toDate);
        timeService.broadcast();
    }
    var dataQuery = "[ { '$match': { '$and': [ { 'timestamp': { '$gte': " +
                    "{'$date': '"+$scope.fromDate+"'},'$lt':{'$date':'"+ $scope.toDate +"'}}},{'$and':[{'severity':{'$ne':null}},{'severity':{'$exists':true,'$ne':''}},{'envid':'"+$scope.env.name+"'}]}]}},{'$group':{'_id':{'interface1':'$interface1','application':'$application'},'count':{'$sum':1}}},{'$group':{'_id':{'application':'$_id.application'},'data':{'$addToSet':{'name':'$_id.interface1','size':'$count'}}}},{'$project':{'_id':1,'name':'$_id.application','children':'$data'}}]";      
    $scope.treemapPromise = mongoAggregateService.callHttp(dataQuery);
    //date dropdown Config
    $scope.customDate = function(fromDate, toDate){
        if(!fromDate || !toDate){ //Error handling - A valid date must be entered
            alert("A valid date must be entered for both fields");
            return;
        }
        $scope.fromDate = new Date(fromDate).toISOString();
        $scope.toDate = new Date(toDate).toISOString();
        timeService.setTime($scope.fromDate, $scope.toDate);
        timeService.broadcast();
        var customDateQuery = "[ { '$match': { '$and': [ { 'timestamp': { '$gte': " +
                    "{'$date':'"+$scope.fromDate+"'},'$lt':{'$date':'"+ $scope.toDate +"'}}},{'$and':[{'severity':{'$ne':null}},{'severity':{'$exists':true,'$ne':''}},{'envid':'"+$scope.env.name+"'}]}]}},{'$group':{'_id':{'interface1':'$interface1','application':'$application'},'count':{'$sum':1}}},{'$group':{'_id':{'application':'$_id.application'},'data':{'$addToSet':{'name':'$_id.interface1','size':'$count'}}}},{'$project':{'_id':1,'name':'$_id.application','children':'$data'}}]";         
        $scope.treemapSaver.dropdownClicked = true;
        if($scope.fromDate < $scope.toDate){
            $scope.valid = "";
            $scope.treemapPromise = mongoAggregateService.callHttp(customDateQuery);
            var displayFromDate = new Date(fromDate).toDateString().substr(4);
            var displayToDate = new Date(toDate).toDateString().substr(4);
            document.getElementById("customDateTimes").innerHTML = displayFromDate+" - "+displayToDate;
            $( "#closeModal" ).click();
        }else{
            $scope.valid = "'To' must be a date before 'From'";
        }
        
    };
    $scope.fromDateChange = function(time){
        $scope.timeSelected = time;
        if($scope.timeSelected.time === "Calender"){
            $(document).ready(function(){
                $("#calendarPage").modal();
            });
            $scope.valid = "";
            return;
        };
        var timeCalulated = calculateTime(time.time);
        $scope.fromDate = timeCalulated.fromDate;
        $scope.toDate = timeCalulated.toDate;
        timeService.setTime($scope.fromDate, $scope.toDate);
        timeService.broadcast();
        var sliderDataQuery = "[ { '$match': { '$and': [ { 'timestamp': { '$gte': " +
                    "{'$date': '"+$scope.fromDate+"'}, '$lt': {'$date': '"+ $scope.toDate +"'} } }, { '$and': [ {'severity': {'$ne': null}}, {'severity': {'$exists': true, '$ne': ''}},{'envid':'"+$scope.env.name+"'} ] } ] } },{ '$group': { '_id' : { 'interface1': '$interface1', 'application': '$application' }, 'count': {'$sum': 1} } } , { '$group': { '_id' : { 'application': '$_id.application' }, 'data': { '$addToSet':{ 'name': '$_id.interface1', 'size': '$count' } } } } , { '$project': { '_id': 1, 'name': '$_id.application', 'children': '$data' } } ]";
        $scope.treemapSaver.dropdownClicked = true;
        $scope.treemapPromise = mongoAggregateService.callHttp(sliderDataQuery);
        $scope.treemapSaver.dropdownVal = $scope.timeSelected.time;//Saves TimeSelected when drop down value changes
        document.getElementById("customDateTimes").innerHTML = "";
    };
    $scope.fromEnvChange = function(time){
        $scope.timeSelected = time;
        if($scope.timeSelected.time === "Calender"){
            $(document).ready(function(){
                $("#calendarPage").modal();
            });
            return;
        };
        var timeCalulated = calculateTime(time.time);
        $scope.fromDate = timeCalulated.fromDate;
        $scope.toDate = timeCalulated.toDate;
        timeService.setTime($scope.fromDate, $scope.toDate);
        var sliderDataQuery = "[ { '$match': { '$and': [ { 'timestamp': { '$gte': " +
                    "{'$date': '"+$scope.fromDate+"'}, '$lt': {'$date': '"+ $scope.toDate +"'} } }, { '$and': [ {'severity': {'$ne': null}}, {'severity': {'$exists': true, '$ne': ''}},{'envid':'"+$scope.env.name+"'} ] } ] } },{ '$group': { '_id' : { 'interface1': '$interface1', 'application': '$application' }, 'count': {'$sum': 1} } } , { '$group': { '_id' : { 'application': '$_id.application' }, 'data': { '$addToSet':{ 'name': '$_id.interface1', 'size': '$count' } } } } , { '$project': { '_id': 1, 'name': '$_id.application', 'children': '$data' } } ]";
        $scope.treemapSaver.dropdownClicked = true;
        $scope.treemapPromise = mongoAggregateService.callHttp(sliderDataQuery);
        $scope.treemapSaver.dropdownVal = $scope.timeSelected.time;//Saves TimeSelected when drop down value changes
        document.getElementById("customDateTimes").innerHTML = "";
    };
    }]);
