package dev.wizard.meta.command

class CommandNotFoundException(val command: String?) : Exception("Command not found: '$command'.")
