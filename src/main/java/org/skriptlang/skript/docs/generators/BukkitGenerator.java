package org.skriptlang.skript.docs.generators;

import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;

@ApiStatus.Internal
public final class BukkitGenerator implements Generator<GenerationResult> {
	
	@Nullable
	@Override
	public GenerationResult generate(SyntaxInfo<?> syntax) {
		if (syntax.type().getDeclaredAnnotation(NoDoc.class) != null)
			return null;
		
		if (syntax instanceof BukkitSyntaxInfos.Event)
			return new EventGenerationResult((BukkitSyntaxInfos.Event<?>) syntax, syntax.type());

		if (syntax.type().getAnnotation(Name.class) != null)
			return new StandardGenerationResult(syntax, syntax.type());
		
		return null;
	}
	
}
