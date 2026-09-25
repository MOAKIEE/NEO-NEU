package edu.neu.campus.app.feature.services

/** Fixed school-published entry points. No credentials or tickets are placed in URLs. */
enum class OfficialService(val title: String, val url: String) {
    PORTAL("智慧东大门户", "https://personal.neu.edu.cn/portal"),
    ACADEMIC("本科教务系统", "https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do"),
    ECODE("e 码通认证", "https://ecode.neu.edu.cn/ecode/#/"),
    CARD_RECHARGE("校园卡充值", "https://pay.neu.edu.cn/scardrecharge51264Z006.html"),
    NETWORK_RECHARGE("网费充值", "https://pay.neu.edu.cn/netdetails51247N005.html"),
    STUDENT_MAIL("学生邮箱", "https://mails.neu.edu.cn/"),
    PAYMENT_HALL("缴费服务大厅", "https://pay.neu.edu.cn/drCasLogin")
}
