package dev.wizard.meta.graphics

inline fun buildAttribute(stride: Int, block: VertexAttribute.Builder.() -> Unit): VertexAttribute {
    return VertexAttribute.Builder(stride).apply(block).build()
}
