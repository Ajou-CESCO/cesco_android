package com.whdaud.pillinTimeAndroid.presentation.mypage.withdrawal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.whdaud.pillinTimeAndroid.presentation.Dimens.BasicPadding
import com.whdaud.pillinTimeAndroid.presentation.common.ButtonColor
import com.whdaud.pillinTimeAndroid.presentation.common.ButtonSize
import com.whdaud.pillinTimeAndroid.presentation.common.CustomAlertDialog
import com.whdaud.pillinTimeAndroid.presentation.common.CustomButton
import com.whdaud.pillinTimeAndroid.presentation.common.CustomTopBar
import com.whdaud.pillinTimeAndroid.ui.theme.Gray100
import com.whdaud.pillinTimeAndroid.ui.theme.Gray70
import com.whdaud.pillinTimeAndroid.ui.theme.PillinTimeTheme
import com.whdaud.pillinTimeAndroid.ui.theme.White

@Composable
fun WithdrawalScreen(
    viewModel: WithdrawalViewModel = hiltViewModel(),
    navController: NavController
) {
    val showDialog = remember { mutableStateOf(false) }
    val bullet = "\u2022"
    Column(
        modifier = Modifier
            .background(White)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CustomTopBar(
            onBackClicked = {
                navController.popBackStack()
            },
            showBackButton = true,
            title = "회원 탈퇴"
        )
        Spacer(modifier = Modifier.weight(.2f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = BasicPadding)
        ) {
            Text(
                modifier = Modifier.align(alignment = Alignment.Start),
                text = "정말 탈퇴하시겠아요?",
                color = Gray100,
                style = PillinTimeTheme.typography.logo2Extra
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "계정을 삭제하는 경우\n ${bullet} 보호자와의 케어 내역이 영구적으로 삭제되며\n ${bullet} 동일한 사용 이름으로 가입하거나 해당 사용자\n이름을 다른 계정에 추가할 수 없습니다.\n\n약속시간을 사용하신 경험이 도움이 되셨길 바랍니다.\n감사합니다.",
                color = Gray70,
                style = PillinTimeTheme.typography.body2Regular
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        CustomButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BasicPadding),
            enabled = true,
            filled = ButtonColor.BLANK,
            size = ButtonSize.MEDIUM,
            text = "탈퇴하기",
            onClick = { showDialog.value = true }
        )
    }
    if (showDialog.value) {
        CustomAlertDialog(
            title = "회원 탈퇴",
            description = "회원 탈퇴하시겠습니까?",
            confirmText = "탈퇴할래요",
            dismissText = "취소할래요",
            onConfirm = { viewModel.deleteUserInfo(navController) },
            onDismiss = { showDialog.value = false }
        )
    }
}