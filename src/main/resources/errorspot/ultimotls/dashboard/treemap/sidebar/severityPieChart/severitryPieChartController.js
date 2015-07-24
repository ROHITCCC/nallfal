/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
var severityPieChartControllerModule = angular.module('severityPieChartControllerModule', ['ultimotls']);

severityPieChartControllerModule.controller('severityPieChartController', ['$scope', 'mongoAggregateService', 'treemapSaver','queryEnv','timeService',
    function($scope, mongoAggregateService, treemapSaver, queryEnv, timeService){
    //service to get current Time
    var time = timeService.getTime();
    $scope.toDate = time.toDate;
    $scope.fromDate = time.fromDate;
    //service to get current Environment
    $scope.env = queryEnv.getEnv();
    
    $scope.treemapSaver = treemapSaver;
    
    //This will make our initial call when our page is first loaded
    var dataQuery = "[{\"$match\":{\"$and\":[{\"timestamp\":{\"$gte\":{\"$date\":\""+$scope.fromDate+"\"},\"$lt\":{\"$date\":\""+
        $scope.toDate+"\"}}},{\"$and\":[{\"severity\":{\"$ne\":null}}, {\"severity\":{\"$exists\":true,\"$ne\":\"\"}},{\"envid\":\""+
        $scope.env.name+"\"}]}]}},{$group:{_id:\"$severity\", count:{$sum:1}}}]";  
    $scope.severityPieChartPromise = mongoAggregateService.callHttp(dataQuery);
    //need a service to pass timeChange
    $scope.$on("timeChangeBroadcast", function(){//Listens for Time Change
        var timeTemp = timeService.getTime();
        $scope.toDate = timeTemp.toDate;
        $scope.fromDate = timeTemp.fromDate;
        $scope.severityPieChartData($scope.fromDate, $scope.toDate);
    })
    //Listening to the environment change broadcast
    $scope.$on("envChangeBroadcast", function(){//Listens for Environment Change
        $scope.env = queryEnv.getEnv();
        $scope.severityPieChartData($scope.fromDate, $scope.toDate);
    })
    //Function that will send our promise to the value that our directive is listening for
    $scope.severityPieChartData = function(fromDate, toDate){
        var dataQuery = "[{\"$match\":{\"$and\":[{\"timestamp\":{\"$gte\":{\"$date\":\""+fromDate+"\"},\"$lt\":{\"$date\":\""+
            toDate+"\"}}},{\"$and\":[{\"severity\":{\"$ne\":null}}, {\"severity\":{\"$exists\":true,\"$ne\":\"\"}},{\"envid\":\""+
            $scope.env.name+"\"}]}]}},{$group:{_id:\"$severity\", count:{$sum:1}}}]";  
        $scope.severityPieChartPromise = mongoAggregateService.callHttp(dataQuery);
    };
}]);