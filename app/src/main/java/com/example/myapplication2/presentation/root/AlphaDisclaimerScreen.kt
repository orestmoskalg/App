package com.example.myapplication2.presentation.root

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication2.R
import com.example.myapplication2.ui.theme.AppDimens
import com.example.myapplication2.ui.theme.*

@Composable
fun AlphaDisclaimerScreen(
    onAccepted: () -> Unit,
) {
    var checked by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.screenPaddingHorizontal)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(AppDimens.cardCornerRadius),
                color = WarningAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
            ) {
                Row(
                    Modifier.padding(AppDimens.cardInnerPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Science, null, tint = WarningAmber, modifier = Modifier.size(28.dp))
                    Text(
                        stringResource(R.string.alpha_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                stringResource(R.string.alpha_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.alpha_ai_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedLight,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryTeal),
                )
                Text(
                    stringResource(R.string.alpha_checkbox),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAccepted,
                enabled = checked,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.alpha_continue), fontWeight = FontWeight.Bold)
            }
        }
    }
}
