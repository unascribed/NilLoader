<img src="nilloader.svg" width="180px" align="right"/>

# NilLoader

NilLoader (ØL or 0L) is a minimal, easy-to-install, application-independent system for applying
runtime patches to programs written in Java, compatible with any launcher that allows you to modify
JVM arguments.

It's based on the native Java agent system, but provides a comprehensive and convenient API for
defining class patches and entrypoints on top of it. It additionally provides a system for working
with obfuscated environments, without forcing you to write code using obfuscated names. (Note that
NilLoader does not provide a "high-level" bytecode patching system like Mixin; you have to write
raw bytecode patches using Mini or ASM directly.)

A NilLoader mod includes a complete copy of NilLoader within itself, allowing a NilLoader mod to be
used directly as a Java agent. However, you can also load NilLoader alone as a Java agent, and it
will discover NilLoader-compatible jars in a subdirectory called `mods`, or `nilmods` if another
loader is being layered with NilLoader and is confused by the presence of NilLoader jars in its
`mods` directory. Copies of NilLoader contained in these mods will be ignored by the class loader,
avoiding version conflicts.

## NilLoader + Minecraft

NilLoader was primarily designed for Minecraft, like a lot of Java bytecode patching frameworks. In
a Minecraft environment, it is compatible with all current and past loaders, and can patch any
version of the game from Cave Game Test to the latest snapshot. However, since NilLoader is so
generic, it does not come with many conveniences that other loaders do, such as intermediate
mappings, cross-version compatibility, or any API of any kind. NilLoader lets you do two things:
patch classes, and get told when the JVM starts before anything else has run.

However, given these two possibilities, you can do just about anything else you want. Note that
NilLoader is not a replacement for Fabric or Forge, it is an additional option for supporting weird
versions or doing things the other loaders won't let you do. Someone could very well build an entire
API on top of NilLoader, but that someone will not be me, and it's not something I intend to have
happen.

NilLoader has its origins in the cross-version patching framework created for Ears, used for its
ports to Beta 1.7, early versions of Forge, NFC, etc. That is, more or less, its intended purpose.

## Using NilLoader

