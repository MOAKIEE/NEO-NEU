package edu.neu.campus.app

import android.content.Context
import edu.neu.campus.app.demo.DemoDataRepository
import edu.neu.campus.app.demo.DemoModeManager
import edu.neu.campus.contract.AcademicRepository
import edu.neu.campus.contract.PortalRepository
import edu.neu.campus.contract.SessionRepository
import edu.neu.campus.repository.CampusData

object CampusDataProvider {
    private var realData: CampusData? = null
    private val demoRepo = DemoDataRepository()

    fun init(context: Context) {
        if (realData == null) {
            realData = CampusData.get(context.applicationContext)
        }
    }

    val session: SessionRepository
        get() = if (DemoModeManager.isDemoMode) demoRepo else realData!!.session

    val academic: AcademicRepository
        get() = if (DemoModeManager.isDemoMode) demoRepo else realData!!.academic

    val portal: PortalRepository
        get() = if (DemoModeManager.isDemoMode) demoRepo else realData!!.portal
}
