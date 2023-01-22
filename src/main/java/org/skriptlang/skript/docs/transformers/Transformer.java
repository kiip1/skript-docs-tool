package org.skriptlang.skript.docs.transformers;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.docs.generators.GenerationResult;

import java.util.List;

@ApiStatus.Experimental
public interface Transformer<T, G extends GenerationResult> {
	
	T transform(G result);
	
	T combine(@Nullable T a, @Nullable T b);
	
	default T finisher(T value) {
		return value;
	}
	
	default T transform(List<G> results) {
		T previous = null;
		for (G result : results) {
			T current = transform(result);
			if (previous == null) {
				previous = current;
			} else {
				previous = combine(previous, current);
			}
		}
		
		return finisher(previous);
	}
	
}
