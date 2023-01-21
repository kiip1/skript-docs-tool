package org.skriptlang.skript.docs.generators;

import ch.njol.skript.doc.NoDoc;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.docs.Documentation;
import org.skriptlang.skript.registration.SyntaxInfo;

@ApiStatus.Internal
public final class DefaultGenerator implements Generator<GenerationResult> {
	
	private final Documentation documentation;
	
	public DefaultGenerator(Documentation documentation) {
		this.documentation = documentation;
	}
	
	@Nullable
	@Override
	public GenerationResult generate(SyntaxInfo<?> syntax) {
		if (syntax.type().getDeclaredAnnotation(NoDoc.class) != null)
			return null;
		
		return new GenerationResultImpl(syntax.type());
	}
	
	@Override
	public Documentation documentation() {
		return documentation;
	}
	
}
