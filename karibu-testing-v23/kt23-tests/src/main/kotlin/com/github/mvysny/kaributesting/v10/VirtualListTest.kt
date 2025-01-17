package com.github.mvysny.kaributesting.v10

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTestDsl
import com.github.mvysny.dynatest.expectThrows
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.karibudsl.v23.virtualList
import com.github.mvysny.kaributesting.v23.*
import com.vaadin.flow.component.ClickNotifier
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.virtuallist.VirtualList
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.LocalDateRenderer
import com.vaadin.flow.data.renderer.NativeButtonRenderer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.test.expect
import kotlin.test.fail

@DynaTestDsl
internal fun DynaNodeGroup.virtualListTests() {

    beforeEach { MockVaadin.setup() }
    afterEach { MockVaadin.tearDown() }

    test("_dump") {
        val dp = ListDataProvider<TestPerson>((0 until 7).map { TestPerson("name $it", it) })
        val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
            setRenderer { "${it.name}, ${it.age}" }
        }
        expect("VirtualList[dataprovider='ListDataProvider<TestPerson>(7 items)']\n--and 7 more\n") { vl._dump(0 until 0) }
        expect("VirtualList[dataprovider='ListDataProvider<TestPerson>(7 items)']\n0: name 0, 0\n1: name 1, 1\n2: name 2, 2\n3: name 3, 3\n4: name 4, 4\n--and 2 more\n") { vl._dump(0 until 5) }
        expect("VirtualList[dataprovider='ListDataProvider<TestPerson>(7 items)']\n0: name 0, 0\n1: name 1, 1\n2: name 2, 2\n3: name 3, 3\n4: name 4, 4\n5: name 5, 5\n6: name 6, 6\n") {
            vl._dump(0 until 20)
        }
    }

    group("expectRow()") {
        test("simple") {
            val dp = ListDataProvider<TestPerson>((0 until 7).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                setRenderer { "${it.name}, ${it.age}" }
            }
            vl.expectRows(7)
            vl.expectRow(0, "name 0, 0")
        }

        test("failed expectRow contains table dump") {
            val dp = ListDataProvider<TestPerson>((0 until 1).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                setRenderer({ "${it.name}, ${it.age}" })
            }
            expectThrows(AssertionError::class, "0: name 0, 0") {
                vl.expectRow(0, "name 1, 1")
            }
        }

        test("row out-of-bounds contains table dump") {
            val dp: ListDataProvider<TestPerson> = ListDataProvider((0 until 1).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                setRenderer { "${it.name}, ${it.age}" }
            }
            expectThrows(AssertionError::class, "0: name 0, 0") {
                vl.expectRow(3, "should fail") // should fail
            }
        }
    }

    group("renderers") {
        test("NativeButtonRenderer") {
            val dp: ListDataProvider<TestPerson> = ListDataProvider((0 until 7).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                setRenderer(NativeButtonRenderer<TestPerson>("View", { }))
            }
            vl.expectRow(0, "View")
        }
        test("ComponentRenderer") {
            val dp: ListDataProvider<TestPerson> = ListDataProvider((0 until 7).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                setRenderer(ComponentRenderer<Button, TestPerson> { it -> Button(it.name) })
            }
            vl.expectRow(0, "Button[caption='name 0']")
        }
        test("LocalDateRenderer") {
            val dp: ListDataProvider<TestPerson> = ListDataProvider((0 until 7).map { TestPerson("name $it", it) })
            val vl = UI.getCurrent().virtualList<TestPerson>(dp) {
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(Locale("fi", "FI"))
                // use this constructor: the non-deprecated one isn't available in Vaadin 22
                @Suppress("DEPRECATION")
                setRenderer(LocalDateRenderer<TestPerson>({ LocalDate.of(2019, 3, 1) }, formatter))
            }
            vl.expectRow(0, "1.3.2019")
        }
    }

    group("click renderer") {
        test("ClickableRenderer") {
            var called = false
            val vl: VirtualList<TestPerson> = VirtualList<TestPerson>().apply {
                setRenderer(NativeButtonRenderer<TestPerson>("View") { person ->
                    called = true
                    expect("name 8") { person.name }
                })
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            vl._clickRenderer(8)
            expect(true) { called }
        }
        test("ComponentRenderer with Button") {
            var called = false
            val vl = VirtualList<TestPerson>().apply {
                setRenderer(ComponentRenderer<Button, TestPerson> { person -> Button("View").apply {
                    onLeftClick {
                        called = true
                        expect("name 8") { person.name }
                    }
                } })
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            vl._clickRenderer(8)
            expect(true) { called }
        }
        test("ComponentRenderer with ClickNotifier") {
            var called = false
            val vl = VirtualList<TestPerson>().apply {
                setRenderer(ComponentRenderer<Checkbox, TestPerson> { person -> Checkbox("View").apply {
                    addClickListener {
                        called = true
                        expect("name 8") { person.name }
                    }
                } })
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            vl._clickRenderer(8)
            expect(true) { called }
        }
        test("fails on disabled VirtualList") {
            val vl: VirtualList<TestPerson> = VirtualList<TestPerson>().apply {
                setRenderer(NativeButtonRenderer<TestPerson>("View") { fail("Shouldn't be called") })
                setItems((0..10).map { TestPerson("name $it", it) })
                isEnabled = false
            }
            expectThrows(IllegalStateException::class, "The VirtualList[DISABLED, dataprovider='ListDataProvider<TestPerson>(11 items)'] is not enabled") {
                vl._clickRenderer(2)
            }
        }
        test("fails on unsupported component type") {
            expect(false) { Label() is ClickNotifier<*> }
            val vl = VirtualList<TestPerson>().apply {
                setItems((0..10).map { TestPerson("name $it", it) })
                setRenderer(ComponentRenderer<Label, TestPerson> { _ -> Label() })
            }
            expectThrows(AssertionError::class, "VirtualList[dataprovider='ListDataProvider<TestPerson>(11 items)']: ComponentRenderer produced Label[] which is not a button nor a ClickNotifier - please use _getCellComponent() instead") {
                vl._clickRenderer(8)
            }
        }
    }

    group("_getRowComponent") {
        test("fails with ClickableRenderer") {
            val vl: VirtualList<TestPerson> = VirtualList<TestPerson>().apply {
                setRenderer(NativeButtonRenderer<TestPerson>("View") {})
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            expectThrows(AssertionError::class, "VirtualList[dataprovider='ListDataProvider<TestPerson>(11 items)'] uses NativeButtonRenderer which is not supported by this function") {
                vl._getRowComponent(8)
            }
        }
        test("ComponentRenderer with Button") {
            var called = false
            val vl = VirtualList<TestPerson>().apply {
                setRenderer(ComponentRenderer<Button, TestPerson> { person -> Button("View").apply {
                    onLeftClick {
                        called = true
                        expect("name 8") { person.name }
                    }
                } })
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            (vl._getRowComponent(8) as Button)._click()
            expect(true) { called }
        }
        test("doesn't fail on disabled VirtualList") {
            val vl: VirtualList<TestPerson> = VirtualList<TestPerson>().apply {
                setRenderer(ComponentRenderer<Button, TestPerson> { _ -> Button("View") })
                setItems((0..10).map { TestPerson("name $it", it) })
                isEnabled = false
            }
            expect(true) { vl._getRowComponent(2) is Button }
        }
    }

    group("_getFormatted()") {
        test("non-existing row") {
            val vl = VirtualList<TestPerson>()
            expectThrows(AssertionError::class, "Requested to get row 0 but the data provider only has 0 rows") {
                vl._getFormatted(0)
            }
        }

        test("basic") {
            val vl = VirtualList<TestPerson>().apply {
                setRenderer { "${it.name} ${it.age}" }
                setItems((0..10).map { TestPerson("name $it", it) })
            }
            expect("name 0 0") { vl._getFormatted(0) }
        }
    }
}

data class TestPerson(var name: String, var age: Int): Comparable<TestPerson> {
    override fun compareTo(other: TestPerson): Int = compareValuesBy(this, other, { it.name }, { it.age })
}
