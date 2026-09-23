function getDqXnxq() {
    var jcRes = $.jwAjax({
        url: context_path + '/sys/jwpubapp/modules/gg/cxmrxnxq.do',
        data: {
            CSDM: 'SYS',
            ZCSDM: 'DQXNXQDM',
            SFSY: '1'
        },
        successMsg: "",
        async: false
    });
    if (!jcRes || !jcRes.rows || jcRes.rows.length < 1) {
        $.bhTip({
            content: '未设置当前学年学期。',
            state: 'warning'
        });
        throw SyntaxError("未设置当前学年学期");
    }
    return {
        XNXQDM: jcRes.rows[0].XNXQDM,
        XNXQMC: jcRes.rows[0].XNXQMC
    };
}

/**
 * 获取学年学期列表
 */
function getXnxqList() {
    return $.jwAjax({
        url: context_path + '/sys/jwpubapp/modules/zdgl/xnxqcx.do',
        data: {
            "*order": "+DM"
        },
        successMsg: "",
        async: false
    });
}

/**
 * 获取校区
 * @returns {null}
 */
function getXxxq(pagePath, params) {
    var url = context_path + (pagePath.indexOf("qxkbcx") >= 0 ? "/sys/kbapp/api/qxkbcx/getMyScheduledCampus.do" : "/sys/kbbpapp/api/kbck/getMyScheduledCampus.do");

    return $.jwAjax({
        url: url,
        data: params,
        successMsg: '',
        async: false
    });
}

/**
 * 获取星期
 * @returns {null}
 */
function getWeek() {
    return $.jwAjax({
        url: '/sys/kbbpapp/api/kbbp/cxxqzd.do',
        async: false,
        successMsg: ""
    });
}

/**
 * 获取上课周次
 * @param term
 * @returns {null}
 */
function getStudyWeek(param) {
    return $.jwAjax({
        url: '/sys/kbbpapp/api/schoolCalendar/getTermWeeks.do',
        async: false,
        data: param,
        successMsg: ""
    });
}

/**
 * 获取节次
 * @param params
 * @returns {null}
 */
function getSection(params) {
    return $.jwAjax({
        url: '/sys/kbbpapp/api/kbck/getSectionList.do',
        async: false,
        successMsg: "",
        data: params
    });
}

function pxbWin(multiCourse, path) {
    BH_UTILS.bhWindow("<div id='pxb-win' style='height:100%;'></div>", "列表",
        [{
            text: '关闭', className: 'bh-btn-default', callback: function () {
            }
        }],
        {
            height: 800,
            width: 1000
        }
    );
    $('#pxb-win').jwQueryAndTable({
        // pagePath: '/modules/qxkbcx.do',
        pagePath: path,
        action: 'cxpxbxx',
        hasSearch: false,
        hasZdyl: false,
        hasCheckbox: false,
        emapTable: {
            minLineNum: 10,
            height: 'auto',
            pageable: false
        },
        params: {
            querySetting: JSON.stringify([{
                "name": "JXBID",
                "value": "," + multiCourse + ",",
                "linkOpt": "and",
                "builder": "m_value_equal"
            }])
        }
    });
}

/**
 * 课表信息导出
 */
function scheduleInfoExport(exportParams) {
    var genExportFileUrl = "/sys/kbapp/api/kbExport/genExportFile.do";
    if (exportParams.genExportFileUrl) {
        genExportFileUrl = exportParams.genExportFileUrl;
    }
    var resp = $.jwAjax({
        showLoading: true,
        url: context_path + genExportFileUrl,
        async: false,
        data: exportParams,
        successMsg: "",
        success: function (res) {
        }
    });
    if (resp.code) {
        return;
    }
    var downloadExportFileUrl = "/sys/kbapp/api/kbExport/downloadExportFile.do";
    if (exportParams.downloadExportFileUrl) {
        downloadExportFileUrl = exportParams.downloadExportFileUrl;
    }
    var excelFormOptions = $.extend({}, exportParams, {key: resp});
    $.excelForm(excelFormOptions, contextPath + downloadExportFileUrl, 'scheduleWord');
}


function refreshTreeNode(values, disabledRefreshRight) {
    if (window.kbbp_page && window.kbbp_page.instance && values && values.selectedTreeItem && Array.isArray(values.selectedTreeItem.NODE_LIST)) {
        console.log('values.selectedTreeItem.NODE_LIST', values.selectedTreeItem.NODE_LIST);
        window.kbbp_page.instance.leftTreeSelectNewTreeItem(
            values.selectedTreeItem.NODE_LIST.map(function (item) {
                return function (node) {
                    return node.type === item.type && node.code === item.code;
                };
            }),
            function () {
            },
            disabledRefreshRight === true
        );
    }
}