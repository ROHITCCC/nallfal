/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
var settingModule = angular.module('settingModule', []);

settingModule.filter('pagination', function () {
    return function (input, start)
    {
        if (!input || !input.length) {
            return;
        }
        start = parseInt(start, 10);
        return input.slice(start);
    };
});
settingModule.directive('uppercased', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, modelCtrl) {
            modelCtrl.$parsers.push(function (input) {
                return input ? input.toUpperCase() : "";
            });
            element.css("text-transform", "uppercase");
        }
    };
});

settingModule.directive('confirmationNeeded', function(){
   return {
       priority: 1,
       terminal: true,
       link: function(scope, element, attr){
           var msg = attr.confirmationNeeded || "Are you sure?";
           var clickAction = attr.ngClick;
           element.bind ('click', function(){
               if (window.confirm(msg)){
                   scope.$eval(clickAction);
               }
           });
       }
   } 
});

settingModule.controller('SettingsController', function ($scope, $http) {
    var settingURL = TLS_PROTOCOL + "://" + TLS_SERVER + ":" + TLS_PORT + "/_logic/" + TLS_DBNAME + "/" + TLS_SETTING_COLLECTION + "/SettingService";
    var schedulerURL = TLS_PROTOCOL + "://" + TLS_SERVER + ":" + TLS_PORT + "/_logic/SchedulerService";
    $scope.settings = {};
    $scope.reports = {};
    $scope.temprep = {};
    $scope.curPage = 0;
    $scope.pageSize = 50;
    $scope.curPageImmi = 0;
    $scope.units = ['sec', 'min', 'hrs', 'day'];
    $scope.numbers = ['3', '5', '10', '15', '20', '30', '40', '50', '100', '200'];
    $scope.selectedNumber = $scope.numbers[7];
    $scope.selectedNumberAggri = $scope.numbers[4];
    $scope.curPageAggri = 0;
    $scope.pageSizeAggri = 4;

//////////////////////////////////////SETTINGS//////////////////////////////////////////
    $scope.settingPromise = function () {
        var promise = $http.get(settingURL + "?object=setting").success(function (data, status) {
        });
        return promise;
    };
    $scope.settingPromise().then(function (data) {
        $scope.settings = data.data._embedded['rh:doc'][0];
        if ($scope.settings.setting.envsetup === undefined) {
            $scope.settings.setting.envsetup = [{name: '', description: '', label: ''}];
            $scope.environments = $scope.settings.setting.envsetup;
        } else {
            $scope.environments = $scope.settings.setting.envsetup;
        }
        ;
        if ($scope.settings.setting.notification === undefined) {
            $scope.settings.setting.notification = {immidate: {frequency: {duration: '', unit: ''}, notification: [{severity: '', email: '', application: {name: '', interfaces: ['']}}]}};
            $scope.notifications = $scope.settings.setting.notification;
        } else {
            $scope.notifications = $scope.settings.setting.notification;
        }

    });
    $scope.settingPromise().catch(function () {
        $scope.newsettingcreator = 1;
        newsetting = {setting: {apisetup: {hostname: '', port: '', database: '', collections: {payload: '', audits: ''}}, notification: {immidate: {frequency: {duration: '', unit: ''}, notification: [{envid: '', severity: '', email: '', application: {name: '', interfaces: ['']}}]}}, envsetup: [{name: '', description: '', label: ''}]}};
        $scope.settings = newsetting;
        $scope.environments = $scope.settings.setting.envsetup;
        $scope.notifications = $scope.settings.setting.notification;
    });
    $scope.settingPromise().finally(function () {

        ////Immidate tools
        $scope.addNewImmidate = function () {
            newson = {envid: '', severity: '', email: '', template: '', application: {name: '', interfaces: ['']}};
            $scope.notifications.immidate.notification.push(newson);
        };
        $scope.addImmidateInterface = function (upindex) {
            if ($scope.curPageImmi >= 1) {
                temp = ($scope.curPageImmi * $scope.pageSizeImmi) + upindex;
                $scope.notifications.immidate.notification[temp].application.interfaces.push({});
            } else {
                $scope.notifications.immidate.notification[upindex].application.interfaces.push('');
            }
        };
        $scope.removeImmidateInterface = function (upindex, index) {
            if ($scope.curPageImmi >= 1) {
                temp = ($scope.curPageImmi * $scope.pageSizeImmi) + upindex;
                $scope.notifications.immidate.notification[temp].application.interfaces.splice(index, 1);
            } else {
                $scope.notifications.immidate.notification[upindex].application.interfaces.splice(index, 1);
            }
        };
        $scope.removeImmidate = function (index) {
            $scope.notifications.immidate.notification.splice(index, 1);
        };
        //Environment tools
        $scope.addNewEnv = function () {
            newson = {name: '', description: '', label: ''};
            $scope.environments.push(newson);
        };
        $scope.removeEnv = function (index) {
            $scope.environments.splice(index, 1);
        };
        $scope.numberOfPagesEnv = function () {
            return Math.ceil($scope.environments.length / $scope.pageSize);
        };
        $scope.savesetting = function () {
            $scope.temp = $scope.settings;
            $scope.savedata($scope.temp);
        };
        $scope.numberOfPagesImmi = function () {
            $scope.pageSizeImmi = $scope.selectedNumber;
            return Math.ceil($scope.notifications.immidate.notification.length / $scope.pageSizeImmi);
        };
        //Env dropdown
        $scope.envDropdown = angular.copy($scope.environments);
    });

//////////////////////////////////////REPORT////////////////////////////////////////////    
    $scope.reportPromise = function () {
        var reportpromise = $http.get(settingURL + "?object=report").success(function (data, status) {
        });
        return reportpromise;
    };
    $scope.reportPromise().then(function (data) {
        $scope.reports = data.data._embedded['rh:doc'];
    });
    $scope.reportPromise().catch(function () {
        $scope.newreport = 1;
        $scope.reports = [{report: {envid: '', application: '', interface1: '', errorType: '', frequency: {starttime: '', duration: '', unit: ''}, email: '', template: ''}}];
    });
    $scope.reportPromise().finally(function () {
        $scope.addNewAggrigated = function () {
            newson = {report: {envid: null, application: null, email: null, interface1: null, errorType: null, frequency: {duration: null, starttime: null, unit: null}}};
            $scope.reports.push(newson);
        };
        $scope.removeAggrigated = function (index) {
            if ($scope.curPageAggri >= 1) {
                temp = ($scope.curPageAggri * $scope.pageSizeAggri) + index;
                $scope.delrowreport(temp);
            } else {
                $scope.delrowreport(index);
            }
        };
        $scope.numberOfPagesAggri = function () {
            $scope.pageSizeAggri = $scope.selectedNumberAggri;
            return Math.ceil($scope.reports.length / $scope.pageSizeAggri);
        };

        $scope.validatereport = function (object, index) {
            if (object.envid === undefined || object.application === '' ||
                    object.application === undefined || object.application === '' ||
                    object.frequency.duration === undefined || object.frequency.duration === '' ||
                    object.frequency.unit === undefined || object.frequency.unit === '' ||
                    object.email === undefined || object.email === '' ||
                    object.interface1 === undefined || object.interface1 === '' ||
                    object.errorType === undefined || object.errorType === '') {
                $('#validaterror').modal();
            } else {
                if (object.frequency.starttime) {
                    object.frequency.starttime = object.frequency.starttime.replace(/ /g, "T");
                }
                $scope.temprep.report = object;
                if ($scope.reports[index]._id !== undefined) {
                    $scope.temprep._id = {$oid: $scope.reports[index]._id.$oid};
                }
                $scope.savedata($scope.temprep, index);
            }
        };
        $scope.saveAggrigated = function (index) {
            if ($scope.curPageAggri >= 1) {
                temp = ($scope.curPageAggri * $scope.pageSizeAggri) + index;
                $scope.validatereport($scope.reports[temp].report, temp);
            } else {
                $scope.validatereport($scope.reports[index].report, index);
            }
        };

        $scope.delrowreport = function (index) {
            if ($scope.reports[index]._id !== undefined) {
                $scope.temprep._id = {$oid: $scope.reports[index]._id.$oid};
                $scope.temprep.report = $scope.reports[index].report;
                $scope.delinfo($scope.temprep, index);
            } else {
                $scope.reports.splice(index, 1);
            }
        };
    });

//////////////////////////////////////GLOBAL////////////////////////////////////////////    
    $scope.savedata = function (insert, index) {
        var conAjax = $http.post(settingURL, insert);
        conAjax.success(function (response) {
            $scope.envDropdown = angular.copy($scope.environments);
            $('#savesuccess').modal();
            if (typeof index !== "undefined") {
                $scope.reports[index]._id = {$oid: response};
            }
        });
        conAjax.error(function (response) {
            $scope.envDropdown = angular.copy($scope.environments);
            $('#savefail').modal();
        });
    };

    $scope.delinfo = function (insert, remove) {
        var conAjax = $http.delete(settingURL, {data: insert});
        ;
        conAjax.success(function (response) {
            $scope.reports.splice(remove, 1);
            $('#deletesuccess').modal();
        });
        conAjax.error(function (response) {
            $('#deletefail').modal();
        });
    };


    $scope.startscheduler = function () {
        var conAjax = $http.post(schedulerURL+"?server=start", {msg:'start'});
        conAjax.success(function (response) {
            
        });
        conAjax.error(function (response) {
           
        });
    };
    
    $scope.stopscheduler = function () {
        var conAjax = $http.post(schedulerURL+"?server=stop", {msg:'stop'});
        conAjax.success(function (response) {
            
        });
        conAjax.error(function (response) {
           
        });
    };
});