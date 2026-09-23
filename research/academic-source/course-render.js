/**
 * Created by jwu on 2023/7/31
 * E-mail:01313101@wisedu.com
 *
 * Description:
 */

define(function (require, exports, module) {
    require("css!./index.css");
    var tpl = require("text!./index.vue");

    return {
        template: tpl,
        props: ["courses", "startPosition", "endPosition", "fixedCellHeight", "timeTableDisplayType", "weekday"],
        inject: ["darkenHexColor", "onShowMultiCourse", "reportCellHeight"],
        data: function () {
            // 根据位置从小到大排序
            var sortedCourses = this.courses.sort(function (a, b) {
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

            // 排列完课程后，每列中课程按照开始位置排序
            for (var i = 0; i < courseColumnGroups.length; i++) {
                courseColumnGroups[i] = courseColumnGroups[i].sort(function (a, b) {
                    return a.dayColumnStartPosition - b.dayColumnStartPosition;
                });
            }

            var renderColumns = [];
            for (var i = 0; i < courseColumnGroups.length; i++) {
                var renderColumn = [];
                for (var j = 0; j < courseColumnGroups[i].length; j++) {
                    var course = courseColumnGroups[i][j];
                    // 处理前置空白占位
                    if (j === 0) {
                        if (course.dayColumnStartPosition > this.startPosition) {
                            // 第一节课开始位置不是渲染组的开始位置，需要在前面填占位
                            renderColumn.push({
                                type: "placeholder",
                                uuid: BH_UTILS.NewGuid(),
                                startPosition: this.startPosition,
                                endPosition: course.dayColumnStartPosition - this.startPosition,
                                scalesSpan: course.dayColumnStartPosition - this.startPosition
                            });
                        }
                    } else {
                        if (course.dayColumnStartPosition - courseColumnGroups[i][j - 1].dayColumnEndPosition > 1) {
                            // 当前课的开始位置与上节课的结束位置不能衔接，需要在之间填入占位
                            renderColumn.push({
                                type: "placeholder",
                                uuid: BH_UTILS.NewGuid(),
                                startPosition: courseColumnGroups[i][j - 1].dayColumnEndPosition + 1,
                                endPosition: course.dayColumnStartPosition - 1,
                                scalesSpan: course.dayColumnStartPosition - courseColumnGroups[i][j - 1].dayColumnEndPosition - 1
                            });
                        }
                    }

                    renderColumn.push({
                        type: "data",
                        uuid: BH_UTILS.NewGuid(),
                        startPosition: course.dayColumnStartPosition,
                        endPosition: course.dayColumnEndPosition,
                        scalesSpan: course.dayColumnEndPosition - course.dayColumnStartPosition + 1,
                        data: course
                    });

                    // 处理最后的空白占位
                    if (j === courseColumnGroups[i].length - 1) {
                        if (course.dayColumnEndPosition < this.endPosition) {
                            // 最后一节课的结束位置不等于总位置数，需要在末尾填入占位
                            renderColumn.push({
                                type: "placeholder",
                                uuid: BH_UTILS.NewGuid(),
                                startPosition: course.dayColumnEndPosition + 1,
                                endPosition: this.endPosition,
                                scalesSpan: this.endPosition - course.dayColumnEndPosition
                            });
                        }
                    }
                }

                renderColumns.push({
                    uuid: BH_UTILS.NewGuid(),
                    courses: renderColumn
                });
            }

            return {
                renderColumns: renderColumns,
                clickTimer: null
            };
        },
        mounted: function () {
            var that = this;
            if (!that.fixedCellHeight) {
                that.$nextTick(function () {
                    that.$refs['kbappTimetableCourseRenderCourseItem'].forEach(function (item) {
                        var totalInfoTextHeight = 0;
                        var infoTextDoms = item.querySelectorAll('.kbappTimetableCourseRenderCourseItemInfoText');
                        for (var i = 0; i < infoTextDoms.length; i++) {
                            totalInfoTextHeight += infoTextDoms[i].clientHeight;
                        }
                        var scalesSpan = Number(item.getAttribute('data-scales-span'));
                        if (that.timeTableDisplayType == "01") {
                            that.reportCellHeight((totalInfoTextHeight + 0.5 + 0.5 + 2 + 2) / scalesSpan, that.weekday);// 加上所有的上下边框，除以所占格子数，得到单个单元格高度
                        } else {
                            that.reportCellHeight((totalInfoTextHeight + 0.5 + 0.5 + 2 + 2) * (that.renderColumns.length || 1), that.weekday);// 加上所有的上下边框，除以所占格子数，得到单个单元格高度
                        }
                    });
                });
            } else if (that.timeTableDisplayType == "02") {
                that.$nextTick(function () {
                    that.reportCellHeight(115 * (that.renderColumns.length || 1), that.weekday);// 加上所有的上下边框，除以所占格子数，得到单个单元格高度
                });
            }
        },
        methods: {
            getItemStyle: function (RWZXSBWID, color) {
                if (RWZXSBWID !== this.currentSelectedTimeSliceId) {
                    return {
                        opacity: this.isDragging ? 0.5 : 1,
                        backgroundColor: color || null,
                        borderColor: color || null
                    };
                } else {
                    return {
                        opacity: this.isDragging ? 0.5 : 1,
                        borderColor: color || null,
                        background: this.getBackground(color),
                        backgroundSize: "20px 20px"
                    };
                }
            },
            onCourseClick: function (multiCourse) {
                this.onShowMultiCourse(multiCourse);
            }
        }
    };
});
