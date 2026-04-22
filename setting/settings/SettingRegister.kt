package dev.wizard.meta.setting.settings

import dev.wizard.meta.graphics.color.ColorRGB
import dev.wizard.meta.setting.settings.impl.number.DoubleSetting
import dev.wizard.meta.setting.settings.impl.number.FloatSetting
import dev.wizard.meta.setting.settings.impl.number.IntegerSetting
import dev.wizard.meta.setting.settings.impl.other.BindSetting
import dev.wizard.meta.setting.settings.impl.other.ColorSetting
import dev.wizard.meta.setting.settings.impl.primitive.BooleanSetting
import dev.wizard.meta.setting.settings.impl.primitive.EnumSetting
import dev.wizard.meta.setting.settings.impl.primitive.StringSetting
import dev.wizard.meta.util.Bind
import kotlin.ranges.ClosedFloatingPointRange

interface SettingRegister<T> {
    fun T.setting(
        name: CharSequence,
        value: Int,
        range: IntRange,
        step: Int,
        visibility: (() -> Boolean)? = null,
        consumer: (Int, Int) -> Int = { _, input -> input },
        description: CharSequence = "",
        fineStep: Int = step,
        isTransient: Boolean = false
    ): IntegerSetting = setting(this, IntegerSetting(settingName(name), value, range, step, visibility, consumer, description, fineStep, isTransient))

    fun T.setting(
        name: CharSequence,
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
        visibility: (() -> Boolean)? = null,
        consumer: (Double, Double) -> Double = { _, input -> input },
        description: CharSequence = "",
        fineStep: Double = step,
        isTransient: Boolean = false
    ): DoubleSetting = setting(this, DoubleSetting(settingName(name), value, range, step, visibility, consumer, description, fineStep, isTransient))

    fun T.setting(
        name: CharSequence,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        step: Float,
        visibility: (() -> Boolean)? = null,
        consumer: (Float, Float) -> Float = { _, input -> input },
        description: CharSequence = "",
        fineStep: Float = step,
        isTransient: Boolean = false
    ): FloatSetting = setting(this, FloatSetting(settingName(name), value, range, step, visibility, consumer, description, fineStep, isTransient))

    fun T.setting(
        name: CharSequence,
        value: Bind,
        action: ((Boolean) -> Unit)? = null,
        visibility: (() -> Boolean)? = null,
        description: CharSequence = "",
        isTransient: Boolean = false
    ): BindSetting = setting(this, BindSetting(settingName(name), value, visibility, action, description, isTransient))

    fun T.setting(
        name: CharSequence,
        value: ColorRGB,
        hasAlpha: Boolean = true,
        visibility: (() -> Boolean)? = null,
        description: CharSequence = "",
        isTransient: Boolean = false
    ): ColorSetting = setting(this, ColorSetting(settingName(name), value, hasAlpha, visibility, description, isTransient))

    fun T.setting(
        name: CharSequence,
        value: Boolean,
        visibility: (() -> Boolean)? = null,
        consumer: (Boolean, Boolean) -> Boolean = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ): BooleanSetting = setting(this, BooleanSetting(settingName(name), value, visibility, consumer, description, isTransient))

    fun <E : Enum<E>> T.setting(
        name: CharSequence,
        value: E,
        visibility: (() -> Boolean)? = null,
        consumer: (E, E) -> E = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ): EnumSetting<E> = setting(this, EnumSetting(settingName(name), value, visibility, consumer, description, isTransient))

    fun T.setting(
        name: CharSequence,
        value: String,
        visibility: (() -> Boolean)? = null,
        consumer: (String, String) -> String = { _, input -> input },
        description: CharSequence = "",
        isTransient: Boolean = false
    ): StringSetting = setting(this, StringSetting(settingName(name), value, visibility, consumer, description, isTransient))

    fun <S : AbstractSetting<*>> setting(owner: T, setting: S): S

    fun settingName(input: CharSequence): CharSequence = input
}
