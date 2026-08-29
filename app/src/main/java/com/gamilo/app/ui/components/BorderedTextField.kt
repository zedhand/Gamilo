package com.gamilo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.MonospaceNumeric

/**
 * Sharp-cornered, hard-bordered text field — no Material floating-label soft outline.
 * [isNumeric] renders the value in [MonospaceNumeric] per the Precision Utility mandate
 * that every currency/mileage/hours figure use a monospaced font for column alignment.
 * [testTag], when given, is applied to the input itself so instrumented tests can target
 * it directly instead of matching on label/placeholder text.
 */
@Composable
fun BorderedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isNumeric: Boolean = false,
    testTag: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = label.uppercase(), color = GamiloColors.TextSecondary, fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = GamiloColors.TextPrimary,
                fontSize = 16.sp,
                fontFamily = if (isNumeric) MonospaceNumeric else FontFamily.Default,
            ),
            keyboardOptions = if (isNumeric) {
                KeyboardOptions(keyboardType = KeyboardType.Decimal)
            } else {
                KeyboardOptions.Default
            },
            cursorBrush = androidx.compose.ui.graphics.SolidColor(GamiloColors.Accent),
            modifier = (if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(GamiloColors.Surface)
                .border(GamiloDimens.BorderWidth, GamiloColors.Border)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
