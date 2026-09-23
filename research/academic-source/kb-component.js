/**
 * Created by jwu on 2023/8/14
 * E-mail:01313101@wisedu.com
 *
 * Description:课表组件
 */

define(function (require) {

    if (!Array.prototype.includes) {
        Array.prototype.includes = function (searchElement, fromIndex) {
            if (this == null) {
                throw new TypeError("Array.prototype.includes called on null or undefined");
            }

            var O = Object(this);
            var len = O.length >>> 0;

            if (len === 0) {
                return false;
            }

            var n = fromIndex | 0;
            var k = Math.max(n >= 0 ? n : len - Math.abs(n), 0);

            while (k < len) {
                if (O[k] === searchElement) {
                    return true;
                }
                k++;
            }

            return false;
        };
    }

    if (!Array.from) {
        Array.from = (function () {
            var toStr = Object.prototype.toString;
            var isCallable = function (fn) {
                return typeof fn === 'function' || toStr.call(fn) === '[object Function]';
            };
            var toInteger = function (value) {
                var number = Number(value);
                if (isNaN(number)) { return 0; }
                if (number === 0 || !isFinite(number)) { return number; }
                return (number > 0 ? 1 : -1) * Math.floor(Math.abs(number));
            };
            var maxSafeInteger = Math.pow(2, 53) - 1;
            var toLength = function (value) {
                var len = toInteger(value);
                return Math.min(Math.max(len, 0), maxSafeInteger);
            };

            // The length property of the from method is 1.
            return function from(arrayLike/*, mapFn, thisArg */) {
                // 1. Let C be the this value.
                var C = this;

                // 2. Let items be ToObject(arrayLike).
                var items = Object(arrayLike);

                // 3. ReturnIfAbrupt(items).
                if (arrayLike == null) {
                    throw new TypeError("Array.from requires an array-like object - not null or undefined");
                }

                // 4. If mapfn is undefined, then let mapping be false.
                var mapFn = arguments.length > 1 ? arguments[1] : void undefined;
                var T;
                if (typeof mapFn !== 'undefined') {
                    // 5. else
                    // 5. a If IsCallable(mapfn) is false, throw a TypeError exception.
                    if (!isCallable(mapFn)) {
                        throw new TypeError('Array.from: when provided, the second argument must be a function');
                    }

                    // 5. b. If thisArg was supplied, let T be thisArg; else let T be undefined.
                    if (arguments.length > 2) {
                        T = arguments[2];
                    }
                }

                // 10. Let lenValue be Get(items, "length").
                // 11. Let len be ToLength(lenValue).
                var len = toLength(items.length);

                // 13. If IsConstructor(C) is true, then
                // 13. a. Let A be the result of calling the [[Construct]] internal method of C with an argument list containing the single item len.
                // 14. a. Else, Let A be ArrayCreate(len).
                var A = isCallable(C) ? Object(new C(len)) : new Array(len);

                // 16. Let k be 0.
                var k = 0;
                // 17. Repeat, while k < len… (also steps a - h)
                var kValue;
                while (k < len) {
                    kValue = items[k];
                    if (mapFn) {
                        A[k] = typeof T === 'undefined' ? mapFn(kValue, k) : mapFn.call(T, kValue, k);
                    } else {
                        A[k] = kValue;
                    }
                    k += 1;
                }
                // 18. Let putStatus be Put(A, "length", len, true).
                A.length = len;
                // 20. Return A.
                return A;
            };
        }());
    }

    require("css!./index.css");
    var tpl = require("text!./index.vue");

    var Timetable = require("./components/timetable/index");
    var OtherCourses = require("./components/otherCourses/index");

    function init(rootElementId, params) {
        A.loadElementUI(function () {
            $("#" + rootElementId).html(tpl);

            new Vue({
                el: "#" + rootElementId,
                components: {
                    Timetable: Timetable,
                    OtherCourses: OtherCourses
                },
                provide: function () {
                    return {
                        onShowMultiCourse: params.onShowMultiCourse,
                        darkenHexColor: this.darkenHexColor
                    };
                },
                data: function () {
                    var currentSelectedZC = params.ZCDatasource[0].serialNumber;
                    for (var i = 0; i < params.ZCDatasource.length; i++) {
                        if (params.ZCDatasource[i].curWeek) {
                            currentSelectedZC = params.ZCDatasource[i].serialNumber;
                            break;
                        }
                    }
                    // 请求行列模式设置
                    var timeTableDisplayType = params.timeTableDisplayType || $.jwGetXtcs("KBBP", "KBHLMSSZ", function () {
                    }, true)[0].CSZA;
                    // 请求周天数显示设置
                    var weekdayDisplayMode = params.weekdayDisplayMode || $.jwGetXtcs("KBBP", "KBZTSXSSZ", function () {
                    }, true)[0].CSZA;

                    var weekdaysDatasource = params.weekdaysDatasource;
                    if (weekdayDisplayMode === '02') {
                        // 周天数显示模式为5天模式
                        weekdaysDatasource = weekdaysDatasource.filter(function (weekday) {
                            return weekday.id !== 6 && weekday.id !== 7;
                        });
                    }

                    return {
                        weekdayDisplayMode: weekdayDisplayMode,// 周天数显示模式，7/5
                        timeTableDisplayType: timeTableDisplayType,
                        printOption: params.printOption,
                        hidePrint: params.hidePrint,
                        XNXQDatasource: params.XNXQDatasource, // 学年学期数据源
                        currentSelectedXNXQ: params.currentXNXQ, // 当前学年学期

                        ZCDatasource: params.ZCDatasource, // 周次数据源
                        currentSelectedZC: currentSelectedZC, // 当前周次

                        XQDatasource: [], // 校区数据源
                        currentSelectedXQ: '', // 当前校区

                        currentKBType: params.KBType || "XQ", // 当前课表类型（XQ：学期课表；ZC：周课表）

                        weekdaysDatasource: weekdaysDatasource, // 星期数据源
                        sectionDatasource: [], // 节次数据源

                        fixedCellHeight: typeof params.fixedCellHeight === "boolean" ? params.fixedCellHeight : true, // 课表单元格高度是否固定，默认固定
                        arrangedList: [], // 课表数据
                        notArrangeList: [], // 未安排上课节次课程
                        practiceList: [], // 集中实践课程
                        confirmationScheduleCustom: {
                            button: "",
                            time: ""
                        } // 自定义确认时间
                    };
                },
                created: function () {
                    var that = this;

                    // 请求校区
                    this.getXQData();

                    // 请求节次数据源
                    params.getSection(
                        {
                            XNXQDM: this.currentSelectedXNXQ,
                            XQDM: this.currentSelectedXQ
                        },
                        function (data) {
                            that.sectionDatasource = data;
                        }
                    );

                    // 请求课表数据
                    this.getTimetableData();
                    if (params.confirmationScheduleCustom && $.type(params.confirmationScheduleCustom) === "function") {
                        params.confirmationScheduleCustom(this.currentSelectedXNXQ, function (temp) {
                            that.confirmationScheduleCustom = temp;
                        });
                    }
                },
                methods: {
                    toggleTableTime: function () {
                        if (this.timeTableDisplayType == '01') {
                            this.timeTableDisplayType = '02';
                        } else {
                            this.timeTableDisplayType = '01';
                        }
                    },
                    hasPermission: function (permissionKey) {
                        // 如果没有key直接显示按钮
                        if (!permissionKey) {
                            return true;
                        }
                        return $.jwAppConfig.hasPermission(permissionKey);
                    },
                    showPrintButtonItem: function (btn) {
                        if (this.printOption.hasOwnProperty(btn) && $.type(this.printOption[btn] === "object") && this.hasPermission(this.printOption[btn].permissionKey)) {
                            return true;
                        }
                        return false;
                    },
                    onPrintBtnClick: function (item) {
                        switch (item) {
                            case "word":
                                params.wordPrint({
                                    XNXQDM: this.currentSelectedXNXQ,
                                    KBLX: params.kblx,
                                    XXXQDM: this.currentSelectedXQ,
                                    KBTYPE: this.currentKBType,
                                    ZC: this.currentSelectedZC
                                });
                                break;
                            case "excel":
                                params.excelPrint({
                                    XNXQDM: this.currentSelectedXNXQ,
                                    KBLX: params.kblx,
                                    XXXQDM: this.currentSelectedXQ,
                                    KBTYPE: this.currentKBType,
                                    ZC: this.currentSelectedZC
                                });
                                break;
                            case "pdf":
                                params.pdfPrint({
                                    XNXQDM: this.currentSelectedXNXQ,
                                    KBLX: params.kblx,
                                    XXXQDM: this.currentSelectedXQ,
                                    KBTYPE: this.currentKBType,
                                    ZC: this.currentSelectedZC
                                });
                                break;
                            default:
                                break;
                        }
                    },
                    smoothScrollTo: function (element, direction, targetPosition, duration) {
                        var startPosition = element.scrollLeft;
                        var startTime = performance.now();

                        function scroll(timestamp) {
                            var elapsedTime = timestamp - startTime;
                            var progress = Math.min(elapsedTime / duration, 1);
                            element.scrollLeft = direction === "left" ? startPosition - targetPosition * progress : startPosition + targetPosition * progress;

                            if (progress < 1) {
                                requestAnimationFrame(scroll);
                            }
                        }

                        requestAnimationFrame(scroll);
                    },
                    moveLeft: function () {
                        this.smoothScrollTo(this.$refs["kbapp-xq-wrapper"], "left", 200, 200);
                    },
                    moveRight: function () {
                        this.smoothScrollTo(this.$refs["kbapp-xq-wrapper"], "right", 200, 200);
                    },
                    getCurrentSelectedXNXQIndex: function () {
                        var currentIndex = 0;
                        for (var i = 0; i < this.XNXQDatasource.length; i++) {
                            if (this.currentSelectedXNXQ === this.XNXQDatasource[i].DM) {
                                currentIndex = i;
                                break;
                            }
                        }

                        return currentIndex;
                    },
                    switchXNXQ: function (type) {
                        var currentIndex = this.getCurrentSelectedXNXQIndex();

                        if (type === "pre") {
                            // 向前切换
                            if (currentIndex > 0) {
                                this.currentSelectedXNXQ = this.XNXQDatasource[currentIndex - 1].DM;
                            }
                        } else {
                            // 向后切换
                            if (currentIndex < this.XNXQDatasource.length - 1) {
                                this.currentSelectedXNXQ = this.XNXQDatasource[currentIndex + 1].DM;
                            }
                        }
                    },
                    getCurrentSelectedZCIndex: function () {
                        var currentIndex = 0;
                        for (var i = 0; i < this.ZCDatasource.length; i++) {
                            if (this.currentSelectedZC === this.ZCDatasource[i].serialNumber) {
                                currentIndex = i;
                                break;
                            }
                        }

                        return currentIndex;
                    },
                    switchZC: function (type) {
                        var currentIndex = this.getCurrentSelectedZCIndex();

                        if (type === "pre") {
                            // 向前切换
                            if (currentIndex > 0) {
                                this.currentSelectedZC = this.ZCDatasource[currentIndex - 1].serialNumber;
                            }
                        } else {
                            // 向后切换
                            if (currentIndex < this.ZCDatasource.length - 1) {
                                this.currentSelectedZC = this.ZCDatasource[currentIndex + 1].serialNumber;
                            }
                        }
                    },
                    switchXQ: function (XQ) {
                        this.currentSelectedXQ = XQ;
                    },
                    switchKBType: function (type) {
                        this.currentKBType = type;
                    },
                    getXQData: function () {
                        var that = this;
                        params.getXQ(this.currentSelectedXNXQ, function (data) {
                            that.XQDatasource = data;
                            // 默认选中第一个校区
                            if (that.XQDatasource.length > 0) {
                                that.currentSelectedXQ = that.XQDatasource[0].id;
                            }
                        });
                    },
                    getTimetableData: function () {
                        // 清空课表数据
                        this.arrangedList = [];
                        this.notArrangeList = [];
                        this.practiceList = [];

                        var param = {
                            XNXQDM: this.currentSelectedXNXQ,
                            XQDM: this.currentSelectedXQ
                        };

                        if (this.currentKBType === "ZC") {
                            param.ZC = this.currentSelectedZC;
                        }

                        var that = this;
                        params.getTimetable(param, function (data) {
                            that.arrangedList = data.arrangedList;
                            that.notArrangeList = data.notArrangeList;
                            that.practiceList = data.practiceList;
                        });
                        that.$nextTick(function (){
                            // 课表列表
                            params.loadKblb(param);
                        });
                    },
                    darkenHexColor: function (hexColor, amount) {
                        // 去除颜色值中的 # 号
                        hexColor = hexColor.replace("#", "");

                        // 将颜色值拆分成红、绿、蓝通道
                        const r = parseInt(hexColor.substr(0, 2), 16);
                        const g = parseInt(hexColor.substr(2, 2), 16);
                        const b = parseInt(hexColor.substr(4, 2), 16);

                        // 减少每个通道的值
                        const lightenedR = Math.max(r - amount, 0);
                        const lightenedG = Math.max(g - amount, 0);
                        const lightenedB = Math.max(b - amount, 0);

                        // 将新的通道值转换为十六进制并拼接
                        const newHexColor =
                            (lightenedR < 16 ? "0" : "") + lightenedR.toString(16) + (lightenedG < 16 ? "0" : "") + lightenedG.toString(16) + (lightenedB < 16 ? "0" : "") + lightenedB.toString(16);

                        return "#" + newHexColor;
                    }
                },
                computed: {
                    showPrintBtn: function () {
                        if (this.hidePrint && this.hidePrint === "1") {
                            return false;
                        }
                        if (!this.printOption || $.type(this.printOption) !== "object") {
                            return false;
                        }
                        var keys = Object.keys(this.printOption);
                        if (!_.includes(keys, "word") && !_.includes(keys, "excel") && !_.includes(keys, "prf")) {
                            return false;
                        }
                        var flag1 = this.printOption.word && $.type(this.printOption.word) === 'object' && this.hasPermission(this.printOption.word.permissionKey);
                        var flag2 = this.printOption.excel && $.type(this.printOption.excel) === 'object' && this.hasPermission(this.printOption.excel.permissionKey);
                        var flag3 = this.printOption.pdf && $.type(this.printOption.pdf) === 'object' && this.hasPermission(this.printOption.pdf.permissionKey);
                        if (!flag1 && !flag2 && !flag3) {
                            return false;
                        }
                        return true;
                    },
                    currentSelectedXNXQMC: function () {
                        var name = "";
                        for (var i = 0; i < this.XNXQDatasource.length; i++) {
                            if (this.currentSelectedXNXQ === this.XNXQDatasource[i].DM) {
                                name = this.XNXQDatasource[i].MC;
                                break;
                            }
                        }
                        return name;
                    },
                    currentSelectedZCMC: function () {
                        var name = "";
                        for (var i = 0; i < this.ZCDatasource.length; i++) {
                            if (this.currentSelectedZC === this.ZCDatasource[i].serialNumber) {
                                name = this.ZCDatasource[i].name;
                                break;
                            }
                        }
                        return name;
                    },
                    currentSelectedZCTime: function () {
                        var startTime = "";
                        var endTime = "";
                        for (var i = 0; i < this.ZCDatasource.length; i++) {
                            if (this.currentSelectedZC === this.ZCDatasource[i].serialNumber) {
                                startTime = this.ZCDatasource[i].startDate;
                                endTime = this.ZCDatasource[i].endDate;
                                break;
                            }
                        }
                        var startDate = "";

                        if (startTime) {
                            var startMonth = new Date(startTime).getMonth() + 1;
                            var startDay = new Date(startTime).getDate();
                            startDate = startMonth + "/" + startDay;
                        }

                        var endDate = "";

                        if (endTime) {
                            var endMonth = new Date(endTime).getMonth() + 1;
                            var endDay = new Date(endTime).getDate();
                            endDate = endMonth + "/" + endDay;
                        }

                        return (startDate || "暂无") + " ~ " + (endDate || "暂无");
                    }
                },
                watch: {
                    currentKBType: function (type) {
                        var that = this;

                        var param = {
                            XQDM: this.currentSelectedXQ,
                            XNXQDM: this.currentSelectedXNXQ
                        };
                        if (type === "ZC") {
                            param.ZC = this.currentSelectedZC;
                        }

                        params.getSection(param, function (data) {
                            that.sectionDatasource = data;
                        });

                        // 请求课表数据
                        this.getTimetableData();
                    },
                    currentSelectedXNXQ: function (newValue) {
                        // 请求校区
                        this.getXQData();

                        // 请求周次数据
                        var that = this;
                        params.getZC({XNXQDM: newValue}, function (data) {
                            that.ZCDatasource = data;

                            var currentSelectedZC = data[0].serialNumber;
                            for (var i = 0; i < data.length; i++) {
                                if (data[i].curWeek) {
                                    currentSelectedZC = data[i].serialNumber;
                                    break;
                                }
                            }

                            that.currentSelectedZC = currentSelectedZC;
                        });

                        if (this.currentKBType === "XQ") {
                            params.getSection(
                                {
                                    XQDM: this.currentSelectedXQ,
                                    XNXQDM: newValue
                                },
                                function (data) {
                                    that.sectionDatasource = data;
                                }
                            );

                            // 请求课表数据
                            this.getTimetableData();
                        }
                        if (params.confirmationScheduleCustom && $.type(params.confirmationScheduleCustom) === "function") {
                            params.confirmationScheduleCustom(newValue, function (temp) {
                                that.confirmationScheduleCustom = temp;
                            });
                        }
                    },
                    currentSelectedZC: function (ZC) {
                        if (this.currentKBType === "ZC") {
                            // 请求节次数据源
                            var that = this;
                            params.getSection(
                                {
                                    XQDM: this.currentSelectedXQ,
                                    XNXQDM: this.currentSelectedXNXQ,
                                    ZC: ZC
                                },
                                function (data) {
                                    that.sectionDatasource = data;
                                }
                            );

                            // 请求课表数据
                            this.getTimetableData();
                        }
                    },
                    currentSelectedXQ: function () {
                        // 请求节次数据源
                        var that = this;
                        var param = {
                            XQDM: this.currentSelectedXQ,
                            XNXQDM: this.currentSelectedXNXQ
                        };
                        if (this.currentKBType === "ZC") {
                            param.ZC = this.currentSelectedZC;
                        }
                        params.getSection(param, function (data) {
                            that.sectionDatasource = data;
                        });

                        // 请求课表数据
                        this.getTimetableData();
                    }
                }
            });
        });
    }

    return init;
});
