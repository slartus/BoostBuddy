package ru.slartus.boostbuddy.components.subscribes

interface LogoutDialogComponent {
    fun onDismissed()
    fun onAcceptClicked()
    fun onCancelClicked()
}

class LogoutDialogComponentImpl(
    private val onDismissed: () -> Unit,
    private val onAcceptClicked: () -> Unit,
    private val onCancelClicked: () -> Unit
) : LogoutDialogComponent {
    override fun onDismissed() {
        onDismissed.invoke()
    }

    override fun onAcceptClicked() {
        onAcceptClicked.invoke()
    }

    override fun onCancelClicked() {
        onCancelClicked.invoke()
    }
}