define(function (require, exports, module) {
    require('css!./index.css');

    function guid() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            var r = Math.random() * 16 | 0,
                v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    function init(rootElementId, params) {
        var tpl = require("text!./index.vue");

        var vueInstance = null;

        A.loadElementUI(function (e) {

            $("#" + rootElementId).html(tpl);

            vueInstance = new Vue({
                el: '#' + rootElementId,
                data: function () {
                    var mergeParams = {
                        key: guid(),
                        progress: undefined,
                        user: undefined,
                        headerOperation: [],
                        list: []
                    };
                    var cloneParams = _.cloneDeep(params);
                    var handleParams = {
                        rootElementId: rootElementId
                    };
                    return Object.assign({}, mergeParams, cloneParams, handleParams);
                },
                computed: {},
                methods: {
                    handleStatusStyle: function (status, originClassName) {
                        var statusStyle = '';
                        switch (status) {
                            case '0':
                                statusStyle = 'NotStart';
                                break;
                            case '1':
                                statusStyle = 'InProgress';
                                break;
                            case '2':
                                statusStyle = 'Finished';
                                break;
                            default:
                                break;
                        }
                        return (originClassName || '') + statusStyle;
                    },
                    handleStatusName: function (status) {
                        switch (status) {
                            case '0':
                                return '未开始';
                            case '1':
                                return '进行中';
                            case '2':
                                return '已完成';
                            default:
                                return '';
                        }
                    },
                    handleExamTitle: function (examItem) {
                        if (params && params.titleFormatter && typeof params.titleFormatter === 'function') {
                            return params.titleFormatter(examItem);
                        }
                        return '';
                    },
                    getItemStatus: function (item) {
                        if (params && params.statusFormatter && typeof params.statusFormatter === 'function') {
                            return params.statusFormatter(item);
                        }
                        return '0';
                    },
                    getItemShowsInfo: function (item) {
                        if (params && params.detailsFormatter && typeof params.detailsFormatter ==='function') {
                            return params.detailsFormatter(item) || [];
                        }
                        return [];
                    },
                    getOperations: function (item) {
                        if (params && params.operationsFormatter && typeof params.operationsFormatter === 'function') {
                            return params.operationsFormatter(item);
                        }
                        return [];
                    }
                },
                mounted: function () {
                }
            });
        });

        return vueInstance;
    }

    return init;
});