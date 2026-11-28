package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Log in, sign up and password recovery (code by e-mail). */
@Composable
fun AuthScreen(vm: AppViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 log in, 1 sign up, 2 recovery
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.10f), Bg, Bg))),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Logo(size = 22.sp)
            Spacer(Modifier.height(12.dp))
            Body(stringResource(R.string.tagline), color = Muted, align = TextAlign.Center)
            Spacer(Modifier.height(36.dp))

            if (mode != 2) {
                Segmented(
                    listOf(stringResource(R.string.login), stringResource(R.string.register)),
                    mode,
                    { mode = it; vm.clearAuthError() },
                )
                Spacer(Modifier.height(20.dp))
            } else {
                H2(stringResource(R.string.forgot_password), Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            SvField(email, { email = it }, stringResource(R.string.email), keyboard = KeyboardType.Email)

            if (mode == 2) {
                Spacer(Modifier.height(12.dp))
                when (vm.resetStage) {
                    0 -> Body(stringResource(R.string.reset_intro), color = Muted, size = 14.sp)
                    1 -> {
                        Body(stringResource(if (vm.resetMail) R.string.reset_sent else R.string.reset_sent_nomail), color = Muted, size = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        SvField(code, { code = it.filter { c -> c.isDigit() }.take(6) }, stringResource(R.string.reset_code), keyboard = KeyboardType.Number)
                        Spacer(Modifier.height(12.dp))
                        SvField(password, { password = it }, stringResource(R.string.reset_new_password), password = true)
                    }
                    else -> Body(stringResource(R.string.reset_done), color = Green, weight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                SvField(password, { password = it }, stringResource(R.string.password), password = true)
                if (mode == 1) {
                    Spacer(Modifier.height(12.dp))
                    SvField(name, { name = it.take(40) }, stringResource(R.string.display_name))
                } else {
                    Text2(stringResource(R.string.forgot_password), Modifier.align(Alignment.End)) {
                        vm.leaveReset()
                        password = ""
                        mode = 2
                    }
                }
            }

            vm.authError?.let { res ->
                Spacer(Modifier.height(14.dp))
                Body(stringResource(res), color = Red, align = TextAlign.Center)
                val reason = vm.bannedReason
                if (!reason.isNullOrBlank() && res == R.string.err_banned) Body(reason, color = Muted, align = TextAlign.Center)
            }

            Spacer(Modifier.height(22.dp))
            if (mode == 2) {
                when (vm.resetStage) {
                    0 -> SvButton(stringResource(R.string.reset_send), { vm.requestReset(email.trim()) }, Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank(), busy = vm.authBusy)
                    1 -> SvButton(stringResource(R.string.reset_submit), { vm.submitReset(email.trim(), code, password) }, Modifier.fillMaxWidth(),
                        enabled = code.length == 6 && password.length >= 8, busy = vm.authBusy)
                    else -> SvButton(stringResource(R.string.login), {
                        vm.leaveReset()
                        password = ""
                        code = ""
                        mode = 0
                    }, Modifier.fillMaxWidth())
                }
                Text2(stringResource(R.string.reset_back), Modifier.padding(top = 8.dp)) {
                    vm.leaveReset()
                    mode = 0
                }
            } else {
                SvButton(
                    stringResource(if (mode == 1) R.string.register else R.string.login),
                    { if (mode == 1) vm.register(email.trim(), password, name.trim()) else vm.login(email.trim(), password) },
                    Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && password.isNotEmpty(),
                    busy = vm.authBusy,
                )
                Spacer(Modifier.height(24.dp))
                SocialLoginButtons(vm)
            }
            Spacer(Modifier.height(32.dp))
            Caption(stringResource(R.string.version, BuildConfig.VERSION_NAME), maxLines = 1)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Text2(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Body(text, modifier.clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp), color = Cyan, size = 14.sp, weight = FontWeight.SemiBold)
}

fun providerTitle(provider: String): String = when (provider) {
    "google" -> "Google"
    "yandex" -> "Яндекс ID"
    "vk" -> "VK ID"
    else -> provider
}

/** Buttons for Google / Yandex / VK sign-in (shown only for providers configured on the server). */
@Composable
fun SocialLoginButtons(vm: AppViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.loadProviders() }
    if (vm.providers.isEmpty() && vm.pausedProviders.isEmpty()) return
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Stroke))
            Caption(stringResource(R.string.or_sign_in_with), Modifier.padding(horizontal = 12.dp), maxLines = 1)
            Box(Modifier.weight(1f).height(1.dp).background(Stroke))
        }
        Spacer(Modifier.height(14.dp))
        vm.providers.forEach { p ->
            SvButton(providerTitle(p), { vm.startOauth(ctx, p) }, Modifier.fillMaxWidth().padding(bottom = 10.dp), kind = BtnKind.Secondary)
        }
        vm.pausedProviders.forEach { p -> ProviderOffNote(p, Modifier.padding(bottom = 10.dp)) }
    }
}

/** "Вход через Яндекс ещё не включён на сервере." — like the Steam import note. */
@Composable
fun ProviderOffNote(provider: String, modifier: Modifier = Modifier) {
    SvCard(modifier.fillMaxWidth(), border = Amber.copy(alpha = 0.6f), padding = 14.dp) {
        Body(stringResource(R.string.provider_off, providerTitle(provider)), color = Amber, size = 14.sp)
    }
}
