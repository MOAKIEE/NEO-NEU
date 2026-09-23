/**
 * Created by jwu on 2023/11/17
 * E-mail:01313101@wisedu.com
 *
 * Description:"我的指标"组件
 */

define(function (require, exports, module) {
    function init(rootElementId, params) {
        require('css!./index.css');
        var tpl = require("text!./index.vue");
        $("#" + rootElementId).html(tpl);

        return new Vue({
            el: '#' + rootElementId,
            data: function () {
                return {
                    data: params.data
                };
            }
        });
    }

    return init;
});