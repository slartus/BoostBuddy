package ru.slartus.boostbuddy.components.filter.tags

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.filter.FilterScreenEntryPoint
import ru.slartus.boostbuddy.components.filter.Tag
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.TagRepository

@Stable
interface FilterTagsComponent {
    val viewStates: Value<FilterTagsViewState>

    fun onTagSelect(tagModel: TagItem.TagModel)
    fun onRepeatClick()
}

class FilterTagsComponentImpl(
    componentContext: ComponentContext,
    selectedTags: List<Tag>,
    private val entryPoint: FilterScreenEntryPoint,
    private val onTagsChange: (tags: List<Tag>) -> Unit,
) : BaseComponent<FilterTagsViewState, Unit>(
    componentContext,
    FilterTagsViewState(
        tags = selectedTags.map { TagItem.TagModel(it, true) }.toImmutableList()
    )
), FilterTagsComponent {
    private val tagRepository by Inject.lazy<TagRepository>()

    init {
        fetchTags()
    }

    private fun fetchTags() {
        scope.launch {
            setLoading()
            val result = when (entryPoint) {
                is FilterScreenEntryPoint.Blog -> tagRepository.getBlogTags(entryPoint.blog.blogUrl)
                FilterScreenEntryPoint.Feed -> tagRepository.getFeedTags(limit = 100, offset = null)
            }
            if (result.isSuccess) {
                val tags = result.getOrThrow()
                viewState = viewState.copy(
                    tags = (
                            viewState.tags.filterIsInstance<TagItem.TagModel>() +
                                    tags.data.searchTags
                                        .map {
                                            TagItem.TagModel(
                                                Tag(it.tag.id, it.tag.title),
                                                selected = false
                                            )
                                        }
                            )
                        .distinctBy { it.tag.title }
                        .toImmutableList(),
                    extra = tags.extra,
                )
            } else {
                setError()
            }
        }
    }

    override fun onTagSelect(tagModel: TagItem.TagModel) {
        viewState = viewState.copy(
            tags = viewState.tags
                .map { item ->
                    when (item) {
                        TagItem.Loading, TagItem.Error -> item
                        is TagItem.TagModel ->
                            if (item.tag.id == tagModel.tag.id) {
                                item.copy(selected = !item.selected)
                            } else {
                                item
                            }
                    }
                }
                .toImmutableList()
        )
        onTagsChange(
            viewState.tags
                .filterIsInstance<TagItem.TagModel>()
                .filter { it.selected }
                .map { it.tag }
        )
    }

    override fun onRepeatClick() {
        fetchTags()
    }

    private fun setLoading() {
        viewState = viewState.copy(
            tags = (viewState.tags.filterIsInstance<TagItem.TagModel>() + TagItem.Loading).toImmutableList()
        )
    }

    private fun setError() {
        viewState = viewState.copy(
            tags = (viewState.tags.filterIsInstance<TagItem.TagModel>() + TagItem.Error).toImmutableList()
        )
    }

    private companion object {
        fun List<TagItem.TagModel>.prepareTagsList(): List<TagItem.TagModel> =
            distinctBy { it.tag.title }
                .sortedWith(
                    compareByDescending<TagItem.TagModel> { it.selected }
                        .thenBy { it.tag.title }
                )
    }
}