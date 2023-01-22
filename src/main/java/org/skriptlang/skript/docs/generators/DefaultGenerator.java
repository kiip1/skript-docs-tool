package org.skriptlang.skript.docs.generators;

import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;

@ApiStatus.Internal
public final class DefaultGenerator implements Generator<GenerationResult> {
	
	@Nullable
	@Override
	public GenerationResult generate(SyntaxInfo<?> syntax) {
		if (syntax.type().getDeclaredAnnotation(NoDoc.class) != null)
			return null;
		
		if (syntax.type().getAnnotation(Name.class) == null)
			return null;
		
		return new GenerationResultImpl(syntax, syntax.type());
	}
	
}
