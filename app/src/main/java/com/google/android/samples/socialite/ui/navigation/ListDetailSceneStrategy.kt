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

// TODO: Remove this when ListDetailSceneStrategy is merged into Material 3 Navigation 3

@file:Suppress("ktlint")
@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.google.android.samples.socialite.ui.navigation

import androidx.collection.mutableIntListOf
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy

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
 * [ListDetailPaneScaffold]. By using [listPaneMetadata], [detailPaneMetadata], or
 * [extraPaneMetadata] in a NavEntry's metadata, entries can be assigned as belonging to a list
 * pane, detail pane, or extra pane. These panes will be displayed together if the window size is
 * sufficiently large, and will automatically adapt if the window size changes, for example, on a
 * foldable device.
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
    override fun calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.last().metadata[ListDetailRoleKey] == null) return null
        val sceneKey = (entries.last().metadata[ListDetailRoleKey] as PaneMetadata).sceneKey
        val scaffoldEntries = mutableListOf<NavEntry<T>>()
        val scaffoldEntryIndices = mutableIntListOf()
        var detailPlaceholder: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null
        for ((index, entry) in entries.withIndex()) {
            val metadata = entry.metadata[ListDetailRoleKey] as? PaneMetadata
            if (metadata != null && metadata.sceneKey == sceneKey) {
                scaffoldEntryIndices.add(index)
                scaffoldEntries.add(entry)
                if (metadata is ListMetadata) {
                    detailPlaceholder = metadata.detailPlaceholder
                }
            }
        }
        if (scaffoldEntries.isEmpty()) return null
        val scene =
            ListDetailPaneScaffoldScene(
                key = sceneKey,
                onBack = onBack,
                backNavBehavior = backNavigationBehavior,
                directive = directive,
                allEntries = entries,
                scaffoldEntries = scaffoldEntries,
                scaffoldEntryIndices = scaffoldEntryIndices,
                detailPlaceholder = detailPlaceholder ?: {},
            )
        return scene
    }
    internal sealed interface PaneMetadata {
        val sceneKey: Any
    }
    internal class ListMetadata(
        override val sceneKey: Any = Unit,
        val detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit = {}
    ) : PaneMetadata
    internal class DetailMetadata(override val sceneKey: Any = Unit) : PaneMetadata
    internal class ExtraMetadata(override val sceneKey: Any = Unit) : PaneMetadata
    companion object {
        internal val ListDetailRoleKey: String = ListDetailPaneScaffoldRole::class.qualifiedName!!
        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [list pane][ListDetailPaneScaffoldRole.List] within a [ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         * @param detailPlaceholder composable content to display in the detail pane in case there
         *   is no other [NavEntry] representing a detail pane in the backstack. Note that this
         *   content does not receive the same scoping mechanisms as a full-fledged [NavEntry].
         */
        fun listPaneMetadata(
            sceneKey: Any = Unit,
            detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit = {}
        ): Map<String, Any> = mapOf(ListDetailRoleKey to ListMetadata(sceneKey, detailPlaceholder))
        /**
         * Constructs metadata to mark a [NavEntry] as belonging to a
         * [detail pane][ListDetailPaneScaffoldRole.Detail] within a [ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        fun detailPaneMetadata(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to DetailMetadata(sceneKey))
        /**
         * Constructs metadata to mark a [NavEntry] as belonging to an
         * [extra pane][ListDetailPaneScaffoldRole.Extra] within a [ListDetailPaneScaffold].
         *
         * @param sceneKey the key to distinguish the scene of the list-detail scaffold, in case
         *   multiple list-detail scaffolds are supported within the same NavDisplay.
         */
        fun extraPaneMetadata(sceneKey: Any = Unit): Map<String, Any> =
            mapOf(ListDetailRoleKey to ExtraMetadata(sceneKey))
    }
}

