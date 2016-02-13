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
settingModule.directive('lowercased', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, modelCtrl) {
            modelCtrl.$parsers.push(function (input) {
                return input ? input.toLowerCase() : "";
            });
            element.css("text-transform", "lowercase");
        }
    };
});
settingModule.directive('confirmationNeeded', function () {
    return {
        priority: 1,
        terminal: true,
        link: function (scope, element, attr) {
            var msg = attr.confirmationNeeded || "Are you sure?";
            var clickAction = attr.ngClick;
            element.bind('click', function () {
                if (window.confirm(msg)) {
                    scope.$eval(clickAction);
                }
            });
        }
    };
});


settingModule.controller('SettingsController', ['$scope', '$http', 'localStorageService', 'resetTimerService', 'logoutService',
    function ($scope, $http, localStorageService, resetTimerService, logoutService) {
        var settingURL = TLS_PROTOCOL + "://" + TLS_SERVER + ":" + TLS_PORT + "/_logic/SettingService";
        var schedulerURL = TLS_PROTOCOL + "://" + TLS_SERVER + ":" + TLS_PORT + "/_logic/SchedulerService";
        var batchURL = TLS_PROTOCOL + "://" + TLS_SERVER + ":" + TLS_PORT + "/_logic/BatchReplayService";
        $scope.settings = {};
        $scope.schedulerstatus = 0;
        $scope.reports = {};
        $scope.errormsg = '';
        $scope.temprep = {};
        $scope.curPage = 0;
        $scope.pageSize = 50;
        $scope.curPageImmi = 0;
        $scope.units = ['sec', 'min', 'hrs', 'day'];
        $scope.numbers = ['3', '5', '10', '15', '20', '30', '40', '50', '100', '200'];
        $scope.selectedNumber = $scope.numbers[7];
        $scope.selectedNumberAggri = $scope.numbers[4];
        $scope.startserviceImmediate = '';
        $scope.startserviceBatch = '';
        $scope.curPageAggri = 0;
        $scope.pageSizeAggri = 4;
        $scope.immidatejob = {"requestType": "", "jobName": "ImmediateNotificationRefreshJob", "jobClass": "ImmediateNotificationRefreshJob", "frequency": {"duration": "", "unit": "", "starttime": ""}};
//////////////////////////////////////SETTINGS//////////////////////////////////////////
        $scope.settingPromise = function () {
            var promise = $http.get(settingURL + "?object=setting").success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
            })
                    .error(function (data, status, header, config) {
                        if (status == 401) {
                            logoutService.logout();
                        }
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
                $scope.settings.setting.notification = {immediate: {frequency: {duration: '', unit: ''}, notification: [{severity: '', email: '', application: {name: '', interfaces: ['']}}]}};
                $scope.notifications = $scope.settings.setting.notification;
            } else {
                $scope.notifications = $scope.settings.setting.notification;
            }

        });
        $scope.settingPromise().catch(function () {
            $scope.newsettingcreator = 1;
            newsetting = {setting: {apisetup: {hostname: '', port: '', database: '', collections: {payload: '', audits: ''}}, notification: {immediate: {frequency: {duration: '1', unit: 'hrs'}, jobRefreshRate: {duration: '1', unit: 'hrs'}, notification: [{envid: '', severity: '', email: '', template: 'ImmediateNotification.html', application: {name: '', interfaces: ['']}}]}}, envsetup: [{name: '', description: '', label: ''}]}};
            $scope.settings = newsetting;
            $scope.environments = $scope.settings.setting.envsetup;
            $scope.notifications = $scope.settings.setting.notification;
        });
        $scope.settingPromise().finally(function () {
            $scope.addNewImmidate = function () {
                newson = {envid: '', severity: '', email: '', template: 'ImmediateNotification.html', application: {name: '', interfaces: ['']}};
                $scope.notifications.immediate.notification.push(newson);
            };
            $scope.addImmidateInterface = function (upindex) {
                if ($scope.curPageImmi >= 1) {
                    temp = ($scope.curPageImmi * $scope.pageSizeImmi) + upindex;
                    $scope.notifications.immediate.notification[temp].application.interfaces.push({});
                } else {
                    $scope.notifications.immediate.notification[upindex].application.interfaces.push('');
                }
            };
            $scope.removeImmidateInterface = function (upindex, index) {
                if ($scope.curPageImmi >= 1) {
                    temp = ($scope.curPageImmi * $scope.pageSizeImmi) + upindex;
                    $scope.notifications.immediate.notification[temp].application.interfaces.splice(index, 1);
                } else {
                    $scope.notifications.immediate.notification[upindex].application.interfaces.splice(index, 1);
                }
            };
            $scope.removeImmidate = function (index) {
                $scope.notifications.immediate.notification.splice(index, 1);
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
            $scope.savesetting = function (reloadFlag) {
                $scope.temp = $scope.settings;
                $scope.savedata($scope.temp);
                $scope.reloadPage = true;
                if (reloadFlag === 'reload') {
                    $scope.reloadPage = true;
                }
            };
            $scope.numberOfPagesImmi = function () {
                $scope.pageSizeImmi = $scope.selectedNumber;
                return Math.ceil($scope.notifications.immediate.notification.length / $scope.pageSizeImmi);
            };
            $scope.inmidateStartjob = function () {
                $scope.immidatejob.requestType = 'startJob';
                $scope.immidatejob.frequency.duration = $scope.notifications.immediate.jobRefreshRate.duration;
                $scope.immidatejob.frequency.unit = $scope.notifications.immediate.jobRefreshRate.unit;
                $scope.scheduler($scope.immidatejob);
                $scope.startserviceImmediate = 'started';
                fromservice = 1;
                $scope.savesetting();
            };
            $scope.inmidateStopjob = function () {
                $scope.immidatejob.requestType = 'stopJob';
                temporal = angular.copy($scope.immidatejob);
                delete temporal.frequency;
                $scope.scheduler(temporal);
                $scope.startserviceImmediate = 'stopped';
            };
            //Env dropdown
            $scope.envDropdown = angular.copy($scope.environments);
            $scope.$watch('envDropdown', function () {
                localStorageService.cookie.add('envOptions', $scope.envDropdown);
            });
            $scope.reload = function () {
                console.log("reload");
                location.reload();
            };
        });
//////////////////////////////////////REPORT////////////////////////////////////////////    
        $scope.reportPromise = function () {
            var reportpromise = $http.get(settingURL + "?object=report").success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
            })
                    .error(function (data, status, header, config) {
                        if (status === 401) {
                            logoutService.logout();
                        }
                    });
            return reportpromise;
        };
        $scope.reportPromise().then(function (data) {
            $scope.reports = data.data._embedded['rh:doc'];
        });
        $scope.reportPromise().catch(function () {
            $scope.newreport = 1;
            $scope.reports = [{report: {envid: '', application: '', interface1: '', errorType: '', frequency: {starttime: '', duration: '', unit: ''}, email: '', template: 'ReportNotification.html'}}];
        });
        $scope.reportPromise().finally(function () {
            //Env dropdown
            $scope.envDropdown = angular.copy($scope.environments);

            $scope.addNewAggrigated = function () {
                newson = {report: {envid: null, application: null, email: null, template: 'ReportNotification.html', interface1: null, errorType: null, frequency: {duration: null, starttime: null, unit: null}}};
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
                        object.email === undefined || object.email === '') {
                    alertify.error("Application, Duration, Unit and Email are required, please review the information and try again");
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
//////////////////////////////////////SCHEDULE INFO///////////////////////////////////// 
        $scope.schedulerJob = function () {
            var getjobs = {"requestType": "getAllJobs"};
            var schedulerjob = $http.post(schedulerURL, getjobs).success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.schedulers = data;
                //find BatchReplayJob in the array
                Batchlenght = null;
                Tlength = $scope.schedulers.length;
                for (i = 0; i < Tlength; i++) {
                    if ($scope.schedulers[i].jobKey === "BatchReplayJob") {
                        Batchlenght = $scope.schedulers[i].frequency;
                        $scope.startserviceBatch = 'started';
                    }
                }

                if (Batchlenght !== null) {
                    if (Batchlenght <= 59) {
                        $scope.batch.frequency.duration = Batchlenght;
                        $scope.batch.frequency.unit = $scope.units[0];
                    }
                    if ((Batchlenght >= 60) && (Batchlenght <= 3599)) {
                        minutes = Math.floor(Batchlenght / 60) % 60;
                        $scope.batch.frequency.duration = minutes;
                        $scope.batch.frequency.unit = $scope.units[1];
                    }
                    if ((Batchlenght >= 3600) && (Batchlenght <= 86399)) {
                        hours = Math.floor(Batchlenght / 3600) % 24;
                        $scope.batch.frequency.duration = hours;
                        $scope.batch.frequency.unit = $scope.units[2];
                    }
                    if (Batchlenght >= 86400) {
                        days = Math.floor(Batchlenght / 86400);
                        days * 86400;
                        $scope.batch.frequency.duration = days;
                        $scope.batch.frequency.unit = $scope.units[3];
                    }
                }
                $scope.starter = {};
                $scope.resumeJob = function (index) {
                    status = 'resume';
                    temporalkey = $scope.schedulers[index].jobKey;
                    $scope.starter = {"requestType": "resumeJob", "jobKey": $scope.schedulers[index].jobKey};
                    $scope.scheduler($scope.starter, status);
                };
                $scope.pauseJob = function (index) {
                    status = 'suspend';
                    temporalkey = $scope.schedulers[index].jobKey;
                    $scope.starter = {"requestType": "suspendJob", "jobKey": $scope.schedulers[index].jobKey};
                    $scope.scheduler($scope.starter, status);
                };
            })
                    .error(function (data, status, header, config) {
                        if (status === 401) {
                            logoutService.logout();
                        }
                    });
            return schedulerjob;
        };
//////////////////////////////////////SCHEDULE STATUS //////////////////////////////////
        $scope.schedulerStatus = function () {
            var getstatus = {"requestType": "getSchedulerStatus"};
            var schedulerstatus = $http.post(schedulerURL, getstatus).success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
            });
            return schedulerstatus;
        };
        $scope.schedulerStatus().then(function (data) {
            $scope.SchedulerStatus = data.data;
            if ($scope.SchedulerStatus === 'started') {
                $scope.schedulerJob();
                $scope.startserviceImmediate = 'stopped';
                $scope.startserviceBatch = 'stopped';
            }
        });
/////////////////////////////////////BATCH JOBS////////////////////////////////////////
        $scope.BatchjobPromise = function () {
            var batchjobpromise = $http.get(batchURL).success(function (data, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.Batchjobs = data;
                $scope.selectedNumberBatchJobs = $scope.numbers[4];
                $scope.curPageBatchjob = 0;
                $scope.pageSizeBatchjob = 4;
                $scope.selectedBatch = [];
                $scope.selectedBatchDeleter = [];
            });
            return batchjobpromise;
        };
        $scope.BatchjobPromise().then(function (data) {
            $scope.batchchooser = function (index) {
                $scope.sendBatchJob = {};
                if ($scope.curPageBatchjob >= 1) {
                    temp = ($scope.curPageBatchjob * $scope.pageSizeBatchjob) + index;
                    batchid = $scope.Batchjobs[temp]._id.$oid;
                    $scope.batchupdater(batchid);
                } else {
                    $scope.sendBatchJob.status = $scope.Batchjobs[index].status;
                    batchid = $scope.Batchjobs[index]._id.$oid;
                    $scope.batchupdater(batchid);
                }
            };
            $scope.numberOfPagesBatch = function () {
                $scope.pageSizeBatchjob = $scope.selectedNumberBatchJobs;
                return Math.ceil($scope.Batchjobs.length / $scope.pageSizeBatchjob);
            };
            $scope.DeleteSelectedBatch = function () {
                $scope.todelete=[];
                for (x in $scope.selectedBatch){
                    if ($scope.selectedBatch[x] === true){
                        $scope.todelete[x]=true ;
                    }
                };
                temporal = Object.keys($scope.todelete);
                $scope.batchupdelete(temporal);
            };
            $scope.checkAll = function (source) {
                $('.batchcheckdata, .batchcheck').click();
                

            };
        });

//////////////////////////////////////GLOBAL//////////////////////////////////////////// 
        $scope.batch = {requestType: '', jobName: "BatchReplayJob", jobClass: "BatchReplayJob", frequency: {starttime: '', duration: '', unit: ''}};
        $scope.schedulerObj = {requestType: '', propertiesFile: ''};
        $scope.savedata = function (insert, index) {
            var conAjax = $http.post(settingURL, insert);
            conAjax.success(function (response, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.envDropdown = angular.copy($scope.environments);
                alertify.success("Information was successfully saved");
                if (typeof index !== "undefined") {
                    $scope.reports[index]._id = {$oid: response};
                }
            });
            conAjax.error(function (response) {
                $scope.envDropdown = angular.copy($scope.environments);
                alertify.error("Error while saving the information, please try again");
            });
        };
        $scope.delinfo = function (insert, remove) {
            var conAjax = $http.delete(settingURL, {data: insert});
            conAjax.success(function (response) {
                $scope.reports.splice(remove, 1);
                alertify.success("Information has been deleted correctly");
            });
            conAjax.error(function (response) {
                alertify.error("Information was not deleted, please try again");
            });
        };
        $scope.batchstart = function () {
            $scope.batch.requestType = "startJob";
            if ($scope.batch.frequency.starttime) {
                $scope.batch.frequency.starttime = $scope.batch.frequency.starttime.replace(/ /g, "T");
            } else {
                $scope.batch.frequency.starttime = "";
            }
            $scope.scheduler($scope.batch, 1);
            $scope.startserviceBatch = 'started';
        };
        $scope.batchstop = function () {
            $scope.batch.requestType = "stopJob";
            delete $scope.batch.frequency;
            $scope.batchScheduler.batchFrequency.$touched = false;
            $scope.batchScheduler.batchFrequency.$invalid = false;
            $scope.batchScheduler.batchUnit.$touched = false;
            $scope.batchScheduler.batchUnit.$invalid = false;
            $scope.scheduler($scope.batch, 2);
            $scope.startserviceBatch = 'stopped';
        };
        $scope.scheduler = function (object, opt) {
            var conAjax = $http.post(schedulerURL, object);
            conAjax.success(function (response, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.schedulerstatus = opt;
                if (opt == "1") {
                    $scope.schedulerJob();
                }
                if (opt == "2") {
                    $scope.schedulerJob();
                }
                if (opt == 'resume' || opt == 'suspend') {
                    $scope.schedulerJob();
                }
                if (opt == "4") {
                    $scope.SchedulerStatus = "stopped";
                    $('.SchedulerJob').remove();
                }
                if (opt == "3") {
                    $scope.SchedulerStatus = "started";
                }
            });
            conAjax.error(function (response) {
                $scope.schedulerstatus = 0;
                $scope.errormsg = response.message;
                alertify.error("Scheduler Error");
            });
        };
        $scope.startscheduler = function () {
            $scope.schedulerObj.requestType = "startScheduler";
            $scope.startserviceImmediate = 'stopped';
            $scope.startserviceBatch = 'stopped';
            $scope.scheduler($scope.schedulerObj, 3);
        };
        $scope.stopscheduler = function () {
            $scope.schedulerObj.requestType = "stopScheduler";
            $scope.startserviceImmediate = 'stopped';
            $scope.startserviceBatch = 'stopped';
            $scope.scheduler($scope.schedulerObj, 4);
        };
        $scope.batchupdater = function (insert) {
            console.log(insert);
            send = batchURL + '/?id=' + insert;
            var conAjax = $http.put(send);
            conAjax.success(function (response, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                $scope.BatchjobPromise();
            });
            conAjax.error(function (response) {
                alertify.error("Batch Scheduler Error");
            });
        };
        $scope.batchupdelete = function (insert) {
            var conAjax = $http.delete(batchURL, {"data": insert});
            conAjax.success(function (response, status, header, config) {
                var auth_token_valid_until = header()['auth-token-valid-until'];
                resetTimerService.set(auth_token_valid_until);
                alertify.success("Information has been deleted correctly");
                $scope.BatchjobPromise();
            });
            conAjax.error(function (response) {
                alertify.error("Batch Delete Error");
            });
        };
    }]);
