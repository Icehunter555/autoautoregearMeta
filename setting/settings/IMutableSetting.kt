package dev.wizard.meta.setting.settings

interface IMutableSetting<T : Any> : ISetting<T> {
    override var value: T
}
