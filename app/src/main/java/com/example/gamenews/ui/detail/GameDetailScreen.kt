package com.example.gamenews.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.gamenews.R
import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.domain.model.SystemRequirements
import com.example.gamenews.ui.components.EmptyState
import com.example.gamenews.ui.components.ErrorState
import com.example.gamenews.ui.components.LoadingIndicator
import com.example.gamenews.ui.messageRes

@Composable
fun GameDetailRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GameDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    uiState: GameDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? GameDetailUiState.Content)?.details?.title
                            ?: stringResource(R.string.detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                GameDetailUiState.Loading -> LoadingIndicator()

                GameDetailUiState.Empty -> EmptyState(message = stringResource(R.string.detail_empty))

                is GameDetailUiState.Content -> DetailContent(
                    details = uiState.details,
                    isRefreshing = uiState.isRefreshing,
                )

                is GameDetailUiState.Error -> ErrorState(
                    message = stringResource(uiState.cause.messageRes()),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    details: GameDetails,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // A background refresh must not replace content that is already on screen.
        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                AsyncImage(
                    model = details.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = details.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LabelledValue(stringResource(R.string.label_genre), details.genre)
                    LabelledValue(stringResource(R.string.label_platform), details.platform)
                    LabelledValue(stringResource(R.string.label_publisher), details.publisher)
                    LabelledValue(stringResource(R.string.label_developer), details.developer)
                    LabelledValue(stringResource(R.string.label_release_date), details.releaseDate)
                    LabelledValue(stringResource(R.string.label_status), details.status)
                }
            }
            if (details.description.isNotBlank()) {
                item {
                    SectionHeader(stringResource(R.string.detail_about))
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            if (details.screenshots.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(R.string.detail_screenshots))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(items = details.screenshots, key = { it.id }) { screenshot ->
                            AsyncImage(
                                model = screenshot.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(width = 280.dp, height = 158.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
            details.minimumSystemRequirements?.let { requirements ->
                item {
                    SectionHeader(stringResource(R.string.detail_requirements))
                    Requirements(requirements)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun Requirements(requirements: SystemRequirements) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        LabelledValue(stringResource(R.string.req_os), requirements.os)
        LabelledValue(stringResource(R.string.req_processor), requirements.processor)
        LabelledValue(stringResource(R.string.req_memory), requirements.memory)
        LabelledValue(stringResource(R.string.req_graphics), requirements.graphics)
        LabelledValue(stringResource(R.string.req_storage), requirements.storage)
    }
}

/** Absent values are dropped rather than rendered as an empty row. */
@Composable
private fun LabelledValue(label: String, value: String?) {
    if (value.isNullOrBlank()) return

    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
