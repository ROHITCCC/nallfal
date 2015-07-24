/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


var sunburstControllerModule = angular.module('sunburstControllerModule', ['ultimotls', 'auditControllerModule', 'ngRoute']);

sunburstControllerModule.controller('sunburstController', ['$scope', 'mongoAggregateService', '$location', '$route','auditQuery', 'sunburstSaver', 'queryEnv',
    function($scope, mongoAggregateService, $location, $route, auditQuery, sunburstSaver, queryEnv){
    // $scope.toDate, $scope.fromDate;
    $scope.toDate = null;
    $scope.fromDate = null;
    $scope.timeOptions = [{"time":.25, "description":"15 minutes"},{"time":.5, "description":"30 minutes"},
                          {"time":1,"description":"1 hour"},{"time":24, "description":"24 hours"},
                          {"time":48,"description":"48 hours"}, {"time":"Calender", "description":"Custom"}];
    $scope.timeSelected = $scope.timeOptions[2];
    $scope.sunburstSaver = sunburstSaver;
    $scope.env = queryEnv.getEnv();
    $scope.auditQuery = auditQuery;
    $scope.$on("envChangeBroadcast", function(){
        $scope.env = queryEnv.getEnv();
        $scope.fromDateChange($scope.timeSelected);
    })
    if(!$scope.toDate){
        var currentDateTime = new Date();
        if(typeof $scope.sunburstSaver.dropdownVal !== 'undefined'){ //checks whether or not the slider value holder in the service exists yet
            for(var i =0; i< $scope.timeOptions.length; i++){
                if ($scope.sunburstSaver.dropdownVal === $scope.timeOptions[i].time){
                    $scope.timeSelected = $scope.timeOptions[i]; 
                }
            }  
            $scope.fromDate = new Date(currentDateTime - ($scope.sunburstSaver.dropdownVal*60*60*1000)).toISOString();  //changes the time accordingly
        }
        else if($scope.timeSelected.time === "Calender"){
            console.log("new event")
        }
        $scope.toDate = new Date(currentDateTime).toISOString();
    }
    var dataQuery = "[{'$match':{'$and':[{'timestamp':{'$gte':{'$date':"+
                         "'"+$scope.fromDate+"'},'$lt':{'$date':'"+$scope.toDate+"'}}},"+
                         "{'$and':[{'severity':{'$ne':null}},{'severity':"+
                         "{'$exists': true,'$ne':''}},{'envid':'"+$scope.env.dbName+"'}]}]}},{'$group':{'_id':{'transactionType'"+
                         ":'$transactionType','interface1':'$interface1','application':"+
                         "'$application'},'count':{'$sum':1}}},{'$group':{'_id':{'transactionType"+
                         "':'$_id.transactionType','application':'$_id.application'},'data':"+
                         "{'$addToSet':{'name':'$_id.interface1','description':{'$literal':"+
                         "'Interface Name'},'size':'$count'}}}},{'$project':{'_id':0,"+
                         "'transactionType':'$_id.transactionType','application':{'name':"+
                         "'$_id.application','description':{'$literal':'Application Name'},"+
                         "'children':'$data'}}},{'$group':{'_id':{'transactionType':"+
                         "'$transactionType'},'children':{'$addToSet':{'children':'$application"+
                         "'}}}},{'$project':{'_id':1,'name':'$_id.transactionType','description'"+
                         ":{'$literal':'Transaction Type'},'children':'$children.children'}}]";
    $scope.sunburstPromise = mongoAggregateService.callHttp(dataQuery);
    //date dropdown Config
    $scope.customDate = function(fromDate, toDate){
        if(!fromDate || !toDate){ //Error handling - A valid date must be entered
            alert("A valid date must be entered for both fields");
            return
        }
        console.log(fromDate, toDate);
        $scope.fromDate = new Date(fromDate).toISOString();
        $scope.toDate = new Date(toDate).toISOString();
        var customDateQuery = "[{'$match':{'$and':[{'timestamp':{'$gte':{'$date':"+
                     "'"+$scope.fromDate+"'},'$lt':{'$date':'"+$scope.toDate+"'}}},"+
                     "{'$and':[{'severity':{'$ne':null}},{'severity':"+
                     "{'$exists': true,'$ne':''}},{'envid':'"+$scope.env.dbName+"'}]}]}},{'$group':{'_id':{'transactionType'"+
                     ":'$transactionType','interface1':'$interface1','application':"+
                     "'$application'},'count':{'$sum':1}}},{'$group':{'_id':{'transactionType"+
                     "':'$_id.transactionType','application':'$_id.application'},'data':"+
                     "{'$addToSet':{'name':'$_id.interface1','description':{'$literal':"+
                     "'Interface Name'},'size':'$count'}}}},{'$project':{'_id':0,"+
                     "'transactionType':'$_id.transactionType','application':{'name':"+
                     "'$_id.application','description':{'$literal':'Application Name'},"+
                     "'children':'$data'}}},{'$group':{'_id':{'transactionType':"+
                     "'$transactionType'},'children':{'$addToSet':{'children':'$application"+
                     "'}}}},{'$project':{'_id':1,'name':'$_id.transactionType','description'"+
                     ":{'$literal':'Transaction Type'},'children':'$children.children'}}]";
        $scope.sunburstPromise = mongoAggregateService.callHttp(customDateQuery);
    }
    $scope.fromDateChange = function(time){
        console.log($scope.env)
        $scope.timeSelected = time;
        if($scope.timeSelected.time === "Calender"){
            $(document).ready(function(){
                $("#calendarPage").modal();
            });
            return
            
        }
        var currentDateTime = new Date();
        $scope.fromDate = new Date(currentDateTime - ($scope.timeSelected.time*60*60*1000)).toISOString();
        $scope.toDate = new Date(currentDateTime).toISOString(); 
        var sliderDataQuery = "[{'$match':{'$and':[{'timestamp':{'$gte':{'$date':"+
                     "'"+$scope.fromDate+"'},'$lt':{'$date':'"+$scope.toDate+"'}}},"+
                     "{'$and':[{'severity':{'$ne':null}},{'severity':"+
                     "{'$exists': true,'$ne':''}},{'envid':'"+$scope.env.dbName+"'}]}]}},{'$group':{'_id':{'transactionType'"+
                     ":'$transactionType','interface1':'$interface1','application':"+
                     "'$application'},'count':{'$sum':1}}},{'$group':{'_id':{'transactionType"+
                     "':'$_id.transactionType','application':'$_id.application'},'data':"+
                     "{'$addToSet':{'name':'$_id.interface1','description':{'$literal':"+
                     "'Interface Name'},'size':'$count'}}}},{'$project':{'_id':0,"+
                     "'transactionType':'$_id.transactionType','application':{'name':"+
                     "'$_id.application','description':{'$literal':'Application Name'},"+
                     "'children':'$data'}}},{'$group':{'_id':{'transactionType':"+
                     "'$transactionType'},'children':{'$addToSet':{'children':'$application"+
                     "'}}}},{'$project':{'_id':1,'name':'$_id.transactionType','description'"+
                     ":{'$literal':'Transaction Type'},'children':'$children.children'}}]";
        $scope.sunburstPromise = mongoAggregateService.callHttp(sliderDataQuery);
        $scope.sunburstSaver.dropdownVal = $scope.timeSelected.time;//Saves TimeSelected when drop down value changes
    };    
}]);
