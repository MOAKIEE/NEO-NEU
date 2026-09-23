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
                        courseList: []
                    };
                    var cloneParams = _.cloneDeep(params);
                    var handleParams = {
                        rootElementId: rootElementId
                    };
                    return Object.assign({}, mergeParams, cloneParams, handleParams);
                },
                components: {},
                methods: {},
                mounted: function () {
                }
            });
        });

        return vueInstance;
    }

    return init;
});