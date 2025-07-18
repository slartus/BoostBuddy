package ru.slartus.boostbuddy.components.filter.tags

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.filter.FilterScreenEntryPoint
import ru.slartus.boostbuddy.components.filter.Tag
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.TagRepository
import ru.slartus.boostbuddy.data.repositories.models.Tags

@Stable
interface FilterTagsComponent {
    val viewStates: Value<FilterTagsViewState>

    fun onTagSelect(tagModel: TagItem.TagModel)
    fun onRepeatClick()
    fun loadNextPage()
}

class FilterTagsComponentImpl(
    componentContext: ComponentContext,
    selectedTags: List<Tag>,
    private val entryPoint: FilterScreenEntryPoint,
    private val onTagsChange: (tags: List<Tag>) -> Unit,
) : BaseComponent<FilterTagsViewState, Unit>(
    componentContext,
    FilterTagsViewState(
        tags = selectedTags.toTagModels(selected = true).toImmutableList()
    )
), FilterTagsComponent {
    private val tagRepository by Inject.lazy<TagRepository>()
    private val loadingItem = TagItem.Loading
    private val errorItem = TagItem.Error

    init {
        loadInitialTags()
    }

    override fun onTagSelect(tagModel: TagItem.TagModel) {
        val updatedTags = updateTagSelection(tagModel)
        viewState = viewState.copy(tags = updatedTags)
        notifyTagsChanged(updatedTags)
    }

    override fun onRepeatClick() {
        loadInitialTags()
    }

    override fun loadNextPage() {
        fetchTags(viewState.extra?.offset)
    }

    private fun loadInitialTags() {
        fetchTags(offset = null)
    }

    private fun fetchTags(offset: String?) {
        scope.launch {
            showLoading()
            val result = when (entryPoint) {
                is FilterScreenEntryPoint.Blog -> tagRepository.getBlogTags(entryPoint.blog.blogUrl)
                FilterScreenEntryPoint.Feed -> tagRepository.getFeedTags(
                    limit = 100,
                    offset = offset
                )
            }

            handleTagsResult(result)
        }
    }

    private fun handleTagsResult(result: Result<Tags>) {
        if (result.isSuccess) {
            val tags = result.getOrThrow()
            updateTagsState(tags)
        } else {
            showError()
        }
    }

    private fun updateTagsState(tagsResponse: Tags) {
        val currentTags = viewState.tags.filterIsInstance<TagItem.TagModel>()
        val newTags = tagsResponse.data.searchTags.map { it.toTagModel() }

        viewState = viewState.copy(
            tags = (currentTags + newTags)
                .distinctBy { it.tag.title }
                .toImmutableList(),
            extra = tagsResponse.extra
        )
    }

    private fun updateTagSelection(tagModel: TagItem.TagModel): ImmutableList<TagItem> {
        return viewState.tags.map { item ->
            when (item) {
                is TagItem.TagModel -> {
                    if (item.tag.id == tagModel.tag.id) {
                        item.copy(selected = !item.selected)
                    } else {
                        item
                    }
                }
                else -> item
            }
        }.toImmutableList()
    }

    private fun notifyTagsChanged(tags: List<TagItem>) {
        onTagsChange(
            tags.filterIsInstance<TagItem.TagModel>()
                .filter { it.selected }
                .map { it.tag }
        )
    }

    private fun showLoading() {
        viewState = viewState.copy(
            tags = (viewState.tags.filterNonTagModels() + loadingItem).toImmutableList()
        )
    }

    private fun showError() {
        viewState = viewState.copy(
            tags = (viewState.tags.filterNonTagModels() + errorItem).toImmutableList()
        )
    }

    private fun List<TagItem>.filterNonTagModels() = filterIsInstance<TagItem.TagModel>()

    private companion object{
        fun List<Tag>.toTagModels(selected: Boolean) =
            map { TagItem.TagModel(it, selected) }

        fun Tags.SearchTag.toTagModel() =
            TagItem.TagModel(Tag(tag.id, tag.title), selected = false)
    }
}