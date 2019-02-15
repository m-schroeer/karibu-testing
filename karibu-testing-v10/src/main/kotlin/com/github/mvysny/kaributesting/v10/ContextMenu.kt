package com.github.mvysny.kaributesting.v10

import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.contextmenu.ContextMenuBase
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.contextmenu.MenuItemBase
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem
import com.vaadin.flow.dom.DomEvent
import elemental.json.impl.JreJsonFactory
import kotlin.test.expect
import kotlin.test.fail

/**
 * Tries to find a menu item with given caption and click it.
 * @throws AssertionError if no such menu item exists, or the menu item is not enabled or visible, or it's nested in
 * a menu item which is invisible or disabled, or it's attached to a component that's invisible.
 */
fun ContextMenu._clickItemWithCaption(caption: String) {
    val parentMap = getParentMap()
    val item = parentMap.keys.firstOrNull { it.getText() == caption } ?: fail("No menu item with caption $caption in ContextMenu:\n${toPrettyTree()}")
    (item as MenuItem)._click()
}

/**
 * Tries to find a menu item with given caption and click it, passing in given [gridItem].
 * @throws AssertionError if no such menu item exists, or the menu item is not enabled or visible, or it's nested in
 * a menu item which is invisible or disabled, or it's attached to a component that's invisible.
 */
fun <T> GridContextMenu<T>._clickItemWithCaption(caption: String, gridItem: T?) {
    val parentMap = getParentMap()
    val item = parentMap.keys.firstOrNull { it.getText() == caption } ?: fail("No menu item with caption $caption in GridContextMenu:\n${toPrettyTree()}")
    @Suppress("UNCHECKED_CAST")
    (item as GridMenuItem<T>)._click(gridItem)
}

private fun ContextMenuBase<*, *, *>.getParentMap(): Map<MenuItemBase<*, *, *>, Component> {
    val result = mutableMapOf<MenuItemBase<*, *, *>, Component>()

    fun fillInParentFor(item: MenuItemBase<*, *, *>, parent: Component) {
        result[item] = parent
        item.getSubMenu().getItems().forEach { fillInParentFor(it, item) }
    }

    getItems().forEach { fillInParentFor(it, this) }
    return result
}

/**
 * Tries to click given menu item.
 * @throws AssertionError if no such menu item exists, or the menu item is not enabled or visible, or it's nested in
 * a menu item which is invisible or disabled, or it's attached to a component that's invisible.
 */
fun MenuItem._click() {
    val parentMap = contextMenu.getParentMap()
    checkMenuItemVisible(this, parentMap)
    checkMenuItemEnabled(this, parentMap)
    _fireEvent(ClickEvent<MenuItem>(this, true, 0, 0, 0, 0, 1, 1, false, false, false, false))
}

/**
 * Tries to click given menu item, passing in given [gridItem].
 * @throws AssertionError if no such menu item exists, or the menu item is not enabled or visible, or it's nested in
 * a menu item which is invisible or disabled, or it's attached to a component that's invisible.
 */
fun <T> GridMenuItem<T>._click(gridItem: T?) {
    val parentMap = contextMenu.getParentMap()
    checkMenuItemVisible(this, parentMap)
    checkMenuItemEnabled(this, parentMap)
    val grid = contextMenu.target as Grid<T>
    val key = grid.dataCommunicator.keyMapper.key(gridItem)
    requireNotNull(key) { "grid ${grid.toPrettyString()} generated null as key for $gridItem" }
    grid.element.setProperty("_contextMenuTargetItemKey", key)
    element._fireDomEvent(DomEvent(element, "click", JreJsonFactory().createObject()))
}

private fun MenuItemBase<*, *, *>.checkMenuItemVisible(originalItem: MenuItemBase<*, *, *>, parentMap: Map<MenuItemBase<*, *, *>, Component>) {
    if (!isVisible()) {
        if (originalItem == this) {
            fail("${originalItem.toPrettyString()} is not visible")
        } else {
            fail("${originalItem.toPrettyString()} is not visible because its parent item is not visible:\n${toPrettyTree()}")
        }
    }
    val parent = parentMap[this] ?: fail("${originalItem.toPrettyString()} is not part of\n${getContextMenu().toPrettyTree()}?!?")
    when(parent) {
        is MenuItem -> parent.checkMenuItemVisible(originalItem, parentMap)
        is ContextMenu -> expect(true, "Cannot click ${originalItem.toPrettyString()} since it's attached to ${parent.target.toPrettyString()} which is not effectively visible") {
            parent.target.isEffectivelyVisible()
        }
        is GridContextMenu<*> -> expect(true, "Cannot click ${originalItem.toPrettyString()} since it's attached to ${parent.target.toPrettyString()} which is not effectively visible") {
            parent.target.isEffectivelyVisible()
        }
        else -> fail("Unexpected parent ${parent.toPrettyString()}")
    }
}

private fun MenuItemBase<*, *, *>.checkMenuItemEnabled(originalItem: MenuItemBase<*, *, *>, parentMap: Map<MenuItemBase<*, *, *>, Component>) {
    if (!isEnabled) {
        if (originalItem == this) {
            fail("${originalItem.toPrettyString()} is not enabled")
        } else {
            fail("${originalItem.toPrettyString()} is not enabled because its parent item is not enabled:\n${toPrettyTree()}")
        }
    }
    val parent = parentMap[this] ?: fail("${originalItem.toPrettyString()} is not part of\n${getContextMenu().toPrettyTree()}?!?")
    when(parent) {
        is MenuItem -> parent.checkMenuItemEnabled(originalItem, parentMap)
        is ContextMenu -> Unit
        is GridContextMenu<*> -> Unit
        else -> fail("Unexpected parent ${parent.toPrettyString()}")
    }
}
