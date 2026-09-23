define(function (require) {
    var utils = require("utils");
    var bs = require("./wdkbBS");
    var kb = require("../../../../kbbpapp/public/components/kb/index");
    var _xnxqList = [];
    var xnxqdm = "";
    var self = null;
    /** ******子页面******* */
    var viewConfig = {
        /** ******页面内事件通过eventMap统一管理******* */
        eventMap: function () {
            return {
                '[data-action="confirmationSchedule"]': this.confirmationSchedule
            };
        },

        /** ******页面入口方法******* */
        initialize: function () {
            /** ******注册子页面******* */
            self = this;
            var indexView = utils.loadCompiledPage("wdkbIndexPage", require);

            this.$rootElement.html(indexView.render({}), true);
            var _curXq = getDqXnxq().XNXQDM;
            _xnxqList = getXnxqList();
            var _weekArr = getWeek();
            var _studyWeek = getStudyWeek({XNXQDM: _curXq});
            // 系统参数：“课表打印是否按节次平分固定行高”
            var _KBDYSFAJCPFGDHG = $.jwGetXtcs("KBBP", "KBDYSFAJCPFGDHG", function () {
            }, true)[0];

            kb("wdkb-kb", {
                printOption: {word: {permissionKey: "wdkb-kbdy-word"}, excel: {permissionKey: "wdkb-kbdy-excel"}},
                XNXQDatasource: _xnxqList.rows,
                currentXNXQ: _curXq,
                ZCDatasource: _studyWeek,
                weekdaysDatasource: _weekArr,
                fixedCellHeight: _KBDYSFAJCPFGDHG.CSZA === "1",
                onShowMultiCourse: function (multiCourse) {
                    pxbWin(multiCourse, "/modules/wdkb.do");
                },
                getXQ: function (XNXQDM, onSuccess) {
                    bs.request.getMyScheduledCampus({XNXQDM: XNXQDM}, function (data) {
                        onSuccess(data);
                    }, null, false);
                },
                getZC: function (params, onSuccess) {
                    onSuccess(getStudyWeek(params));
                },
                getSection: function (params, onSuccess) {
                    var _sectionList = $.jwAjax({
                        url: "/api/wdkbcx/getMySectionList.do",
                        async: false,
                        successMsg: "",
                        data: params
                    });
                    onSuccess(_sectionList);
                },
                getTimetable: function (params, onSuccess) {
                    $.jwAjax({
                        url: "/api/wdkbcx/getMyScheduleDetail.do",
                        async: false,
                        successMsg: "",
                        data: params,
                        success: function (res) {
                            onSuccess(res);
                        }
                    });
                },
                confirmationScheduleCustom: function (term, callback) {
                    xnxqdm = term;
                    $.jwAjax({
                        url: bs.api.scheduleConfirmationSetting,
                        data: {
                            XNXQDM: term
                        },
                        successMsg: "",
                        success: function (resp) {
                            callback({
                                time: resp.confirmationTime ? '<span class="bh-mh-8">课表确认截止时间：' + resp.confirmationTime + "</span>" : "",
                                button: resp.openSwitch
                                    ? '<a href="javascript:void(0);" data-action="' +
                                    (resp.canConfirm ? "confirmationSchedule" : "") +
                                    '" class="el-button el-button--mini el-button--primary ' +
                                    (resp.canConfirm ? "" : "is-disabled") +
                                    '">课表确认</a>'
                                    : ""
                            });
                        }
                    });
                },
                wordPrint: function (params) {
                    self.printSchedule(params, "01");
                },
                excelPrint: function (params) {
                    self.printSchedule(params, "02");
                },
                pdfPrint: function (params) {
                    self.printSchedule(params, "03");
                }
            });
        },
        confirmationSchedule: function () {
            var name = _.find(_xnxqList.rows, function (term) {
                return term.DM === xnxqdm;
            }).MC;
            utils.window({
                title: "课表确认",
                content: "<div id='wdkb-kbqr'>确认对" + name + "个人课表进行确认？</div>",
                height: "180px",
                width: "350px",
                buttons: [
                    {
                        text: "确认",
                        className: "bh-btn-primary ",
                        callback: function () {
                            $.jwAjax({
                                url: bs.api.confirmSchedule,
                                data: {
                                    XNXQDM: xnxqdm
                                },
                                successMsg: "课表确认成功",
                                success: function (resp) {
                                    self.initialize();
                                }
                            });
                        }
                    },
                    {
                        text: "关闭",
                        className: "bh-btn-default ",
                        callback: function () {
                        }
                    }
                ]
            });
        },

        printSchedule: function (params, exportType) {
            var querySetting = [
                {
                    name: "XNXQDM",
                    value: params.XNXQDM,
                    linkOpt: "AND",
                    builder: "equal"
                }
            ];
            scheduleInfoExport({
                genExportFileUrl: "/sys/kbapp/api/wdkbcx/genMyExportFile.do",
                XNXQDM: params.XNXQDM,
                querySetting: JSON.stringify(querySetting),
                EXPORTTYPE: exportType
            });
        }
    };
    return viewConfig;
});
