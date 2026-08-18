package io.github.darriousliu.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.logic.NetworkRepo
import io.github.darriousliu.han1meviewer.core.common.exception.LocalizedStateException
import io.github.darriousliu.han1meviewer.core.model.UserAccount
import io.github.darriousliu.han1meviewer.core.model.UserAccountAction
import io.github.darriousliu.han1meviewer.core.model.UserAccountActionEvent
import io.github.darriousliu.han1meviewer.core.model.UserAccountSubmittingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.not_logged_in_currently
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserAccountViewModel : ViewModel() {

    private val _accountState = MutableStateFlow<WebsiteState<UserAccount>>(WebsiteState.Loading)
    val accountState = _accountState.asStateFlow()

    private val _actionFlow = MutableSharedFlow<UserAccountActionEvent>()
    val actionFlow = _actionFlow.asSharedFlow()

    private val _submittingState = MutableStateFlow(UserAccountSubmittingState.Idle)
    val submittingState = _submittingState.asStateFlow()

    fun loadAccount(forceReload: Boolean = false) {
        if (!forceReload && _accountState.value is WebsiteState.Success) return
        val userId = Preferences.savedUserId
        if (userId.isBlank()) {
            _accountState.value = WebsiteState.Error(
                LocalizedStateException(Res.string.not_logged_in_currently)
            )
            return
        }
        viewModelScope.launch {
            _accountState.value = WebsiteState.Loading
            NetworkRepo.getUserAccountPage(userId).collect { state ->
                _accountState.value = state
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        val account = (_accountState.value as? WebsiteState.Success)?.info ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingProfile
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.ProfileUpdated, WebsiteState.Loading))
            NetworkRepo.updateUserAccountProfile(
                userId = account.userId,
                csrfToken = account.csrfToken,
                name = name,
                email = email,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.ProfileUpdated, state))
                if (state is WebsiteState.Success) {
                    loadAccount(forceReload = true)
                }
                if (state !is WebsiteState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }

    fun updatePassword(oldPassword: String, newPassword: String, newPasswordConfirm: String) {
        val account = (_accountState.value as? WebsiteState.Success)?.info ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingPassword
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.PasswordUpdated, WebsiteState.Loading))
            NetworkRepo.updateUserAccountPassword(
                userId = account.userId,
                csrfToken = account.csrfToken,
                oldPassword = oldPassword,
                newPassword = newPassword,
                newPasswordConfirm = newPasswordConfirm,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.PasswordUpdated, state))
                if (state !is WebsiteState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }

    fun updateAvatar(photoBytes: ByteArray, fileName: String) {
        val account = (_accountState.value as? WebsiteState.Success)?.info ?: return
        if (_submittingState.value != UserAccountSubmittingState.Idle) return
        viewModelScope.launch {
            _submittingState.value = UserAccountSubmittingState.UpdatingAvatar
            _actionFlow.emit(UserAccountActionEvent(UserAccountAction.AvatarUpdated, WebsiteState.Loading))
            NetworkRepo.updateUserAccountAvatar(
                userId = account.userId,
                csrfToken = account.csrfToken,
                photoBytes = photoBytes,
                fileName = fileName,
            ).collect { state ->
                _actionFlow.emit(UserAccountActionEvent(UserAccountAction.AvatarUpdated, state))
                if (state is WebsiteState.Success) {
                    loadAccount(forceReload = true)
                }
                if (state !is WebsiteState.Loading) {
                    _submittingState.value = UserAccountSubmittingState.Idle
                }
            }
        }
    }
}
