(function (window) {
    var self;
    var JW_UTILS = {
        _init: function () {
            self = this;

            self.storeTableConfig();
            self.storeUploadFinger();
        },
        /**
         * 将周次数组转为字符串
         * @param {number[]} weekArr 选择的周次数组，如： [1,3,5,7,9,10]
         * @param {number} [maxLength] 最大周次
         * @return 生成的周次字符串 1010101011
         */
        toWeekStr: function (weekArr, maxLength) {
            if (_.isEmpty(weekArr)) {
                console.error("周次数组不得为空");
                return "";
            }
            var max = _.max(weekArr);
            var selectWeek = {};
            _.each(weekArr, function (week) {
                selectWeek[week] = true;
            });
            var str = "";
            for (var i = 1; i <= max; i++) {
                str += selectWeek[i] ? "1" : "0";
            }
            maxLength = maxLength || str.length;
            while (str.length < maxLength) {
                str += "0";
            }
            return str;
        },
        /**
         * 从周次01字符串返回选择的周次
         * @param weekStr 1010101011
         * @return 选择的周次数组 [1,3,5,7,9,10]
         */
        getWeeksFromStr: function (weekStr) {
            var result = [];
            _.each(weekStr, function (c, index) {
                if (c === "1") {
                    result.push(index + 1);
                }
            });
            return result;
        },
        /**
         * 比较两个周次字符串是否相等
         * @param weekStr1
         * @param weekStr2
         * @return {boolean}
         */
        isEqualsWeekStr: function (weekStr1, weekStr2) {
            if (weekStr1 === weekStr2) {
                // 相等，直接返回true
                return true;
            }
            if (!weekStr1 || !weekStr2) {
                // 有一个为空，返回false
                return false;
            }
            var weeks1 = self.getWeeksFromStr(weekStr1);
            var weeks2 = self.getWeeksFromStr(weekStr2);
            if (weeks1.length !== weeks2.length) {
                return false;
            }
            var selectedWeek = {};
            _.each(weeks1, function (week) {
                selectedWeek[week] = true;
            });
            var notContainsWeek = _.filter(weeks2, function (week) {
                return !selectedWeek[week];
            });
            // 不包含的周次为0，视为相等
            return notContainsWeek.length === 0;
        },
        /**
         * 判断第几周是否被选择
         * @param weekStr
         * @param selectWeek
         */
        isWeekSelected: function (weekStr, selectWeek) {
            if (!weekStr || selectWeek < 1) {
                return false;
            }
            return weekStr.charAt(selectWeek - 1) === "1";
        },
        /**
         * 根据给定的周次字符串，生成周次汉字描述
         * 例：
         * 101011111010101010 => 1-3周(单),5-9周,11-17周(单)
         * 101010101010101010 => 1-17周(单)
         * 111111111111111111 => 1-18周
         * @param rawString
         * @return
         */
        genWeeksCnNameText: function (rawString) {
            if (rawString === null || rawString === undefined || rawString === "") {
                return "";
            }
            var sawsb = "";
            var regex;
            var regexMatcher = null;
            var begin = 0;
            var end = 0;
            sawsb += rawString + "00";
            var LENS = sawsb.length;
            var sb = "";
            regex = new RegExp("(1{2," + LENS + "})");
            while (regex.exec(sawsb)) {
                regexMatcher = regex.exec(sawsb);
                begin = regexMatcher.index;
                end = regexMatcher.index + regexMatcher[0].length;
                if (sb.length > 0) sb += ",";
                sb += begin + 1;
                sb += "-" + end + "周";
                for (var i = begin; i < end; i++) {
                    sawsb = replacepos(sawsb, i, i + 1, "0");
                }
            }
            regex = new RegExp("((10){2," + (LENS - 1) + "})");
            var sbArrTmp = [];
            while (regex.exec(sawsb)) {
                regexMatcher = regex.exec(sawsb);
                begin = regexMatcher.index;
                end = regexMatcher.index + regexMatcher[0].length;
                if ((begin + 1) % 2 == 0) {
                    sbArrTmp.push({
                        value: regexMatcher[0],
                        begin: begin,
                        end: end
                    });
                    for (var j = begin; j < end; j++) {
                        sawsb = replacepos(sawsb, j, j + 1, "0");
                    }
                    continue;
                }
                if (sb.length > 0) {
                    sb += ",";
                }
                sb += begin + 1;
                if (end % 2 == 0) {
                    sb += "-" + (end - 1) + "周(单)";
                } else {
                    sb += "-" + end + "周(单)";
                }
                for (var k = begin; k < end; k++) {
                    sawsb = replacepos(sawsb, k, k + 1, "0");
                }
            }
            if (sbArrTmp.length > 0) {
                for (var m = 0, tmpLength1 = sbArrTmp.length; m < tmpLength1; m++) {
                    sawsb = replacepos(sawsb, sbArrTmp[m].begin, sbArrTmp[m].end, sbArrTmp[m].value);
                }
            }
            sbArrTmp = [];
            regex = new RegExp("((01){2," + (LENS - 1) + "})");
            while (regex.exec(sawsb)) {
                regexMatcher = regex.exec(sawsb);
                begin = regexMatcher.index;
                end = regexMatcher.index + regexMatcher[0].length;
                if ((begin + 1) % 2 == 0) {
                    sbArrTmp.push({
                        value: regexMatcher[0],
                        begin: begin,
                        end: end
                    });
                    for (var n = begin; n < end; k++) {
                        sawsb = replacepos(sawsb, n, n + 1, "0");
                    }
                    continue;
                }
                if (sb.length > 0) {
                    sb += ",";
                }
                if (begin % 2 == 0) {
                    sb += begin + 2;
                } else {
                    sb += begin + 3;
                }
                sb += "-" + end + "周(双)";
                for (var x = begin; x < end; x++) {
                    sawsb = replacepos(sawsb, x, x + 1, "0");
                }
            }
            if (sbArrTmp.length > 0) {
                for (var y = 0, tmpLength = sbArrTmp.length; y < tmpLength; y++) {
                    sawsb = replacepos(sawsb, sbArrTmp[y].begin, sbArrTmp[y].end, sbArrTmp[y].value);
                }
            }
            regex = new RegExp("(1+)");
            while (regex.exec(sawsb)) {
                regexMatcher = regex.exec(sawsb);
                begin = regexMatcher.index;
                end = regexMatcher.index + regexMatcher[0].length;
                if (sb.length > 0) sb += ",";
                if (begin + 1 == end) {
                    sb += end + "周";
                } else {
                    sb += begin + 1;
                    sb += "-" + end + "周";
                }
                for (var z = begin; z < end; z++) {
                    sawsb = replacepos(sawsb, z, z + 1, "0");
                }
            }

            var zcarr = sb.toString().split(",");
            if (zcarr.length > 1) {
                zcarr.sort(function (obj1, obj2) {
                    var s1 = obj1.toString().split("-|周")[0];
                    var s2 = obj2.toString().split("-|周")[0];
                    return parseInt(s1) - parseInt(s2);
                });
            }
            return zcarr.join(",");
        },
        /**
         * querySetting字符串的指定项不为空
         * @param querySetting
         * @param itemNames 包含的项
         */
        isQuerySettingHasItems: function (querySetting, itemNames) {
            if (!_.isArray(itemNames)) {
                throw new Error("itemNames必须为数组");
            }
            A.Assert.notEmpty(itemNames, "itemNames不可为空");
            if (_.isString(querySetting)) {
                querySetting = A.toJson(querySetting);
            }
            var noItems = {};
            _.each(itemNames, function (itemName) {
                noItems[itemName] = true;
            });
            _removeItemIfQuerySettingExists(querySetting, noItems);
            return _.isEmpty(noItems);
        },
        /**
         * 获取dom元素绑定的所有事件
         * @param dom
         * @returns {*|{}|jQuery}
         */
        getEventsData: function (dom) {
            var $dom = $(dom);
            return $.data($dom, "events") || $._data($dom[0], "events");
        },
        /**
         * 从querySetting中获取字段值。请慎用，该方法只会返回field的一个值，
         * 如果field有多个值（如日期范围字段可能存在2个值） 只返回第一个找到的值，且该方法传参时必须有值
         * @param querySetting
         * @param name
         */
        getFieldValueFromQuerySetting: function (querySetting, name) {
            A.Assert.notNull(name, "name不得为空");
            if (_.isString(querySetting)) {
                querySetting = A.toJson(querySetting);
            }
            return _getFieldValueFromQuerySetting(querySetting, name);
        },
        /**
         * 创建调用其他应用页面的frame
         * @param appName 要调用的应用名
         * @param hash 功能菜单名
         * @param params 传递的参数
         */
        createAppFrame: function (appName, hash, params) {
            var appUrl = "{contextPath}/sys/{appName}/*default/index.do?min=1&callerAppName={callerAppName}&forceApp={appName}" + "&_yhz=00000{groupId}";
            appUrl = A.formatStr(appUrl, {
                contextPath: _JW_INIT_CONFIG.contextPath,
                appName: appName,
                callerAppName: _JW_INIT_CONFIG.appname,
                groupId: _JW_INIT_CONFIG.ROLEID
            });
            var paramStr = "";
            _.each(params, function (v, k) {
                paramStr += A.formatStr("&{}={}", k, v);
            });
            var url = appUrl + paramStr + "#/" + hash;
            return '<iframe src="' + url + '" style="opacity:1;width:100%;height:800px;" scrolling="no" allowTransparency="true" frameBorder="0"></iframe>';
        },
        /**
         * 获取一个有过期时间的缓存对象
         * 此缓存对象的get方法支持异步获取数据
         * 使用方式 :
         * <li>cache.get(function(cacheValue){}, function (set) {})</li>
         * <li>第一个参数为获取到数据之后的回调</li>
         * <li>第二个参数为数据未获取到时的获取方式，获取到数据后调用set设置缓存数据</li>
         *
         * @param timeout 过期时间，毫秒数，默认30秒
         */
        createCache: function (timeout) {
            return new Cache(timeout);
        },

        JumpType: Object.freeze({
            PAGE: "MENU",
            TAB: "TAB",
            BUTTON: "BUTTON"
        }),
        /**
         * 浏览器打开新tab页
         *
         * @param url url地址
         * @param target
         * @param features
         */
        windowOpen: function (url, target, features) {
            if (!Boolean(url && typeof url === "string")) {
                throw new Error("PageJump.windowOpen url 必须为 string");
            }
            window.open(url, target, features);
        },
        /**
         * 解析url，<br>
         * 原url：http://localhost:8080/emap/sys/jsjy/*default/index.do?_roleId=11438793c6f44-d315-478b-ae5c-96918c9503fb&forceApp=jsjy#/sh<br>
         *
         * 解析后对象<br>
         * {<br>
         *     "appName": "jsjy",<br>
         *     "hash": "sh",<br>
         *     "params": {<br>
         *         "_roleId": "11438793c6f44-d315-478b-ae5c-96918c9503fb",<br>
         *         "forceApp": "jsjy"<br>
         *     }<br>
         * }<br>
         * @param url
         */
        parseUrl: function (url) {
            var SYS_PREFIX = "/sys/";
            var flagEnd = url.indexOf(SYS_PREFIX) + SYS_PREFIX.length;
            var leftPath = url.substring(flagEnd);
            var appEnd = leftPath.indexOf("/");
            var appName = leftPath.substring(0, appEnd);
            var paramStart = url.indexOf("?");
            var hashStart = url.indexOf("#");
            var params = {};
            var hash = "";
            if (paramStart > 0) {
                var paramStr = url.substring(paramStart + 1);
                if (hashStart > 0) {
                    paramStr = paramStr.split("#")[0];
                }
                _.each(paramStr.split("&"), function (part) {
                    var paramArr = part.split("=");
                    params[paramArr[0]] = paramArr[1];
                });
            }
            if (hashStart > 0) {
                hash = url.substring(hashStart + 1).replace("/", "");
            }
            return {
                appName: appName,
                hash: hash,
                params: params
            };
        },
        /**
         * 打开页面，自动兼容新老首页
         * 新首页 - 在首页内打开新tab
         * 老首页 - 浏览器打开tab页面
         *
         * @param appName 目标应用appName
         * @param menuCode 菜单地址
         * @param params 传参
         */
        goTo: function (appName, menuCode, params) {
            if (!Boolean(appName && typeof appName === "string")) {
                throw new Error("PageJump.goTo appName 必须为 string");
            }
            if (!Boolean(menuCode && typeof menuCode === "string")) {
                throw new Error("PageJump.goTo menuCode 必须为 string");
            }
            var hasParams = false;
            if (params !== undefined) {
                // 参数校验
                if (!Boolean(params && typeof params === "object")) {
                    throw new Error("PageJump.goTo params 必须为 object");
                } else {
                    hasParams = Object.keys(params).length;
                }
            }
            // 在新首页内打开
            // 可能出现iframe多层嵌套，这里取最顶层的window，最顶层的window上挂载了HOME_OPEN_PAGE 一定是在新首页内
            if (window.top && window.top.HOME_OPEN_PAGE && typeof window.top.HOME_OPEN_PAGE === "function") {
                // 首页跳转至指定页面
                window.top.HOME_OPEN_PAGE(appName, menuCode, hasParams ? params : {});
            } else {
                // window跳转
                var APP_PATH = _JW_INIT_CONFIG.contextPath + "/sys/" + appName + "/*default/index.do";
                if (hasParams) {
                    APP_PATH = APP_PATH + "?";
                    var paramKeyAndValue = Object.keys(params).map(function (paramKey) {
                        return paramKey + "=" + params[paramKey];
                    });
                    APP_PATH = APP_PATH + paramKeyAndValue.join("&");
                }
                var PAGE_PATH = APP_PATH + "#/" + menuCode;
                this.windowOpen(PAGE_PATH);
            }
        },

        /**
         * 关闭页面，自动兼容新老首页
         * 新首页 - 在首页内打开新tab
         *
         * @param appName 目标应用appName
         * @param menuCode 菜单地址
         */
        closeTab: function (appName, menuCode) {
            if (!Boolean(appName && typeof appName === "string")) {
                throw new Error("PageJump.closeTab appName 必须为 string");
            }
            if (!Boolean(menuCode && typeof menuCode === "string")) {
                throw new Error("PageJump.closeTab menuCode 必须为 string");
            }

            // 在新首页内
            // 可能出现iframe多层嵌套，这里取最顶层的window，最顶层的window上挂载了HOME_CLOSE_PAGE 一定是在新首页内
            if (window.top && window.top.HOME_CLOSE_PAGE && typeof window.top.HOME_CLOSE_PAGE === "function") {
                window.top.HOME_CLOSE_PAGE(appName, menuCode);
            } else {
                console.error("不在新首页内，无法关闭页面");
            }
        },
        getTableConfig: function (key) {
            var _tableConfig = JSON.parse(window.localStorage.getItem("__JWCOMMON_TABLECONFIG"));

            if (_tableConfig && _tableConfig[key]) {
                return _tableConfig[key];
            }
            return {};
        },
        storeTableConfig: function (payload) {
            var _tableConfig = $.jwAjax({
                url: "/sys/jwcommon/tableconfig/queryTableConfig.do",
                data: {},
                successMsg: "",
                async: false
            });

            if (_tableConfig && _tableConfig.code && _tableConfig.code === "0") {
                window.localStorage.setItem("__JWCOMMON_TABLECONFIG", JSON.stringify(_tableConfig.datas));
            }
        },

        getUploadFinger: function (menu) {
            var _fileConfig = JSON.parse(window.localStorage.getItem("__JWCOMMON_FILECONFIG"));
            if (_fileConfig && _fileConfig.CZSC && _fileConfig.CZSC[menu]) {
                return _fileConfig.CZSC[menu];
            }
            return [];
        },
        storeUploadFinger: function ($h2) {
            var match = window.location.href.match(/\/sys\/([^\/?]+)/);
            var appName = "";
            // 检查是否成功匹配
            if (match) {
                appName = match[1];
            }

            var _fileConfig = $.jwAjax({
                url: "/sys/jwcommon/api/appOperatingManual/getMenuOperatingManual/" + appName + ".do",
                data: {},
                successMsg: "",
                async: false
            });
            if (_fileConfig && _fileConfig.code && _fileConfig.code === "0") {
                window.localStorage.setItem("__JWCOMMON_FILECONFIG", JSON.stringify(_fileConfig.datas));
            }
        },
        // 2025.07.07 cpdu 拼接url 满足支出传入 /sys/appname 也就是渲染其他app的表格
        getPageMeta: function (pagePath, params, requestOption) {
            var params = $.extend(
                {
                    "*json": "1"
                },
                params
            );
            var pageMeta = BH_UTILS.doSyncAjax(self.formatterPagePath(pagePath), params, null, requestOption);
            window._EMAP_PageMeta = window._EMAP_PageMeta || {};
            window._EMAP_PageMeta[pagePath] = pageMeta;
            if (typeof pageMeta.loginURL != "undefined") {
                window._EMAP_PageMeta = {};
            }
            return pageMeta;
        },
        formatterPagePath: function (pagePath) {
            if (/^(\/sys\/).*/.test(pagePath)) {
                pagePath = context_path + pagePath;
            } else if (/^(sys\/).*/.test(pagePath)) {
                pagePath = context_path + "/" + pagePath;
            } else if (/^(\/code\/).*/.test(pagePath)) {
                pagePath = context_path + pagePath;
            } else if (/^(code\/).*/.test(pagePath)) {
                pagePath = context_path + "/" + pagePath;
            } else {
                pagePath = WIS_EMAP_SERV.getAbsPath(pagePath);
            }
            return pagePath
        },
        /**
         * 获取模型数据  针对jwQueryAndTable jwForm jwWinForm jwQueryCard
         * @param pagePath string epg
         * @param action string 动作
         * @param type string 类型  form grid search
         * @param params object | undefined 请求参数
         */
        getModel: function (pagePath, action, type, params) {
            

            var cacheKey = pagePath + action + type;

            if (params && $.type(params) === "object") {
                var keys = Object.keys(params);

                keys = keys.sort(); // 将字段排序  保证key的唯一性

                for (var i = 0; i < keys.length; i++) {
                    cacheKey += keys[i] + params[keys[i]];
                }
            }

            var pageMeta = A.getCache($.md5(cacheKey));

            if (!pageMeta) {
                pageMeta = this.getPageMeta(pagePath, params);
                A.putCache($.md5(cacheKey), pageMeta);
            }
            var model;

            if (type == "search") {
                pageMeta = A.getCache($.md5(cacheKey) + "-searchMeta");
                if (!pageMeta) {
                    var url = this.formatterPagePath(pagePath);
                    url = WIS_EMAP_SERV.joinActionIdToDoUrl(url, action);
                    pageMeta = BH_UTILS.doSyncAjax(
                        url,
                        $.extend(
                            {
                                "*searchMeta": "1"
                            },
                            params
                        ),
                        null
                    );
                    A.putCache($.md5(cacheKey) + "-searchMeta", pageMeta);
                }
                model = pageMeta.searchMeta;
            } else {
                var getData = pageMeta.models.filter(function (val) {
                    return val.name == action;
                });
                model = getData[0];
            }
            WIS_EMAP_SERV.modelName = model.modelName;
            WIS_EMAP_SERV.appName = model.appName;
            WIS_EMAP_SERV.url = model.url;
            WIS_EMAP_SERV.name = model.name;

            return _.cloneDeep(WIS_EMAP_SERV.convertModel(model, type));
        },
        domIsHide: function (targets) {
            if (!targets || !targets.length) {
                return false;
            }
            // 检查元素的显示状态
            if (targets.css("display") === "none" || targets.css("visibility") === "hidden") {
                return false;
            }

            // 检查透明度
            if (parseFloat(targets.css("opacity")) === 0) {
                return false;
            }

            // 检查是否被其他元素覆盖
            var rect = targets[0].getBoundingClientRect();
            var elementsAtPoint = document.elementsFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);

            // 如果目标元素不在该点的最上层，那么它可能被遮挡
            if (elementsAtPoint[0] !== targets[0]) {
                return false;
            }

            return true;
        }
    };

    function _getFieldValueFromQuerySetting(querySetting, name) {
        var value = undefined;
        _.each(querySetting, function (queryItem) {
            if (_.isArray(queryItem)) {
                value = _getFieldValueFromQuerySetting(queryItem, name);
            } else {
                if (queryItem.name === name) {
                    value = queryItem.value;
                }
            }
            if (value) {
                // 找到值
                return false;
            }
        });
        return value;
    }

    function _removeItemIfQuerySettingExists(querySetting, noItems) {
        _.each(querySetting, function (queryItem) {
            if (_.isArray(queryItem)) {
                _removeItemIfQuerySettingExists(queryItem, noItems);
            } else {
                if (noItems[queryItem.name] && queryItem.value) {
                    // 存在此项，并且值不为空，移除
                    delete noItems[queryItem.name];
                }
            }
        });
    }

    function replacepos(text, start, stop, replacetext) {
        return text.substring(0, start) + replacetext + text.substring(stop);
    }

    /**
     * 简单的有过期时间的缓存对象
     * @param timeout 超时时间，毫秒数
     * @constructor
     */
    function Cache(timeout) {
        // 默认30秒
        this.timeout = timeout || 30 * 1000;
        this.expired = undefined;
        this.cacheObj = undefined;

        Cache.prototype.set = function (cacheObj) {
            this.cacheObj = cacheObj;
            this.expired = new Date().getTime() + this.timeout;
        };

        /**
         * 获取缓存
         * @param cb 缓存对象获取到之后的回调
         * @param initFn 缓存初始化方法
         * @return {null}
         */
        Cache.prototype.get = function (cb, initFn) {
            var _this = this;
            // 缓存对象不存在，或已超过过期时间
            if (this.cacheObj === undefined || this.expired < new Date().getTime()) {
                if (initFn) {
                    // 调用初始化函数，将值设置到缓存中
                    initFn(function (cacheObj) {
                        _this.set(cacheObj);
                        cb && cb(cacheObj);
                    });
                } else {
                    // 返回undefined
                    cb && cb(undefined);
                }
            } else {
                cb && cb(this.cacheObj);
            }
        };
    }

    JW_UTILS._init();
    window.JW_UTILS = window.JWU = JW_UTILS;
})(window);
