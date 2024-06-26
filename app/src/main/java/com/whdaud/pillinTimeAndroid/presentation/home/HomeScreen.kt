package com.whdaud.pillinTimeAndroid.presentation.home

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.whdaud.pillinTimeAndroid.R
import com.whdaud.pillinTimeAndroid.domain.entity.HomeUser
import com.whdaud.pillinTimeAndroid.presentation.Dimens
import com.whdaud.pillinTimeAndroid.presentation.common.ClientListBar
import com.whdaud.pillinTimeAndroid.presentation.common.CustomLottieView
import com.whdaud.pillinTimeAndroid.presentation.common.CustomSnackBar
import com.whdaud.pillinTimeAndroid.presentation.common.ManagerEmptyView
import com.whdaud.pillinTimeAndroid.presentation.home.components.HomeDetailPage
import com.whdaud.pillinTimeAndroid.presentation.main.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavController
) {
    val userDetails by mainViewModel.userDetails.collectAsState()
    val userDoseLog by mainViewModel.userDoseLog.collectAsState()
    val relationInfoList by mainViewModel.relationInfoList.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    val relationUserNames = relationInfoList.map { it.memberName }
    val remoteHealthData by viewModel.remoteHealthData.collectAsState()
    val managerRequest by viewModel.managerRequest.collectAsState()

    val pagerState = rememberPagerState(
        pageCount = {
            if (userDetails?.isManager == true) {
                relationInfoList.size
            } else 1
        }
    )
    val scope = rememberCoroutineScope()
    var selectedUserIndex by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    val permissionsLauncher =
        if (userDetails?.isManager == false)
            rememberLauncherForActivityResult(viewModel.permissionsLauncher) { hasPermissions = true }
        else null
    val snackbarHostState = remember { SnackbarHostState() }
    val snackMessage = remember { mutableStateOf("") }

    // 피보호자일때만 권한 체크
    LaunchedEffect(userDetails?.isManager == false) {
        permissionsLauncher?.let {
            it.launch(viewModel.permissions)
            Log.d("permission", "permission launched")
        }
    }

    val homeUser = if (userDetails?.isManager == true) {
        if(relationInfoList.isNotEmpty()) {
            HomeUser(
                memberId = relationInfoList[selectedUserIndex].memberId,
                name = relationInfoList[selectedUserIndex].memberName,
                cabinetId = relationInfoList[selectedUserIndex].cabinetId,
                isManager = true
            )
        } else {
            null
        }
    } else {
        userDetails?.let {
            HomeUser(
                memberId = it.memberId,
                name = it.name,
                cabinetId = it.cabinetId,
                isManager = it.isManager
            )
        }
    }

    LaunchedEffect(homeUser) {
        homeUser?.memberId.let {
            if (it != null) {
                mainViewModel.getUserDoseLog(it)
                viewModel.getRemoteHealthData(it)
                Log.e("homeScreen health", "$it, ${homeUser?.isManager}")
            }
        }
    }

    if(isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CustomLottieView(
                modifier = Modifier.size(200.dp),
                lottieAnim = R.raw.dots_loading
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (userDetails?.isManager == true) {
                ClientListBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    profiles = relationUserNames,
                    selectedIndex = selectedUserIndex,
                    onProfileSelected = { index ->
                        selectedUserIndex = index
                        scope.launch {
                            pagerState.scrollToPage(selectedUserIndex)
                        }
                    }
                )
            }
            if (userDetails?.isManager == true && relationInfoList.isEmpty()) {
                ManagerEmptyView(navController)
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HomeDetailPage(
                        modifier = Modifier.zIndex(-1f),
                        navController = navController,
                        userDetail = homeUser,
                        onPullRefresh = {
                            scope.launch {
                                isRefreshing = true
                                homeUser?.memberId?.let { it1 -> viewModel.getRemoteHealthData(it1) }
                                if (userDetails?.isManager == true) {
                                    mainViewModel.getUserDoseLog(relationInfoList[selectedUserIndex].memberId)
                                    mainViewModel.getRelationship()
                                } else {
                                    userDetails?.memberId?.let { mainViewModel.getUserDoseLog(it) }
                                }
                                mainViewModel.getInitData()
                                viewModel.getManagerRequest()
                                delay(1000)
                                isRefreshing = false
                            }
                        },

                        onHealthRefresh = {
                            homeUser?.memberId?.let { it1 -> viewModel.postLocalHealthData(it1) }
                        },
                        isRefreshing = isRefreshing,
                        userDoseLog = userDoseLog,
                        healthData = remoteHealthData,
                        managerRequest = managerRequest
                    ) { requestId, managerName ->
                        if (managerName != null) {
                            viewModel.acceptManagerRequest(requestId, managerName) { message ->
                                if (message != null) {
                                    scope.launch {
                                        snackMessage.value = message
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = Dimens.BasicPadding)
                .fillMaxSize()
                .zIndex(2f),
            contentAlignment = Alignment.BottomCenter
        ) {
            CustomSnackBar(
                snackbarHostState = snackbarHostState,
                message = snackMessage.value
            )
        }
    }
    LaunchedEffect(userDetails) { if (userDetails != null) { isLoading = false } }
    LaunchedEffect(pagerState.currentPage) { selectedUserIndex = pagerState.currentPage }
    LaunchedEffect(userDetails?.isManager) { mainViewModel.getRelationship() }
    // when refreshed, there is no relation
    LaunchedEffect(relationInfoList, userDetails) {
        if (relationInfoList.isEmpty() && userDetails?.isManager == false) {
            navController.navigate("signupClientScreen") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }
}