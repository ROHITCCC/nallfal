var errorPieChartControllerModule = angular.module('errorPieChartControllerModule', ['ultimotls']);

errorPieChartControllerModule.controller('errorPieChartController', ['$scope', 'mongoAggregateService', 'treemapSaver' ,'queryEnv','timeService',
    function($scope, mongoAggregateService, treemapSaver, queryEnv, timeService){
    //service to get current Time
    var time = timeService.getTime();
    $scope.toDate = time.toDate;
    $scope.fromDate = time.fromDate;
    //service to get current Environment
    $scope.env = queryEnv.getEnv();
    
    $scope.treemapSaver = treemapSaver;
    var dataQuery = "[{\"$match\":{\"$and\":[{\"timestamp\":{\"$gte\":{\"$date\":\""+$scope.fromDate+"\"},\"$lt\":{\"$date\":\""+
            $scope.toDate+"\"}}},{\"$and\":[{\"errorType\":{\"$ne\":null}}, {\"errorType\":{\"$exists\":true,\"$ne\":\"\"}},{\"envid\":\""+
            $scope.env.name+"\"}]}]}},{$group:{_id:\"$errorType\", count:{$sum:1}}}]";  
    $scope.errorPieChartPromise = mongoAggregateService.callHttp(dataQuery);
    //need a service to pass timeChange
    $scope.$on("timeChangeBroadcast", function(){//Listens for Time Change
        var timeTemp = timeService.getTime();
        $scope.toDate = timeTemp.toDate;
        $scope.fromDate = timeTemp.fromDate;
        $scope.errorPieChartData($scope.fromDate, $scope.toDate);
    })
    //Listening to the environment change broadcast
    $scope.$on("envChangeBroadcast", function(){//Listens for Environment Change
        $scope.env = queryEnv.getEnv();
        $scope.errorPieChartData($scope.fromDate, $scope.toDate);
    })
    //Function that will send our promise to the value that our directive is listening for
    $scope.errorPieChartData = function(fromDate, toDate){
        var dataQuery = "[{\"$match\":{\"$and\":[{\"timestamp\":{\"$gte\":{\"$date\":\""+fromDate+"\"},\"$lt\":{\"$date\":\""+
            toDate+"\"}}},{\"$and\":[{\"errorType\":{\"$ne\":null}}, {\"errorType\":{\"$exists\":true,\"$ne\":\"\"}},{\"envid\":\""+
            $scope.env.name+"\"}]}]}},{$group:{_id:\"$errorType\", count:{$sum:1}}}]";  
        $scope.errorPieChartPromise = mongoAggregateService.callHttp(dataQuery);
    };
}]);