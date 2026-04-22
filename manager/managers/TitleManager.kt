package dev.wizard.meta.manager.managers

import dev.wizard.meta.MetaMod
import dev.wizard.meta.event.ListenerKt.listener
import dev.wizard.meta.event.events.TickEvent
import dev.wizard.meta.manager.Manager
import dev.wizard.meta.module.modules.client.ClickGUI
import org.lwjgl.opengl.Display
import java.lang.reflect.Field
import java.lang.reflect.Method

object TitleManager : Manager() {
    private const val FULL_TITLE = "Meta 0.3B-10mq29"
    private var animationState = AnimationState.TYPING
    private var currentTitle = ""
    private var tickCounter = 0
    private const val TYPE_DELAY = 2
    private const val DISPLAY_TIME = 40
    private const val WAIT_TIME = 20

    private lateinit var titleField: Field
    private lateinit var displayImpl: Any
    private lateinit var setTitleMethod: Method
    private var initialized = false

    private fun initializeTitleAnimation() {
        try {
            val displayClazz = Display::class.java
            titleField = displayClazz.getDeclaredField("title")
            val displayImplField = displayClazz.getDeclaredField("display_impl")
            displayImplField.isAccessible = true
            displayImpl = displayImplField.get(null)
            displayImplField.isAccessible = false
            
            val displayImplClass = Class.forName("org.lwjgl.opengl.DisplayImplementation")
            setTitleMethod = displayImplClass.getDeclaredMethod("setTitle", String::class.java)
        } catch (e: Exception) {
            MetaMod.logger.error("Failed to initialize title animation!")
            ClickGUI.animatedTitle = false
        }
    }

    private fun updateAnimatedTitle() {
        when (animationState) {
            AnimationState.TYPING -> {
                if (tickCounter % TYPE_DELAY == 0) {
                    if (currentTitle.length < FULL_TITLE.length) {
                        currentTitle = FULL_TITLE.substring(0, currentTitle.length + 1)
                        updateTitle(currentTitle)
                    } else {
                        animationState = AnimationState.DISPLAYING
                        tickCounter = 0
                    }
                }
                tickCounter++
            }
            AnimationState.DISPLAYING -> {
                if (tickCounter >= DISPLAY_TIME) {
                    animationState = AnimationState.BACKSPACING
                    tickCounter = 0
                }
                tickCounter++
            }
            AnimationState.BACKSPACING -> {
                if (tickCounter % TYPE_DELAY == 0) {
                    if (currentTitle.isNotEmpty()) {
                        currentTitle = currentTitle.substring(0, currentTitle.length - 1)
                        updateTitle(currentTitle)
                    } else {
                        animationState = AnimationState.WAITING
                        tickCounter = 0
                    }
                }
                tickCounter++
            }
            AnimationState.WAITING -> {
                if (tickCounter >= WAIT_TIME) {
                    animationState = AnimationState.TYPING
                    tickCounter = 0
                }
                tickCounter++
            }
        }
    }

    private fun updateTitle(newTitle: String) {
        try {
            titleField.isAccessible = true
            titleField.set(null, newTitle)
            titleField.isAccessible = false
            
            setTitleMethod.isAccessible = true
            setTitleMethod.invoke(displayImpl, newTitle)
            setTitleMethod.isAccessible = false
        } catch (ignored: Throwable) {}
    }

    init {
        listener<TickEvent.Post> {
            if (ClickGUI.animatedTitle) {
                if (!initialized) {
                    initializeTitleAnimation()
                    initialized = true
                }
                updateAnimatedTitle()
            } else if (initialized) {
                updateTitle(FULL_TITLE)
                initialized = false
            }
        }
    }

    private enum class AnimationState {
        TYPING, DISPLAYING, BACKSPACING, WAITING
    }
}
