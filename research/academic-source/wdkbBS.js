define(function (require) {
    function request(api, params, onSuccessCallback, onFailureCallback, async) {
        async ? (async = true) : (async = false);
        $.jwAjax({
            url: api,
            data: params,
            successMsg: "",
            async: async,
            success: function (res) {
                if (onSuccessCallback && typeof onSuccessCallback === "function") {
                    onSuccessCallback(res);
                }
            },
            error: function () {
                if (onFailureCallback && typeof onFailureCallback === "function") {
                    onFailureCallback();
                }
            }
        });
    }

    var api = {
        pageModel: '/modules/qxkbcx.do',
        advancedQueryModel: '/modules/qxkbcx.do',
        getScheduleTypeListUrl: '/api/qxkbcx/getScheduleTypeList.do',
        cxdqxnxq: '/modules/qxkbcx/cxdqxnxq.do',
        cxpxbxx: '/modules/qxkbcx/cxpxbxx.do',
        scheduleConfirmationSetting: '/api/wdkbcx/scheduleConfirmationSetting.do',
        confirmSchedule: "/api/wdkbcx/confirmSchedule.do",
        getMyScheduledCampus: "/sys/kbapp/api/wdkbcx/getMyScheduledCampus.do"
    };

    var bs = {
        api: api,
        request: {
            getMyScheduledCampus: function (params, onSuccessCallback, onFailureCallback, async) {
                request(api.getMyScheduledCampus, params, onSuccessCallback, onFailureCallback, async);
            }
        }
    };
    return bs;
});

