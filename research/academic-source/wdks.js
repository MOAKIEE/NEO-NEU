define(function (require) {
    var utils = require('utils');

    var ksap = require('./ksap/ksap');
    var ksbm = require('./ksbm/ksbm');
    CACHE_DQXNXQ = {};
    var self = null;

    var viewConfig = {
        initialize: function (param) {
            this.pushSubView([ksap, ksbm]);
            self = this;
            self.init();
        },

        init: function () {
            var indexView = utils.loadCompiledPage('index', require);
            self.$rootElement.html(indexView.render(), true);
            $('.ksxnxq').jwXnxqHead({
                csdm: 'SYS',
                zcsdm: 'DQXNXQDM',
                title: '更改学年学期',
                isNeedSave: true,
                initCallback: function (data) {
                    CACHE_DQXNXQ = data;
                    self.initTabs(data);
                },
                changeCallback: function (data) {
                    CACHE_DQXNXQ = data;
                    self.refresh(data);
                }
            });

        },
        refresh: function () {
            ksap.refresh();
            ksbm.refresh();
        },
        initTabs: function (data) {
            self.$rootElement.find('.tab-container').jwAuthTab({
                tabs: [{
                    title: '考试安排',
                    needAuth: true,
                    authId: 'wdkw-wdksap',
                    callback: function ($element) {
                        ksap.initialize($element);
                    }
                }, {
                    title: '补考考试报名',
                    needAuth: true,
                    authId: 'wdkw-ksbm',
                    callback: function ($element) {
                        ksbm.initialize($element);
                    }
                }]
            });
        }
    };
    return viewConfig;
});