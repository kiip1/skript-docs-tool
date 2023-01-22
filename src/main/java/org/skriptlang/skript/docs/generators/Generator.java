package org.skriptlang.skript.docs.generators;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SkriptRegistry;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Experimental
public interface Generator<T extends GenerationResult> {
	
	@Nullable
	T generate(SyntaxInfo<?> syntax);
	
	default List<T> generate(SkriptRegistry registry, SkriptRegistry.Key<?> key) {
		List<T> results = new ArrayList<>();
		for (SyntaxInfo<?> syntax : registry.syntaxes(key)) {
			T result = generate(syntax);
			if (result != null)
				results.add(result);
		}
		
		return results;
	}
	
}
