(function (window) {
    var pubAppName = "jwcommon";

    function _afterPageInit(page) {
        var cb = _JW_CONFIG.afterPageInit;
        cb && cb(page);
    }

    var PAGE = {
        _init: function () {
            _afterPageInit(this);
        },
        /**
         * 加载jwcommon/web/public/script/common/page下的组件
         * @param componentName 组件名称
         * @param cb 回调
         */
        loadComponent: function (componentName, cb) {
            var path = contextPath + "/sys/" + pubAppName + "/public/script/common/page/{}/index.js";
            path = A.formatStr(path, componentName);
            getCommon().requireJs(path, cb);
        },
        /**
         * 加载表格页面
         * @param  {[type]} viewConfig [description]
         * @param  {[type]} params     [description]
         */
        loadGridPage: function (viewConfig, params) {
            var path = _getBaseView("bgym");
            getCommon().requireJs(path, function (bgym) {
                viewConfig.pushSubView(bgym);
                bgym.initialize(params);
            });
        },
        /**
         * 加载卡片页面
         * @param viewConfig
         * @param params
         */
        loadCardPage: function (viewConfig, params) {
            var path = _getBaseView("kpym");
            getCommon().requireJs(path, function (kpym) {
                viewConfig.pushSubView(kpym);
                kpym.initialize(params);
            });
        },
        /**
         * 加载tab页签页面
         * @param viewConfig
         * @param params
         */
        loadTabPage: function (viewConfig, params) {
            var path = _getBaseView("tabym");
            getCommon().requireJs(path, function (tabym) {
                viewConfig.pushSubView(tabym);
                viewConfig._tabym = tabym;
                tabym.initialize(params);
            });
        },
        /**
         * 加载步骤页面
         * @param viewConfig
         * @param params
         */
        loadWizardPage: function (viewConfig, params) {
            var path = _getBaseView("bzym");
            getCommon().requireJs(path, function (bzym) {
                viewConfig.pushSubView(bzym);
                viewConfig._bzym = bzym;
                bzym.initialize(params);
            });
        },
        /**
         * 加载左侧菜单页面
         * @param viewConfig
         * @param params
         */
        loadMenuPage: function (viewConfig, params) {
            var path = _getBaseView("menuym");
            getCommon().requireJs(path, function (menuym) {
                viewConfig.pushSubView(menuym);
                viewConfig._menuym = menuym;
                menuym.initialize(params);
            });
        },
        /**
         * 加载进度条页面
         * @param  {[type]} viewConfig [description]
         * @param  {[type]} params     [description]
         * @return {[type]}            [description]
         */
        loadProgressBar: function (viewConfig, params) {
            var path = _getBaseView("progress-bar");
            getCommon().requireJs(path, function (progressBar) {
                viewConfig.pushSubView(progressBar);
                viewConfig._progressBar = progressBar;
                progressBar.initialize(params);
            });
        },

         /**
         * @typedef {Object} Params
         * @property {number} [interval] 获取扫码状态的间隔时间 default 2000
         * @property {function} [failCallback] 签名失败后的回调 default undefined
         * @property {HTMLElement} [element] 二维码容器,不传则默认弹窗
         * @property {string} token 唯一键
         * @property {string} [title] 弹窗默认的标题
         * @property {string} [cancelSelector] 取消的选择器按钮
         * @property {function} [cancel] 取消的回调事件
         * @property {number} [timeout] 监听事件超时时间
         * @property {Object} [params] 状态库额外携带的参数
         * @property {'01' | '02' | '01,02'} [signMethod] 签名方式 01 微信扫码签名 02 上传签名  default 01,02
         */

        /**
         * 扫码签名通用页面
         * @param {object} viewConfig - 页面config对象  用于绑定evenmap事件
         * @param {Params} params - 扫码签名通用页面配置传参
         * @param {function} [callback] 签名成功之后的回调 default undefined
         * @returns {void}
         * @see Params
         *
         *
         * @example
         *
         * JWP.loadQrSign(undefined, {
         *  token:  BH_UTILS.NewGuid().replace(/-/g, ""),
         *  timeout: 120,
         *  callback: function (result, params) {
         *  }
         * });
         */
        loadQrSign: function (viewConfig, option, callback) {
            var path = _getBaseView("qrsign");
            if (_.isFunction(callback)) {
                option.callback = callback;
            }
            getCommon().requireJs(path, function (vc) {
                if (viewConfig) {
                    viewConfig.pushSubView(vc);
                    viewConfig.__qrsign = vc;
                }
                vc.initialize(option);
            });
        },
        loadQrSignView: function (viewConfig, option) {
            var path = _getBaseView("qrsign-view");
            getCommon().requireJs(path, function (vc) {
                viewConfig.pushSubView(vc);
                viewConfig["_qrsign-view"] = vc;
                vc.initialize(option);
            });
        },
        loadQueryCard: function (cb) {
            var path = contextPath + "/sys/" + pubAppName + "/public/script/common/page/cykp/index.js";
            getCommon().requireJs(path, cb);
        },
        loadAuditInstance: function (cb) {
            var path = contextPath + "/sys/" + pubAppName + "/public/script/common/page/jwAuditInstance/index.js";
            getCommon().requireJs(path, cb);
        },
        loadFlow: function (cb) {
            var path = contextPath + "/sys/" + pubAppName + "/public/script/common/page/cylc/index.js";
            getCommon().requireJs(path, cb);
        },
        loadVueProgress: function (option) {
            var path = contextPath + "/sys/" + pubAppName + "/public/script/common/page/vue-progress/vue-progress.js";
            getCommon().requireJs(path, function (vc) {
                vc.initialize(option);
            });
        }
    };

    function getCommon() {
        return window._JW_CONFIG.getCommon();
    }

    function _getBaseView(page, index) {
        index = index || page;
        var base = "../";
        var level = window._JW_CONFIG.pageLevel;
        for (var i = 0; i < level; i++) {
            base += "../";
        }
        var basePath = base + pubAppName + "/public/script/common/page/";
        return basePath + page + "/" + index;
    }

    PAGE._init();
    var globalName = window._JW_CONFIG.COMMON_PAGE_NAME;
    window[globalName] = PAGE;

    _JW_BOOT_HELPER.addFrameworkInitCallback(function () {
        // 给body添加属性 方便根据参数动态设置 body > main > article flex布局
        // _JW_INIT_CONFIG.jwTableMinHeight = undefined
        var appConfig = require("configUtils");
        if (appConfig && appConfig.LAYOUT_FLEX && _JW_INIT_CONFIG.jwTableHeightAuto) {
            $("body").attr("layout-flex", "");
            var utils = require("utils");
            var up = utils.getUserParams();
            if (up.min && up.min === "1") {
                $("body").attr("layout-full", "");
            }
        }

        if (window._JW_INIT_CONFIG.disableEmapEditorUpload) {
            var toolbar = [
                ["style", ["style"]],
                ["font", ["bold", "underline", "italic", "clear"]],
                ["fontname", ["fontname"]],
                ["color", ["color"]],
                ["para", ["ul", "ol", "paragraph"]],
                ["table", ["table"]],
                ["insert", ["link", "hr"]],
                ["view", ["codeview"]]
            ];
            if (!WIS_EMAP_CONFIG.emapEditor) {
                WIS_EMAP_CONFIG.setOptions({ emapEditor: { toolbar: toolbar } });
            } else {
                WIS_EMAP_CONFIG.emapEditor["toolbar"] = toolbar;
            }
        }

        // 覆盖utils.getUserParams
        var utils = require("utils");

        utils.getUserParams = function () {
            var queryString = location.search && location.search.substr(1);
            if (!queryString) {
                return {};
            }

            var params = {};
            var pairs = queryString.split("&");

            for (var i = 0; i < pairs.length; i++) {
                var pair = pairs[i].split("=");
                var key = decodeURIComponent(pair[0]);
                var value = decodeURIComponent(pair[1] || "");
                params[key] = value;
            }

            return params;
        };
    });
})(window);
