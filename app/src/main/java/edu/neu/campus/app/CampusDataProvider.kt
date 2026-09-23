package edu.neu.campus.app

import android.content.Context
import edu.neu.campus.contract.AcademicRepository
import edu.neu.campus.contract.PortalRepository
import edu.neu.campus.contract.SessionRepository
import edu.neu.campus.repository.CampusData

object CampusDataProvider {
    private var realData: CampusData? = null

    fun init(context: Context) {
        if (realData == null) {
            realData = CampusData.get(context.applicationContext)
        }
    }

    val session: SessionRepository
        get() = realData!!.session

    val academic: AcademicRepository
        get() = realData!!.academic

    val portal: PortalRepository
        get() = realData!!.portal
}
