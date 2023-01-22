package org.skriptlang.skript.docs.generators;

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
		
		GenerationResult result = new GenerationResultImpl(syntax, syntax.type());
		try {
			result.name();
		} catch (Exception ignored) {
			return null;
		}
		
		return result;
	}
	
}
