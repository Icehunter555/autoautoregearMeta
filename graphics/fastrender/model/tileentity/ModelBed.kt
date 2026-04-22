package dev.wizard.meta.graphics.fastrender.model.tileentity

import dev.wizard.meta.graphics.fastrender.model.Model
import dev.wizard.meta.graphics.fastrender.model.ModelBuilder

class ModelBed : Model(64, 64) {
    override fun buildModel(builder: ModelBuilder) {
        builder.childModel(0.0f, 0.0f) {
            addBox(-8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }
        builder.childModel(0.0f, 44.0f) {
            addBox(-8.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }
        builder.childModel(0.0f, 50.0f) {
            addBox(5.0f, 0.0f, -8.0f, 3.0f, 3.0f, 3.0f)
        }
        builder.childModel(0.0f, 22.0f) {
            addBox(-8.0f, 3.0f, -8.0f, 16.0f, 6.0f, 16.0f)
        }
        builder.childModel(12.0f, 44.0f) {
            addBox(-8.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }
        builder.childModel(12.0f, 50.0f) {
            addBox(5.0f, 0.0f, 5.0f, 3.0f, 3.0f, 3.0f)
        }
    }
}
