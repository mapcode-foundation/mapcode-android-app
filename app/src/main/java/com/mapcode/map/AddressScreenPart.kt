package com.mapcode.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapcode.R

@Composable
fun AddressArea(
    modifier: Modifier = Modifier,
    address: String,
    onChange: (String) -> Unit,
    helper: AddressHelper,
    error: AddressError
) {
    Column(modifier) {
        ClearableTextField(
            modifier = Modifier.fillMaxWidth(),
            text = address,
            onChange = onChange,
            label = stringResource(R.string.address_bar_label),
            clearButtonContentDescription = stringResource(R.string.clear_address_content_description)
        )

        val extraTextHeight = 20.dp

        //to prevent the layout jumping up and down fill with empty space if no helper or error
        if (helper == AddressHelper.None && error == AddressError.None) {
            Spacer(Modifier.height(extraTextHeight))
        } else {
            AddressHelper(
                Modifier
                    .height(extraTextHeight)
                    .padding(start = 4.dp), helper = helper
            )
            AddressError(
                Modifier
                    .height(extraTextHeight)
                    .padding(start = 4.dp), error = error
            )
        }
    }
}

/**
 * Create the correct components for the helper message state.
 */
@Composable
private fun AddressHelper(modifier: Modifier = Modifier, helper: AddressHelper) {
    val helperMessage = when (helper) {
        AddressHelper.NoInternet -> stringResource(R.string.no_internet_error)
        AddressHelper.NoAddress -> stringResource(R.string.no_address_error)
        is AddressHelper.Location -> helper.location
        AddressHelper.None -> null
    }

    if (helperMessage != null) {
        HelperText(modifier, message = helperMessage)
    }
}

/**
 * Create the correct components for the error message state.
 */
@Composable
private fun AddressError(modifier: Modifier = Modifier, error: AddressError) {
    val errorMessage = when (error) {
        is AddressError.UnknownAddress -> stringResource(R.string.cant_find_address_error, error.addressQuery)
        AddressError.None -> null
    }

    if (errorMessage != null) {
        ErrorText(modifier, message = errorMessage)
    }
}

/**
 * This creates a Text styled as an error.
 */
@Composable
private fun ErrorText(modifier: Modifier = Modifier, message: String) {
    Text(
        modifier = modifier,
        text = message,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Bold
    )
}

/**
 * This creates a Text styled for the helper messages.
 */
@Composable
private fun HelperText(modifier: Modifier = Modifier, message: String) {
    Text(modifier = modifier, text = message, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
}
