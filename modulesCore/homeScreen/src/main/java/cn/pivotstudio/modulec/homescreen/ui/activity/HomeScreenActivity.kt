package cn.pivotstudio.modulec.homescreen.ui.activity

import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import cn.pivotstudio.moduleb.database.MMKVUtil
import cn.pivotstudio.moduleb.libbase.BuildConfig
import cn.pivotstudio.moduleb.libbase.base.ui.activity.BaseActivity
import cn.pivotstudio.moduleb.libbase.constant.Constant
import cn.pivotstudio.moduleb.libbase.constant.ResultCodeConstant
import cn.pivotstudio.modulec.homescreen.R
import cn.pivotstudio.modulec.homescreen.custom_view.dialog.UpdateDialog
import cn.pivotstudio.modulec.homescreen.custom_view.dialog.WelcomeDialog
import cn.pivotstudio.modulec.homescreen.databinding.ActivityHsHomescreenBinding
import cn.pivotstudio.modulec.homescreen.network.DownloadService
import cn.pivotstudio.modulec.homescreen.network.DownloadService.DownloadBinder
import cn.pivotstudio.modulec.homescreen.ui.fragment.ForestDetailFragment
import cn.pivotstudio.modulec.homescreen.ui.fragment.ForestFragment
import cn.pivotstudio.modulec.homescreen.ui.fragment.HomePageFragment
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * @classname: HomeScreenActivity
 * @description:
 * @date: 2022/4/28 20:58
 * @version:1.0
 * @author:
 */
@Route(path = "/homeScreen/HomeScreenActivity")
class HomeScreenActivity : BaseActivity() {
    private lateinit var binding: ActivityHsHomescreenBinding
    private lateinit var navController: NavController
    private val fragmentList = listOf(
        R.id.all_forest_fragment,
        R.id.forest_detail_fragment,
        R.id.holeFollowReplyFragment,
        R.id.itemMineFragment,
        R.id.itemDetailFragment2
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_hs_homescreen)
        initView()
        checkVersion()
    }

    /**
     * fragment中使用onActivityResult需要在此重写触发，使用navigation后activity的onActivityResult被调用后不会再触发子fragment的onActivityResult，需要手动调用
     */
    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val mMainNavFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        when (val fragment = mMainNavFragment!!.childFragmentManager.primaryNavigationFragment) {
            is HomePageFragment,
            is ForestDetailFragment,
            is ForestFragment -> {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        newConfig.uiMode
    }





    /**
     * 视图初始化
     */
    private fun initView() {

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.homeScreenToolbar.let {
            setSupportActionBar(it)
            it.setupWithNavController(
                navController,
                AppBarConfiguration(
                    setOf(
                        R.id.homepage_fragment,
                        R.id.forest_fragment,
                        R.id.notice_fragment,
                        R.id.mine_fragment,
                        R.id.forest_detail_fragment
                    )
                )
            )
        }


        navController.addOnDestinationChangedListener { _, destination, argument ->
            supportActionBar?.title = destination.label

            // BottomNavigationBar显示情况特判
            binding.apply {
                layoutBottomBar.isVisible =
                    !fragmentList.any { it == destination.id }

                bottomNavigationView.setupWithNavController(navController)
                bottomNavigationView.background = null
            }

            // ActionBar显示情况特判
            supportActionBar?.let {
                if (destination.id == R.id.forest_detail_fragment) {
                    it.hide()
                } else {
                    it.show()
                }
            }

        }


    }

    /**
     * 获取版本内容
     */
    private fun packageName(context: Context): String? {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return name
    }

    /**
     * 检查版本以及是否第一次使用
     */
    private fun checkVersion() {
//        val homeScreenRepository = HomeScreenRepository()
//        homeScreenRepository.getVersionMsgForNetwork()
//        homeScreenRepository.pHomeScreenVersionMsg.observe(this) { versionResponse: VersionResponse ->
//            val oldVersion = packageName(this@HomeScreenActivity)
//            val lastVersion = versionResponse.androidversion
//            val downloadUrl = versionResponse.androidUpdateUrl
//            if (lastVersion != oldVersion) { //如果当前不是新版本
//                val updateDialog =
//                    UpdateDialog(this@HomeScreenActivity, oldVersion, lastVersion, downloadUrl)
//                updateDialog.show()
//            } else { //是最新版本
//
//            }
//        }
        val mmkvUtil = MMKVUtil.getMMKV(this)
        if (!mmkvUtil.getBoolean(Constant.IS_FIRST_USED)) { //是否第一次使用1037树洞,保证welcomeDialog只在第一使用时显式
            val welcomeDialog = WelcomeDialog(context)
            welcomeDialog.show()
            mmkvUtil.put(Constant.IS_FIRST_USED, true)
        }

        val manager = this.packageManager
        var oldCode = 0L
        try {
            val info: PackageInfo = manager.getPackageInfo(this.packageName, 0)
            oldCode = info.longVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        if(4L > oldCode) {
            if(checkNotification()) {
                val updateDialog = UpdateDialog(context, oldCode.toString(), "4")
                updateDialog.show()
            }else {
                runBlocking {
                    Toast.makeText(context, "没有开启通知权限，请前往开启", Toast.LENGTH_SHORT).show()
                    delay(1000L)
                }
                val localIntent = Intent()
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                localIntent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                localIntent.data = Uri.fromParts("package", this.packageName, null)
                startActivity(localIntent)
            }
        }
    }

    private fun checkNotification(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled()

    /**
     * 监听点击事件
     */
    fun jumpToPublishHoleByARouter(v: View) {
        val id = v.id
        if (id == R.id.fab_homescreen_publishhole) {
            if (BuildConfig.isRelease) {
                ARouter.getInstance().build("/publishHole/PublishHoleActivity")
                    .navigation(this, ResultCodeConstant.PUBLISH_HOLE)
            } else {
                showMsg("当前为模块测试阶段")
            }
        }
    }

    private var firstTime: Long = 0

    /**
     * 点击退出键，连点两次退出
     *
     * @param keyCode
     * @param event
     * @return
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            navController.currentDestination?.let { navDestination ->
                if (fragmentList.any { it == navDestination.id } || supportFragmentManager.backStackEntryCount > 0) {
                    return navController.popBackStack()
                }else {
                    val secondTime = System.currentTimeMillis()
                    if (secondTime - firstTime > 2000) {
                        Toast.makeText(this@HomeScreenActivity, "再按一次退出程序", Toast.LENGTH_SHORT).show()
                        firstTime = secondTime
                        return true
                    } else {
                        finish()
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    fun setOnBottomBarItemReselectedListener(listener: NavigationBarView.OnItemReselectedListener) {
        binding.bottomNavigationView.setOnItemReselectedListener(listener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onNavigateUp(): Boolean {
        return navController.navigateUp() || super.onNavigateUp()
    }
}