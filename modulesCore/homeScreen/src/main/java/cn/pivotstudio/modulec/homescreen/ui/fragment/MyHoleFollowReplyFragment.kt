package cn.pivotstudio.modulec.homescreen.ui.fragment

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pivotstudio.husthole.moduleb.network.ApiStatus
import cn.pivotstudio.moduleb.libbase.base.ui.fragment.BaseFragment
import cn.pivotstudio.modulec.homescreen.R
import cn.pivotstudio.modulec.homescreen.databinding.FragmentMyholeBinding
import cn.pivotstudio.modulec.homescreen.oldversion.model.StandardRefreshFooter
import cn.pivotstudio.modulec.homescreen.oldversion.model.StandardRefreshHeader
import cn.pivotstudio.modulec.homescreen.ui.adapter.MineRecycleViewAdapter
import cn.pivotstudio.modulec.homescreen.viewmodel.HoleFollowReplyViewModel
import cn.pivotstudio.modulec.homescreen.viewmodel.MyHoleFragmentViewModel.Companion.GET_FOLLOW
import cn.pivotstudio.modulec.homescreen.viewmodel.MyHoleFragmentViewModel.Companion.GET_HOLE
import cn.pivotstudio.modulec.homescreen.viewmodel.MyHoleFragmentViewModel.Companion.GET_REPLY
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 *@classname MyHoleFollowReplyFragment
 * @description:
 * @date :2022/9/20 21:46
 * @version :1.0
 * @author
 */

class MyHoleFollowReplyFragment(val type: Int) : BaseFragment() {
    private val viewModel: HoleFollowReplyViewModel by viewModels()
    private lateinit var binding: FragmentMyholeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyholeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initRefresh()
    }

    override fun onResume() {
        when (type) {  //更新列表
            GET_HOLE -> viewModel.getMyHole()
            GET_FOLLOW -> viewModel.getMyFollow()
            GET_REPLY -> viewModel.getMyReply()
        }
        super.onResume()
    }
    private fun initView() {
        val adapter = MineRecycleViewAdapter(type, viewModel, this, binding)
        binding.myHoleRecyclerView.apply {
            this.adapter = adapter
            addItemDecoration(SpaceItemDecoration(0, 20))
        }
        viewModel.apply {
            tip.observe(viewLifecycleOwner) {
                it?.let {
                    showMsg(it)
                    viewModel.doneShowingTip()
                }
            }
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    //依据页面的不同提交相应的List
                    val list: StateFlow<List<*>>? = when (type) {
                        GET_HOLE -> myHole
                        GET_FOLLOW -> myFollow
                        GET_REPLY -> myReply
                        else -> null
                    }
                    list?.onEach {
                        finishRefreshAnim()
                    }?.collectLatest {
                        adapter.submitList(it)
                        if (it.isEmpty()) {
                            binding.myHoleRecyclerView.visibility = View.GONE
                            binding.minePlaceholder.visibility = View.VISIBLE
                        } else {
                            binding.myHoleRecyclerView.visibility = View.VISIBLE
                            binding.minePlaceholder.visibility = View.GONE
                        }
                    }
                }
            }
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    //对不同网络状态处理
                    loadingState.collectLatest { state ->
                        when (state) {
                            ApiStatus.SUCCESSFUL,
                            ApiStatus.ERROR -> {
                                finishRefreshAnim()
                            }
                            ApiStatus.LOADING -> {}
                        }
                    }
                }
            }
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    //展示异常时相应的视图
                    showingPlaceholder.collectLatest {
                        it?.let { placeholderType ->
                            showPlaceHolderBy(placeholderType)
                        }
                    }
                }
            }
        }
    }

    private fun initRefresh() {
        binding.refreshLayout.apply {
            setRefreshHeader(StandardRefreshHeader(activity))
            setRefreshFooter(StandardRefreshFooter(activity))
            setEnableLoadMore(true)
            setEnableRefresh(true)
            setOnRefreshListener {
                when (type) {
                    GET_HOLE -> viewModel.getMyHole()
                    GET_FOLLOW -> viewModel.getMyFollow()
                    GET_REPLY -> viewModel.getMyReply()
                }
                binding.myHoleRecyclerView.isEnabled = false
            }
            setOnLoadMoreListener {
                when (type) {
                    GET_HOLE -> viewModel.loadMoreHole()
                    GET_FOLLOW -> viewModel.loadMoreFollow()
                    GET_REPLY -> viewModel.loadMoreReply()
                }
                binding.myHoleRecyclerView.isEnabled = false
            }
        }
    }

    private fun finishRefreshAnim() {
        binding.refreshLayout.finishRefresh() //结束下拉刷新动画
        binding.refreshLayout.finishLoadMore() //结束上拉加载动画
        binding.myHoleRecyclerView.isEnabled = true
    }

    private fun showPlaceHolderBy(placeholderType: HoleFollowReplyViewModel.PlaceholderType) {
        when (placeholderType) {
            HoleFollowReplyViewModel.PlaceholderType.PLACEHOLDER_NETWORK_ERROR -> {
                binding.placeholderHomeNetError.visibility = View.VISIBLE
                binding.placeholderHomeNoContent.visibility = View.GONE
            }

            HoleFollowReplyViewModel.PlaceholderType.PLACEHOLDER_NO_CONTENT -> {
                binding.placeholderHomeNoContent.visibility = View.VISIBLE
                binding.placeholderHomeNetError.visibility = View.GONE
                val tv = requireView().findViewById<TextView>(R.id.tv_no_content)
                tv.text = when (type) {
                    GET_HOLE -> getString(R.string.res_no_myhole)
                    GET_FOLLOW -> getString(R.string.res_no_myfollow)
                    GET_REPLY -> getString(R.string.res_no_myreply)
                    else -> "null"
                }
            }
        }
    }

    /**
     * @description:自定义设置item间距
     */
    class SpaceItemDecoration(
        private val leftRight: Int,
        private val topBottom: Int
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val layoutManager: LinearLayoutManager = parent.layoutManager as LinearLayoutManager
            if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                //最后一项需要 bottom
                if (parent.getChildAdapterPosition(view) == layoutManager.itemCount - 1) {
                    outRect.bottom = topBottom;
                }
                outRect.top = topBottom;
                outRect.left = leftRight;
                outRect.right = leftRight;
            } else {
                //最后一项需要right
                if (parent.getChildAdapterPosition(view) == layoutManager.itemCount - 1) {
                    outRect.right = leftRight;
                }
                outRect.top = topBottom;
                outRect.left = leftRight;
                outRect.bottom = topBottom;
            }
        }
    }

    companion object {
        const val TAG = "MyHoleFollowReplyFragment"

        @JvmStatic
        fun newInstance(type: Int): MyHoleFollowReplyFragment {
            return MyHoleFollowReplyFragment(type)
        }
    }
}

