/**
 * 渲染下拉
 * @param tagDom
 * @param tagList
 */
function updateDropdownList(tagDom, tagList) {
    tagDom.off('open');
    tagDom.jqxDropDownList("clear");
    tagDom.jqxDropDownList({
        source: tagList
    });
}


/**
 * 初始化下拉
 */
function initializationDropdown(tagDom, tagList) {
    if (tagDom.length > 0) {
        tagDom.off('open');
        tagDom.on('open');
        tagDom.off('close');
        tagDom.jqxDropDownList("clear");
        tagDom.jqxDropDownList({
            source: tagList,
            filterable: true,
            filterPlaceHolder: '搜索参数名称',
            placeHolder: '请选择',
            searchMode: 'contains'
        });
    }
}


function pubConfirm(content, callBack, param) {
    BH_UTILS.bhDialogWarning({
        title: '提示',
        content: content,
        buttons: [
            {
                text: '确认',
                callback: function () {
                    callBack(param);
                }
            },
            {
                text: '取消',
                callback: function () {
                }
            }
        ]
    });
}

//渲染空页面
function setKongPage(tagDom, msg) {
    var template = '<div class="notchoose-section">' +
        '<div class="bh-text-center close_img">' +
        '<img src="../../pjglapp/public/images/cry.png">' +
        '</div>' +
        '<div class="bh-text-center">' +
        '<div class="bh-mt-24 bh-mb-8" style="font-size: 18px;">' + (msg || '未查询到数据') + '</div>' +
        '<div class="open_time"></div>' +
        '</div>' +
        '</div>';
    tagDom.html(template);
}


//渲染弹窗
function getBhWindowHeight() {
    return (window.innerHeight > 850 ? 850 : window.innerHeight) + 'px';
}

function getPageModules(page) {
    var url = WIS_CONFIG.ROOT_PATH + "/sys/funauthapp/api/getAppConfig/" + WIS_CONFIG.APPNAME + "-" + WIS_CONFIG.APPID + ".do";
    var data = BH_UTILS.doSyncAjax(url);
    var pages = data.MODULES;
    for (var i = 0; i < pages.length; i++) {
        if (pages[i].route == page) {
            return pages[i].buttons;
        }
    }
    return [];
}

function get_random_str(number) {
    x = 'AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz';
    var str = '';
    for (var i = 0; i < number; i++) {
        //重点  这里利用了Math.random()函数生成的随机数大于0 小于1 我们可以
        //用它的随机数来乘以字符串的长度,得到的也是一个随机值，再通过parseInt()
        //函数取整，这样就可以实现字符串的随机取值了
        str += x[parseInt(Math.random() * x.length)];
    }
    return str;
}

function isShuzi(number) {
    return !isNaN(parseFloat(number)) && isFinite(number);
}


function initJdtByKey(key, shuaxinFunc) {
    $.jwInitProgressIndicator({
        title: '进度',
        zxjdKey: key,
        closeCallback: function () {
            shuaxinFunc();
        }
    });
}

function actionRefreshDictionary() {
    var appName = window.WIS_CONFIG.APPNAME;
    BH_UTILS.doSyncAjax(WIS_EMAP_SERV.getContextPath() + "/sys/emapcomponent/clearAppDicCache.do?app=" + appName);
}


/**
 * 考试批次联动
 */
function initLink(parentDom) {
    $(parentDom).find('[data-name="XNXQDM"]').on('close', function (event) {
        var xnxqdm = $(parentDom).find("[data-name='XNXQDM']").val();
        updateDropdownList($(parentDom).find("[data-name='LY_KEY']"), getKsdm(xnxqdm));
    });
}

function getKsdm(xnxqdm) {
    var res = $.jwAjax({
        url: "/modules/qxcjcx/cxbkpcxx.do",
        data: {XNXQDM: xnxqdm},
        successMsg: "",
        async: false
    });
    var result = [];
    res.rows.map(function (obj, index) {
        result.push({
            value: obj.KSDM, label: obj.KSMC
        });
    });
    return result;
}

function commonRequest(api, params, onSuccess, onFail) {
    $.jwAjax({
        url: api,
        data: params,
        successMsg: "",
        success: function (resp) {
            if (typeof onSuccess === 'function') {
                onSuccess(resp);
            }
        },
        error: function (error) {
            if (typeof onFail === 'function') {
                onFail(error);
            }
        }
    });
}