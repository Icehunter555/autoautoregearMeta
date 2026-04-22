package dev.wizard.meta.graphics.fastrender.model.tileentity

import dev.wizard.meta.graphics.fastrender.model.Model
import dev.wizard.meta.graphics.fastrender.model.ModelBuilder

class ModelShulkerBox : Model(64, 64) {
    override fun buildModel(builder: ModelBuilder) {
        builder.childModel(0.0f, 28.0f) {
            addBox(-8.0f, 0.0f, -8.0f, 16.0f, 8.0f, 16.0f)
        }
        builder.childModel(0.0f, 0.0f) {
            addBox(-8.0f, 4.0f, -8.0f, 16.0f, 12.0f, 16.0f)
        }
    }
}
