package org.skriptlang.skript.docs.transformers;

import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.docs.generators.GenerationResult;

import java.util.List;

@ApiStatus.Experimental
public interface Transformer<T> {
	
	T transform(GenerationResult result);
	
	T combine(T a, T b);
	
	default T transform(List<GenerationResult> results) {
		T previous = null;
		for (GenerationResult result : results) {
			T current = transform(result);
			if (previous == null) {
				previous = current;
			} else {
				previous = combine(previous, current);
			}
		}
		
		return previous;
	}
	
}
