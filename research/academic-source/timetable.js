/**
 * Created by jwu on 2023/8/16
 * E-mail:01313101@wisedu.com
 *
 * Description:"课表"组件
 */

define(function (require) {

    require("css!./index.css");
    var tpl = require("text!./index.vue");

    var dayColumn = require("./components/dayColumn/index");

    return {
        name: "Timetable",
        template: tpl,

        components: {dayColumn: dayColumn},
        props: [
            "weekdaysDatasource",
            "sectionDatasource",
            "arrangedList",
            "fixedCellHeight",
            "weekdayDisplayMode",
            "timeTableDisplayType"
        ],
        data: function () {
            return {
                sections: [], // 节次数据源
                displaySections: [], // 展示的节次数据源
                accurateSection: true, // 是否精确展示节次信息
                showSectionTime: false, //是否展示节次时间
                weekdays: this.weekdaysDatasource, // 星期数据源
                firstDayOfWeek: this.weekdaysDatasource[0].id, // 一周的开始是周几
                bgBlocks: [], // 背景网格
                totalScales: 0, // 课表总刻度
                coursesByDay: [], // 按天分组存放的课程
                highestCellHeight: 0,
                cellHeight: this.weekdayDisplayMode === '01' ? [115, 115, 115, 115, 115, 115, 115] : [115, 115, 115, 115, 115],
                timetableHeight: 805,
                WEEKTITLENAME: ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"],
                activeSections: [],
                activeWeekDays: []
            };
        },
        provide: function () {
            return {
                reportCellHeight: this.reportCellHeight
            };
        },
        methods: {
            kbappTimetableDayColumnStyle: function (timeTableDisplayType, item, height) {
                if (timeTableDisplayType === '01') {
                    return {flex: item.maxCourseNumber, minWidth: item.maxCourseNumber * 85 + 'px'};
                }
                return {
                    minHeight: height + "px",
                    maxHeight: height + "px"
                };
            },
            reportCellHeight: function (height, weekDay) {
                if (this.timeTableDisplayType == '02') {
                    if (this.weekdays[0].id === 7) {
                        if (weekDay == 6 && this.cellHeight[0] < height) {
                            this.cellHeight[0] = height;
                        } else if (this.cellHeight[weekDay + 1] < height) {
                            this.cellHeight[weekDay + 1] = height;
                        }
                    } else if (this.cellHeight[weekDay] < height) {
                        this.cellHeight[weekDay] = height;
                    }
                    var sum = 0;
                    this.cellHeight.forEach(function (ele) {
                        sum += ele;
                    });
                    this.timetableHeight = sum + 42;
                } else {
                    // 由时间片提报其能显示得下所有内容时的单元格高度，使用提报的最大高度配合节次数设置课表高度，保证每个时间片能显示得下所有内容
                    if (height > this.highestCellHeight) {
                        this.highestCellHeight = height;
                        if (this.timeTableDisplayType == '01') {
                            this.timetableHeight = height * this.sections.length + 40; // 计算总高度（40px 是星期单元格的高度）
                        }
                    }
                }
            },
            genDayColumnKey: function (index, courses) {
                return index + JSON.stringify(this.sections) + JSON.stringify(courses);
            },
            calculateMinutesBetweenTimes: function (startTime, endTime) {
                var startTimeParts = startTime.split(":");
                var endTimeParts = endTime.split(":");

                var startTotalMinutes = Number(startTimeParts[0]) * 60 + Number(startTimeParts[1]);
                var endTotalMinutes = Number(endTimeParts[0]) * 60 + Number(endTimeParts[1]);

                // 计算时间间隔的分钟数
                return endTotalMinutes - startTotalMinutes;
            },
            getTimePositionInDayColumn: function (time, position, type) {
                // 时间小于等于第一节课的开始时间，或者大于等于最后一节课的结束时间，直接返回最小或最大位置
                if (this.calculateMinutesBetweenTimes(this.sections[0].startTime, time) <= 0) {
                    return this.sections[0].dayColumnStartPosition;
                } else if (this.calculateMinutesBetweenTimes(time, this.sections[this.sections.length - 1].endTime) <= 0) {
                    return this.sections[this.sections.length - 1].dayColumnEndPosition;
                }

                // 检查占用了哪节课
                for (var i = 0; i < this.sections.length; i++) {
                    if (this.calculateMinutesBetweenTimes(this.sections[i].startTime, time) >= 0 && this.calculateMinutesBetweenTimes(time, this.sections[i].endTime) >= 0) {
                        var durationFromCourseStartTime = this.calculateMinutesBetweenTimes(this.sections[i].startTime, time); // 计算给定时间距节次开始时间的时长
                        if (position === "start") {
                            return this.sections[i].dayColumnStartPosition + durationFromCourseStartTime; // 节次开始位置 + 时长，得到给定时间的具体位置
                        } else {
                            return this.sections[i].dayColumnStartPosition + durationFromCourseStartTime - 1; // 节次开始位置 + 时长 - 1，得到给定时间的具体位置
                        }
                    }
                }

                // 若没有占用课，检查占用了哪两节课之间的课间
                for (var j = 0; j < this.sections.length - 1; j++) {
                    if (this.calculateMinutesBetweenTimes(this.sections[j].endTime, time) > 0 && this.calculateMinutesBetweenTimes(time, this.sections[j + 1].startTime) > 0) {
                        if (type === "up") {
                            // 向上对齐，取上节课的结束位置
                            return this.sections[j].dayColumnEndPosition;
                        } else {
                            // 向下对齐，取下节课的开始位置
                            return this.sections[j + 1].dayColumnStartPosition;
                        }
                    }
                }
            },
            getPositionsInDayColumn: function (KSJC, JSJC, ZDYKSSJ, ZDYJSSJ) {
                var positions = [];
                if (!ZDYKSSJ) {
                    // 没有自定义开始时间的值，直接使用开始节次的位置
                    // 如果开始节次大于节次数据的总节次数，直接返回空，课表不展示这节课
                    if (KSJC > this.sections.length) {
                        return null;
                    }
                    positions[0] = this.sections[Math.max(KSJC - 1, 0)].dayColumnStartPosition;
                } else {
                    positions[0] = this.getTimePositionInDayColumn(ZDYKSSJ, "start", "down");
                }

                if (!ZDYJSSJ) {
                    // 没有自定义结束时间的值，直接使用结束节次的位置
                    positions[1] = this.sections[Math.min(JSJC - 1, this.sections.length - 1)].dayColumnEndPosition; // 防止课程节次超出总节次数
                } else {
                    positions[1] = this.getTimePositionInDayColumn(ZDYJSSJ, "end", "up");
                }

                return positions;
            },
            genRenderData: function (timetable) {
                // 将课程按日存放
                // var coursesByDay = [[], [], [], [], [], [], []];
                var that = this;

                this.activeSections = [];
                this.activeWeekDays = [];

                var coursesByDay = Array.from({length: this.weekdayDisplayMode === '01' ? 7 : 5}, function (_, index) {
                    return {
                        uuid: BH_UTILS.NewGuid(),
                        weekDay: index,
                        maxCourseNumber: 1,
                        renderData: [],
                        course: []
                    };
                });
                for (var k = 0; k < timetable.length; k++) {
                    // 5天模式下，过滤掉周六和周日的课
                    if (this.weekdayDisplayMode === '02' && (timetable[k].dayOfWeek === 6 || timetable[k].dayOfWeek === 7)) {
                        continue;
                    }

                    // 计算课程块在课表内的开始、结束位置
                    var positions = null;
                    if (this.accurateSection) {
                        positions = this.getPositionsInDayColumn(timetable[k].beginSection, timetable[k].endSection, timetable[k].beginTime, timetable[k].endTime);
                    } else {
                        // 开始节次在节次范围内时，才计算位置
                        if (timetable[k].beginSection <= this.sections.length) {
                            var start = this.sections[Math.max(timetable[k].beginSection - 1, 0)].dayColumnStartPosition;
                            // 防止结束节次超出总节次
                            var end = this.sections[Math.min(timetable[k].endSection - 1, this.sections.length - 1)].dayColumnEndPosition;
                            positions = [start, end];
                        }
                    }

                    // 有有效位置数据，才在课表中显示
                    if (positions) {
                        // 将timetable中的包含的节次存放
                        for (var s = timetable[k].beginSection; s <= timetable[k].endSection; s++) {
                            if (this.activeSections.indexOf(s) === -1) {
                                this.activeSections.push(s);
                            }
                        }
                        // 将timetable中的包含的星期存放
                        if (this.activeWeekDays.indexOf(timetable[k].dayOfWeek - 1) === -1) {
                            this.activeWeekDays.push(timetable[k].dayOfWeek - 1);
                        }

                        coursesByDay[timetable[k].dayOfWeek - 1].course.push(
                            Object.assign({}, timetable[k], {
                                dayColumnStartPosition: positions[0],
                                dayColumnEndPosition: positions[1]
                            })
                        );
                    }
                }
                if (this.weekdays[0].id === 7) {
                    coursesByDay.unshift(coursesByDay.pop());
                }

                coursesByDay.forEach(function (dayCourses, index) {
                    dayCourses.course.forEach(function (course) {
                        const sectionLength = that.displaySections.length;
                        let middleNumber = Math.ceil(sectionLength / 2);
                        if (that.timeTableDisplayType == '02') {
                            if (course.beginSection > middleNumber) {
                                course.popoverPosition = "left-start"; // 悬浮提示框
                            } else if (course.endSection < middleNumber) {
                                course.popoverPosition = "right-start"; // 悬浮提示框
                            } else {
                                course.popoverPosition = "top-start"; // 悬浮提示框
                            }
                        } else {
                            course.popoverPosition = index < 5 ? "right-start" : "left-start"; // 悬浮提示框，前 5 天在右侧展示，后 2 天在左侧展示
                        }
                    });
                });

                coursesByDay.forEach(function (item) {
                    var courseResult = that.dealWidthCourseData(item.course, that.totalScales);
                    item.renderData = courseResult.renderData;
                    item.maxCourseNumber = courseResult.maxCourseNumber;
                });
                this.coursesByDay = coursesByDay;
            },

            dealWidthCourseData: function (courses, totalScales) {
                let maxCourseNumber = 1; // 最大冲突课程数量
                var sortedCourses = courses.sort(function (a, b) {
                    return a.dayColumnStartPosition - b.dayColumnStartPosition;
                });
                // 构造课程渲染数据，将冲突的课程合并（不冲突的自成一条数据），用于传入 courseRender 展示
                var courseConflictGroups = [];
                if (sortedCourses.length > 0) {
                    for (var i = 0; i < sortedCourses.length; i++) {
                        var findConflictGroup = false;

                        for (var j = 0; j < courseConflictGroups.length; j++) {
                            for (var k = 0; k < courseConflictGroups[j].length; k++) {
                                if (
                                    courseConflictGroups[j][k].dayColumnStartPosition <= sortedCourses[i].dayColumnStartPosition &&
                                    sortedCourses[i].dayColumnStartPosition <= courseConflictGroups[j][k].dayColumnEndPosition
                                ) {
                                    // 开始位置在其他课程的位置范围内，表明有冲突，需要存放在同一个冲突组中
                                    courseConflictGroups[j].push(sortedCourses[i]);
                                    findConflictGroup = true;
                                    break;
                                }
                            }
                            if (findConflictGroup) {
                                break;
                            }
                        }

                        if (!findConflictGroup) {
                            // 与当前所有的冲突组都不冲突，新开一个组存放
                            courseConflictGroups.push([sortedCourses[i]]);
                        }
                    }
                }

                // 计算每个冲突组显示的位置
                var renderDataTemp = [];
                for (var i = 0; i < courseConflictGroups.length; i++) {
                    var conflictCourses = courseConflictGroups[i];
                    var startPosition = conflictCourses[0].dayColumnStartPosition;
                    var endPosition = conflictCourses[0].dayColumnEndPosition;

                    if (conflictCourses.length > 1) {
                        for (var j = 1; j < conflictCourses.length; j++) {
                            if (conflictCourses[j].dayColumnEndPosition > endPosition) {
                                endPosition = conflictCourses[j].dayColumnEndPosition;
                            }
                        }
                    }

                    // if (maxCourseNumber < conflictCourses?.length) {
                    //     maxCourseNumber = conflictCourses?.length;
                    // }

                    renderDataTemp.push({
                        uuid: BH_UTILS.NewGuid(),
                        type: "data", // 类型为数据
                        startPosition: startPosition, // 开始位置
                        endPosition: endPosition, // 结束位置
                        scalesSpan: endPosition - startPosition + 1, // 节次跨度（长度）
                        courses: conflictCourses // 冲突课程
                    });
                }

                // 检查每个冲突组前是否需要填入空白占位（flex 布局，需要使用空白占位把内容撑起来）
                var renderData = [];
                for (var i = 0; i < renderDataTemp.length; i++) {
                    // 处理前置空白占位
                    if (i === 0) {
                        if (renderDataTemp[0].startPosition > 1) {
                            // 第一节课开始位置不是起始位置，需要在前面填占位
                            renderData.push({
                                uuid: BH_UTILS.NewGuid(),
                                type: "placeholder",
                                startPosition: 1,
                                endPosition: renderDataTemp[0].startPosition - 1,
                                scalesSpan: renderDataTemp[0].startPosition - 1
                            });
                        }
                    } else {
                        if (renderDataTemp[i].startPosition - renderDataTemp[i - 1].endPosition > 1) {
                            // 当前课的开始位置与上节课的结束位置不能衔接，需要在之间填入占位
                            renderData.push({
                                uuid: BH_UTILS.NewGuid(),
                                type: "placeholder",
                                startPosition: renderDataTemp[i - 1].endPosition + 1,
                                endPosition: renderDataTemp[i].startPosition - 1,
                                scalesSpan: renderDataTemp[i].startPosition - renderDataTemp[i - 1].endPosition - 1
                            });
                        }
                    }

                    renderData.push(renderDataTemp[i]);

                    // 处理最后的空白占位
                    if (i === renderDataTemp.length - 1) {
                        if (renderDataTemp[i].endPosition < totalScales) {
                            // 最后一节课的结束位置不等于总的结束位置，需要在末尾填入占位
                            renderData.push({
                                uuid: BH_UTILS.NewGuid(),
                                type: "placeholder",
                                startPosition: renderDataTemp[i].endPosition + 1,
                                endPosition: totalScales,
                                scalesSpan: totalScales - renderDataTemp[i].endPosition
                            });
                        }
                    }
                }


                renderData.forEach(function (item) {
                    if (item.type === "data") {
                        var sortedCourses = item.courses.sort(function (a, b) {
                            return a.dayColumnStartPosition - b.dayColumnStartPosition;
                        });

                        // 构造课程渲染数据，按照课程位置冲突分组排列课程，一组就是一个完整的竖向空间
                        var courseColumnGroups = [];
                        if (sortedCourses.length > 0) {
                            for (var i = 0; i < sortedCourses.length; i++) {
                                var findPosition = false;

                                for (var j = 0; j < courseColumnGroups.length; j++) {
                                    // 判断当前课程与当前分组内课程是否有冲突，没有冲突则放入其中；有冲突则新开一个分组
                                    var noConflictInWholeGroup = true;
                                    for (var k = 0; k < courseColumnGroups[j].length; k++) {
                                        if (
                                            courseColumnGroups[j][k].dayColumnStartPosition <= sortedCourses[i].dayColumnStartPosition &&
                                            sortedCourses[i].dayColumnStartPosition <= courseColumnGroups[j][k].dayColumnEndPosition
                                        ) {
                                            // 开始位置在其他课程的位置范围内，表明有冲突
                                            noConflictInWholeGroup = false;
                                            break;
                                        }
                                    }
                                    if (noConflictInWholeGroup) {
                                        courseColumnGroups[j].push(sortedCourses[i]);
                                        findPosition = true;
                                        break;
                                    }
                                }

                                if (!findPosition) {
                                    // 当前已有的分组中没有空间存放当前课程，新开一个组存放
                                    courseColumnGroups.push([sortedCourses[i]]);
                                }
                            }
                        }
                        maxCourseNumber = maxCourseNumber <= courseColumnGroups.length ? courseColumnGroups.length : maxCourseNumber;
                    }
                });

                return {
                    renderData: renderData,
                    maxCourseNumber: maxCourseNumber
                };
            },

            getActiveSections: function (codes) {
                var that = this;
                return codes.some(function (code) {
                    var index = that.activeSections.indexOf(code);
                    return index !== -1;
                });
            },

            getActiveWeekDays: function (weekDay) {
                return this.activeWeekDays.indexOf(weekDay) === -1;
            }
        },
        watch: {
            timeTableDisplayType: function () {
                this.highestCellHeight = 0;
                this.cellHeight = this.weekdayDisplayMode === '01' ? [115, 115, 115, 115, 115, 115, 115] : [115, 115, 115, 115, 115];
            },
            sectionDatasource: {
                handler: function (newValue) {
                    if (newValue && newValue.length > 0) {
                        var that = this;

                        if (this.fixedCellHeight && newValue[0].startTime) {
                            // 根据时间精确展示
                            this.accurateSection = true;
                        } else {
                            // 根据节次模糊展示
                            this.accurateSection = false;
                        }

                        // 是否展示节次时间
                        this.showSectionTime = !!newValue[0].startTime;

                        var sectionsWithDuration = []; // 根据节次开始时间、结束时间获得时长
                        var position = 1; // 位置从 1 开始
                        newValue.forEach(function (item) {
                            var duration = that.accurateSection ? that.calculateMinutesBetweenTimes(item.startTime, item.endTime) : 1;
                            sectionsWithDuration.push(
                                Object.assign({}, item, {
                                    duration: duration,
                                    dayColumnStartPosition: position,
                                    dayColumnEndPosition: position + duration - 1
                                })
                            );
                            position += duration;
                        });

                        var enableBigSectionMode = false;
                        var sectionsByBigSectionGroup = [];
                        sectionsWithDuration.forEach(function (section) {
                            // 按大节将节次分组存放
                            if (section.sectionGroup) {
                                enableBigSectionMode = true; // 存在 sectionGroup，即需开启大节展示模式
                                var targetGroup = null;
                                for (var i = 0; i < sectionsByBigSectionGroup.length; i++) {
                                    if (sectionsByBigSectionGroup[i].bigSectionCode === section.sectionGroup.bigSectionCode) {
                                        targetGroup = sectionsByBigSectionGroup[i];
                                        break;
                                    }
                                }

                                if (targetGroup) {
                                    targetGroup.sections.push(section);
                                } else {
                                    sectionsByBigSectionGroup.push(Object.assign({}, section.sectionGroup, {sections: [section]}));
                                }
                            } else {
                                // 没有分组信息则自成一组
                                sectionsByBigSectionGroup.push({
                                    bigSectionCode: undefined,
                                    bigSectionName: undefined,
                                    sectionCode: undefined,
                                    sections: [section]
                                });
                            }
                        });


                        var displaySections = [];

                        sectionsByBigSectionGroup.forEach(function (group) {
                            var startSection = group.sections[0].code; // 取第一个节次的 code 作为开始节次
                            var startTime = group.sections[0].startTime; // 取第一个节次的 startTime 作为开始时间
                            var endSection = group.sections[group.sections.length - 1].code; // 取最后一个节次的 code 作为结束节次
                            var endTime = group.sections[group.sections.length - 1].endTime; // 取最后一个节次的 startTime 作为结束时间
                            var dayColumnStartPosition = group.sections[0].dayColumnStartPosition; // 取第一个节次的 dayColumnStartPosition 作为开始位置
                            var dayColumnEndPosition = group.sections[group.sections.length - 1].dayColumnEndPosition; // 取最后一个节次的 dayColumnEndPosition 作为结束位置
                            var name = group.bigSectionName || group.sections[0].name; // 取大节名称或自称一组的节次名称作为展示名称
                            var id = "";
                            var duration = 0;
                            var codes = [];
                            group.sections.forEach(function (section) {
                                id += section.id;
                                duration += section.duration;
                                codes.push(section.code);
                            });

                            var displaySection = {
                                id: id,
                                startSection: startSection,
                                startTime: startTime,
                                endSection: endSection,
                                endTime: endTime,
                                dayColumnStartPosition: dayColumnStartPosition,
                                dayColumnEndPosition: dayColumnEndPosition,
                                name: name,
                                duration: duration,
                                codes: codes
                            };

                            displaySections.push(displaySection);
                        });

                        // 背景网格
                        var bgBlocks = [];
                        for (var i = 0; i < this.weekdaysDatasource.length; i++) {
                            var dayBgBlocks = [];
                            for (var j = 0; j < sectionsWithDuration.length; j++) {
                                var section = j + 1;
                                // 判断该格子是否需要显示底边（合并成大节展示）
                                var showBottomLine = false;
                                for (var k = 0; k < displaySections.length; k++) {
                                    // 找到该节次所属大节
                                    if (displaySections[k].startSection <= section && section <= displaySections[k].endSection) {
                                        showBottomLine = section === displaySections[k].endSection; // 只有在最后一格的情况下，要展示底边
                                        break;
                                    }
                                }

                                dayBgBlocks.push({
                                    id: this.weekdaysDatasource[i].id + ":" + section, // 使用星期和节次作为格子 id
                                    flexGrow: sectionsWithDuration[j].duration,
                                    showBottomLine: showBottomLine
                                });
                            }
                            bgBlocks.push(dayBgBlocks);
                        }

                        this.sections = sectionsWithDuration;
                        this.displaySections = displaySections;
                        this.bgBlocks = bgBlocks;
                        this.totalScales = sectionsWithDuration[sectionsWithDuration.length - 1].dayColumnEndPosition;
                        this.highestCellHeight = (this.timetableHeight - 40) / this.sections.length; // 计算初始单元格高度作为最高高度（40px 是星期单元格的高度）
                    } else {
                        // 清空数据
                        this.highestCellHeight = 0;
                        this.sections = [];
                        this.bgBlocks = [];
                        this.totalScales = 0;
                        this.coursesByDay = [];
                    }
                },
                deep: true,
                immediate: true
            },
            arrangedList: {
                handler: function (newValue) {
                    this.highestCellHeight = 0;
                    this.timetableHeight = this.weekdayDisplayMode === '01' ? 115 * 7 + 42 : 115 * 5 + 42;
                    this.cellHeight = this.weekdayDisplayMode === '01' ? [115, 115, 115, 115, 115, 115, 115] : [115, 115, 115, 115, 115];
                    this.$nextTick(function () {
                        this.genRenderData(newValue || []);
                    });
                    // if (!newValue || newValue.length === 0) {
                    //     this.coursesByDay = []; // 清空课表
                    //     return;
                    // }

                    // if (this.sections && this.sections.length > 0) {
                    //     this.genRenderData(newValue);
                    // }
                },
                deep: true,
                immediate: true
            },
            coursesByDay: {
                handler: function (newValue) {
                },
                deep: true
            }
        }
    };
});
