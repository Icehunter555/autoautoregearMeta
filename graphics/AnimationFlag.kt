package dev.wizard.meta.graphics

class AnimationFlag(private val interpolation: InterpolateFunction) {
    var prev: Float = 0f
        private set
    var current: Float = 0f
        private set
    var time: Long = System.currentTimeMillis()
        private set

    constructor(easing: Easing, length: Float) : this(InterpolateFunction { time, prev, current ->
        easing.incOrDec(Easing.toDelta(time, length), prev, current)
    })

    fun forceUpdate(value: Float) {
        forceUpdate(value, value)
    }

    fun forceUpdate(prev: Float, current: Float) {
        if (prev.isNaN() || current.isNaN()) return
        this.prev = prev
        this.current = current
        this.time = System.currentTimeMillis()
    }

    fun update(current: Float) {
        if (!current.isNaN() && this.current != current) {
            this.prev = this.current
            this.current = current
            this.time = System.currentTimeMillis()
        }
    }

    fun getAndUpdate(current: Float): Float {
        val render = interpolation.invoke(time, prev, this.current)
        if (!current.isNaN() && current != this.current) {
            this.prev = render
            this.current = current
            this.time = System.currentTimeMillis()
        }
        return render
    }

    fun get(): Float {
        return interpolation.invoke(time, prev, current)
    }

    fun forceCurrent() {
        prev = current
        time = System.currentTimeMillis()
    }
}