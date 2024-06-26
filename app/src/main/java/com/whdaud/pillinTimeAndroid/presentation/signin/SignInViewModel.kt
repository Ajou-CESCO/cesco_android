package com.whdaud.pillinTimeAndroid.presentation.signin

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.whdaud.pillinTimeAndroid.data.local.LocalUserDataSource
import com.whdaud.pillinTimeAndroid.data.remote.dto.FCMTokenDTO
import com.whdaud.pillinTimeAndroid.data.remote.dto.request.SignInRequest
import com.whdaud.pillinTimeAndroid.data.remote.dto.request.SignInSmsAuthRequest
import com.whdaud.pillinTimeAndroid.domain.repository.FcmRepository
import com.whdaud.pillinTimeAndroid.domain.repository.SignInRepository
import com.whdaud.pillinTimeAndroid.presentation.common.InputType
import com.whdaud.pillinTimeAndroid.presentation.signin.components.SignInPageList
import com.whdaud.pillinTimeAndroid.presentation.signin.components.signInPages
import com.whdaud.pillinTimeAndroid.util.PhoneVisualTransformation
import com.whdaud.pillinTimeAndroid.util.SsnVisualTransformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInRepository: SignInRepository,
    private val localUserDataSource: LocalUserDataSource,
    private val fcmRepository: FcmRepository
) : ViewModel() {
    private var phone = mutableStateOf("")
    private var otp = mutableStateOf("")
    var name = mutableStateOf("")
    private var ssn = mutableStateOf("")
    private var currentPageIndex = mutableIntStateOf(0)
    private var smsAuthCode = mutableStateOf("")
    private val _isLoading = MutableStateFlow(false)
    var isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun signIn(navController: NavController) {
        val formattedPhone = "${phone.value.substring(0, 3)}-${
            phone.value.substring(3, 7)
        }-${phone.value.substring(7, 11)}"
        val formattedSsn = "${ssn.value.substring(0, 6)}-${ssn.value.substring(6)}"

        viewModelScope.launch {
            val signInRequest = SignInRequest(name.value, formattedPhone, formattedSsn)
            val result = signInRepository.signIn(signInRequest)
            result.onSuccess { authenticateResponse ->
                Log.e("login", "succeed to call api ${authenticateResponse.message}")
                localUserDataSource.saveAccessToken(authenticateResponse.result.accessToken)
                localUserDataSource.saveUserName(name.value)
                _isLoading.value = true
                postFcmToken()
                delay(3000)
                navController.navigate("bottomNavigatorScreen") {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            }.onFailure {
                Log.e("login", "failed to call api ${it.message}")
                localUserDataSource.saveUserName(name.value)
                localUserDataSource.saveUserPhone(formattedPhone)
                localUserDataSource.saveUserSsn(formattedSsn)
                delay(1000)
                navController.navigate("roleSelectScreen")
            }
        }
    }

    fun postSmsAuth() {
        viewModelScope.launch {
            val result = signInRepository.postSmsAuth(SignInSmsAuthRequest(phone.value))
            result.onSuccess { codeResponse ->
                smsAuthCode.value = codeResponse.result.code
            }.onFailure {
                Log.e("SMS", "failed to call api ${it.message}")
            }
        }
    }
    private fun postFcmToken() {
        viewModelScope.launch {
            val token = Firebase.messaging.token.await()
            Log.e("FCM token:", token)
            val result = fcmRepository.postFcmToken(FCMTokenDTO(token))
            result.onSuccess {
                Log.e("SignInViewModel FCM Token", "succeeded to post FCM Token: ${it.status}")
            }.onFailure {
                Log.e("SignInViewModel FCM Token", "failed to post FCM Token: ${it.cause}")
            }
        }
    }

    fun getCurrentPage(): SignInPageList {
        return signInPages.getOrElse(currentPageIndex.intValue) { signInPages[0] }
    }

    fun getCurrentPageTitle(): String {
        val page = getCurrentPage()
        return page.title.replace("{name}", name.value)
    }

    fun getCurrentInput(): String {
        return when (currentPageIndex.intValue) {
            0 -> phone.value
            1 -> otp.value
            2 -> name.value
            3 -> ssn.value
            else -> ""
        }
    }

    fun getInputType(): InputType {
        return when (currentPageIndex.intValue) {
            0 -> InputType.PHONE
            1 -> InputType.OTP
            2 -> InputType.NAME
            3 -> InputType.SSN
            else -> InputType.PLAIN
        }
    }

    fun getVisualTransformations(): (AnnotatedString) -> TransformedText {
        val transformation = when (currentPageIndex.intValue) {
            0 -> PhoneVisualTransformation()
            3 -> SsnVisualTransformation()
            else -> VisualTransformation.None
        }
        return { input -> transformation.filter(input) }
    }

    fun updateInput(input: String) {
        when (currentPageIndex.intValue) {
            0 -> phone.value = input.filter { it.isDigit() }
            1 -> otp.value = input
            2 -> name.value = input
            3 -> ssn.value = input.filter { it.isDigit() }
        }
    }

    fun isValidateInput(): Boolean {
        val ssnRegex = Regex("^[0-9]{6}-?[1-4]$")
        return when (currentPageIndex.intValue) {
            0 -> phone.value.matches(Regex("^01[0-1,7]-?[0-9]{4}-?[0-9]{4}$")) || phone.value.isEmpty()
            1 -> otp.value == smsAuthCode.value || otp.value.isEmpty()
//            1 -> true
            2 -> name.value.matches(Regex("^[가-힣a-zA-Z]{1,}$")) || name.value.isEmpty()
            3 -> ssn.value.matches(ssnRegex) || ssn.value.isEmpty()
            else -> false
        }
    }

    fun nextPage() {
        if (currentPageIndex.intValue < signInPages.size - 1) {
            currentPageIndex.intValue++
        }
    }

    fun previousPage() {
        if (currentPageIndex.intValue > 0) {
            currentPageIndex.intValue--
        }
    }
}