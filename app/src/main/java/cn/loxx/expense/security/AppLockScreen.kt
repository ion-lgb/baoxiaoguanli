package cn.loxx.expense.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cn.loxx.expense.ui.theme.GlassCard

private val allowedAuthenticators =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var status by remember { mutableStateOf(LockStatus.PROMPTING) }

    fun authenticate() {
        val fragmentActivity = activity ?: run { status = LockStatus.UNAVAILABLE; return }
        val manager = BiometricManager.from(context)
        if (manager.canAuthenticate(allowedAuthenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            status = LockStatus.NO_CREDENTIAL
            return
        }
        status = LockStatus.PROMPTING
        val prompt = BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockSession.unlocked = true
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        status = LockStatus.PROMPTING
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("报销管理已锁定")
            .setSubtitle("验证指纹、人脸或锁屏密码以继续")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { authenticate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(28.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "应用已锁定",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (status) {
                        LockStatus.NO_CREDENTIAL ->
                            "未检测到可用的指纹或锁屏密码，请先在系统设置中配置，或暂时进入应用后在设置中关闭应用锁"
                        else -> "验证通过后即可继续使用"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                when (status) {
                    LockStatus.NO_CREDENTIAL -> Button(onClick = {
                        AppLockSession.unlocked = true
                        onUnlocked()
                    }) { Text("暂时进入") }
                    else -> Button(onClick = { authenticate() }) { Text("重新验证") }
                }
            }
        }
    }
}

private enum class LockStatus { PROMPTING, NO_CREDENTIAL, UNAVAILABLE }
