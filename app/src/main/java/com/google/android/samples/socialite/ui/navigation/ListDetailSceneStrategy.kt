/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO: Remove this when ListDetailSceneStrategy is merged into Navigation 3

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.google.android.samples.socialite.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.NavEntry
import androidx.navigation3.Scene
import androidx.navigation3.SceneStrategy
import androidx.navigation3.SceneStrategyResult
import kotlinx.coroutines.CancellationException

/**
 * Creates and remembers a [ListDetailPaneScaffoldSceneStrategy].
 *
 * @param onBack a callback for handling system back press. The passed [Int] refers to the number of
 *   entries to pop from the end of the backstack, as calculated by the
 *   [ListDetailPaneScaffoldSceneStrategy].
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 */
@Composable
fun <T : Any> rememberListDetailSceneStrategy(
    onBack: (Int) -> Unit,
    backNavigationBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilCurrentDestinationChange,
    directive: PaneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()),
): ListDetailPaneScaffoldSceneStrategy<T> {
    return remember(onBack, backNavigationBehavior, directive) {
        ListDetailPaneScaffoldSceneStrategy(
            onBack = onBack,
            backNavigationBehavior = backNavigationBehavior,
            directive = directive,
        )
    }
}

/**
 * A [ListDetailPaneScaffoldSceneStrategy] supports arranging [NavEntry]s into an adaptive
 * [ListDetailPaneScaffold]. By using [paneRole] in a NavEntry's metadata, entries can be assigned
 * as belonging to a list pane, detail pane, or extra pane. These panes will be displayed together
 * if the window size is sufficiently large, and will automatically adapt if the window size
 * changes, for example, on a foldable device.
 *
 * @param onBack a callback for handling system back press. The passed [Int] refers to the number of
 *   entries to pop from the end of the backstack, as calculated by the
 *   [ListDetailPaneScaffoldSceneStrategy].
 * @param backNavigationBehavior the behavior describing which backstack entries may be skipped
 *   during the back navigation. See [BackNavigationBehavior].
 * @param directive The top-level directives about how the list-detail scaffold should arrange its
 *   panes.
 */
class ListDetailPaneScaffoldSceneStrategy<T : Any>(
    val onBack: (Int) -> Unit,
    val backNavigationBehavior: BackNavigationBehavior,
    val directive: PaneScaffoldDirective,
) : SceneStrategy<T> {
    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>): SceneStrategyResult<T>? {
        if (entries.last().metadata[ListDetailRoleKey] == null) return null

        val scaffoldEntries = mutableListOf<NavEntry<T>>()
        val scaffoldEntryIndices = mutableListOf<Int>()

        for ((index, entry) in entries.withIndex()) {
            if (entry.metadata[ListDetailRoleKey] is ThreePaneScaffoldRole) {
                scaffoldEntryIndices.add(index)
                scaffoldEntries.add(entry)
            }
        }

        if (scaffoldEntries.isEmpty()) return null

        val scene =
            ListDetailPaneScaffoldScene(
                key = scaffoldEntries.first().key,
                onBack = onBack,
                backNavBehavior = backNavigationBehavior,
                directive = directive,
                allEntries = entries,
                scaffoldEntries = scaffoldEntries,
                scaffoldEntryIndices = scaffoldEntryIndices,
                // TODO: bubble this up in the API
                detailPlaceholder = { Text("Detail Placeholder") },
            )

        return SceneStrategyResult(
            scene = scene,
            previousEntries = scene.onBackResult.previousEntries,
        )
    }

    companion object {
        internal val ListDetailRoleKey: String = ListDetailPaneScaffoldRole::class.qualifiedName!!

        fun paneRole(role: ThreePaneScaffoldRole) = mapOf(ListDetailRoleKey to role)
    }
}

internal class ListDetailPaneScaffoldScene<T : Any>(
    override val key: T,
    val onBack: (Int) -> Unit,
    val backNavBehavior: BackNavigationBehavior,
    val directive: PaneScaffoldDirective,
    /** All backstack entries, including those not relevant to the list-detail scaffold scene. */
    val allEntries: List<NavEntry<T>>,
    /** The entries in the backstack that are handled by this list-detail scaffold scene. */
    val scaffoldEntries: List<NavEntry<T>>,
    /** The indices of [allEntries] that result in [scaffoldEntries]. */
    val scaffoldEntryIndices: List<Int>,
    val detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit,
) : Scene<T> {
    override val entries: List<NavEntry<T>>
        get() = scaffoldEntries

    private val entriesAsNavItems: List<ThreePaneScaffoldDestinationItem<T>> =
        entries.map { it.toNavItem()!! }

    class OnBackResult<T : Any>(
        /**
         * The previous scaffold value of the list-detail scaffold once a back event is handled.
         *
         * If this value is null, it means that either:
         * - there is no previous NavEntry in the backstack, or
         * - the back event leaves the list-detail scaffold scene and is therefore not handled
         *   internally.
         */
        val previousScaffoldValue: ThreePaneScaffoldValue?,

        /** The resulting backstack after the back event is handled. */
        val previousEntries: List<NavEntry<T>>,
    )

    val onBackResult: OnBackResult<T> = calculateOnBackResult()

    private fun calculateOnBackResult(): OnBackResult<T> {
        // index relative to `scaffoldEntries`
        val prevDestRelativeIndex = getPreviousDestinationIndex()

        // index relative to `allEntries`
        val prevDestAbsoluteIndex =
            if (prevDestRelativeIndex < 0) {
                scaffoldEntryIndices.first() - 1
            } else {
                scaffoldEntryIndices[prevDestRelativeIndex]
            }

        val scaffoldEntryIndicesSet = scaffoldEntryIndices.toSet()

        for (index in allEntries.lastIndex downTo 0) {
            if (index !in scaffoldEntryIndicesSet) {
                // Back event leaves the scaffold
                return OnBackResult(
                    previousScaffoldValue = null,
                    previousEntries = allEntries.subList(0, index + 1).toList(),
                )
            }
            if (index == prevDestAbsoluteIndex) {
                // Back event stays within the scaffold -- handled internally
                val previousScaffoldValue =
                    calculateScaffoldValue(
                        destinationHistory = entriesAsNavItems.subList(0, prevDestRelativeIndex + 1),
                    )
                return OnBackResult(
                    previousScaffoldValue = previousScaffoldValue,
                    previousEntries = allEntries.subList(0, index + 1).toList(),
                )
            }
        }

        // No previous entry in backstack
        return OnBackResult(
            previousScaffoldValue = null,
            previousEntries = emptyList(),
        )
    }

    private fun getPreviousDestinationIndex(): Int {
        if (entriesAsNavItems.size <= 1) {
            // No previous destination
            return -1
        }
        val currentDestination = entriesAsNavItems.last()
        val currentScaffoldValue: ThreePaneScaffoldValue =
            calculateScaffoldValue(destinationHistory = entriesAsNavItems)

        when (backNavBehavior) {
            BackNavigationBehavior.PopLatest -> return entriesAsNavItems.lastIndex - 1
            BackNavigationBehavior.PopUntilScaffoldValueChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val previousValue =
                        calculateScaffoldValue(
                            destinationHistory =
                            entriesAsNavItems.subList(0, previousDestinationIndex + 1),
                        )
                    if (previousValue != currentScaffoldValue) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilCurrentDestinationChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val destination = entriesAsNavItems[previousDestinationIndex].pane
                    if (destination != currentDestination.pane) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilContentChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val contentKey = entriesAsNavItems[previousDestinationIndex].contentKey
                    if (contentKey != currentDestination.contentKey) {
                        return previousDestinationIndex
                    }
                    // A scaffold value change also counts as a content change.
                    val previousValue =
                        calculateScaffoldValue(
                            destinationHistory =
                            entriesAsNavItems.subList(0, previousDestinationIndex + 1),
                        )
                    if (previousValue != currentScaffoldValue) {
                        return previousDestinationIndex
                    }
                }
        }

        return -1
    }

    private fun calculateScaffoldValue(
        destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>,
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            maxHorizontalPartitions = directive.maxHorizontalPartitions,
            /*maxVerticalPartitions = directive.maxVerticalPartitions,*/
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            destinationHistory = destinationHistory,
        )

    override val content: @Composable () -> Unit = {
        val scaffoldValue = calculateScaffoldValue(destinationHistory = entriesAsNavItems)
        val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }
        LaunchedEffect(scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }

        val previousScaffoldValue = onBackResult.previousScaffoldValue
        PredictiveBackHandler(enabled = previousScaffoldValue != null) { progress ->
            try {
                progress.collect { backEvent ->
                    scaffoldState.seekTo(
                        fraction =
                        backProgressToStateProgress(
                            progress = backEvent.progress,
                            scaffoldValue = scaffoldValue,
                        ),
                        targetState = previousScaffoldValue!!,
                    )
                }
                onBack(allEntries.size - onBackResult.previousEntries.size)
            } catch (_: CancellationException) {
                scaffoldState.animateTo(targetState = scaffoldValue)
            }
        }

        val lastList =
            entries.findLast {
                it.metadata[ListDetailPaneScaffoldSceneStrategy.ListDetailRoleKey] ==
                    ListDetailPaneScaffoldRole.List
            }
        val lastDetail =
            entries.findLast {
                it.metadata[ListDetailPaneScaffoldSceneStrategy.ListDetailRoleKey] ==
                    ListDetailPaneScaffoldRole.Detail
            }
        val lastExtra =
            entries.findLast {
                it.metadata[ListDetailPaneScaffoldSceneStrategy.ListDetailRoleKey] ==
                    ListDetailPaneScaffoldRole.Extra
            }

        ListDetailPaneScaffold(
            directive = directive,
            scaffoldState = scaffoldState,
            listPane =
            lastList?.content?.let {
                {
                    // TODO: allow customizing AnimatedPane params
                    AnimatedPane { it.invoke(lastList.key) }
                }
            } ?: {},
            detailPane =
            lastDetail?.content?.let { { AnimatedPane { it.invoke(lastDetail.key) } } }
                ?: detailPlaceholder,
            extraPane = lastExtra?.content?.let { { AnimatedPane { it.invoke(lastExtra.key) } } },
            // TODO: drag handle/pane expansion state
        )
    }
}

private fun <T : Any> NavEntry<T>.toNavItem(): ThreePaneScaffoldDestinationItem<T>? {
    val role =
        this.metadata[ListDetailPaneScaffoldSceneStrategy.ListDetailRoleKey]
            as? ThreePaneScaffoldRole ?: return null
    return ThreePaneScaffoldDestinationItem(
        pane = role,
        contentKey = this.key,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun backProgressToStateProgress(
    progress: Float,
    scaffoldValue: ThreePaneScaffoldValue,
): Float =
    THREE_PANE_SCAFFOLD_PREDICTIVE_BACK_EASING.transform(progress) *
        when (scaffoldValue.expandedCount) {
            1 -> SINGLE_PANE_PROGRESS_RATIO
            2 -> DUAL_PANE_PROGRESS_RATIO
            else -> TRIPLE_PANE_PROGRESS_RATIO
        }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ThreePaneScaffoldValue.expandedCount: Int
    get() {
        var count = 0
        if (primary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (secondary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (tertiary == PaneAdaptedValue.Expanded) {
            count++
        }
        return count
    }

private val THREE_PANE_SCAFFOLD_PREDICTIVE_BACK_EASING: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private const val SINGLE_PANE_PROGRESS_RATIO: Float = 0.1f
private const val DUAL_PANE_PROGRESS_RATIO: Float = 0.15f
private const val TRIPLE_PANE_PROGRESS_RATIO: Float = 0.2f
