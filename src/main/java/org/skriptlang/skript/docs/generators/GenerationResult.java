package org.skriptlang.skript.docs.generators;

import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.registration.SyntaxInfo;

import java.util.List;

@ApiStatus.Experimental
public interface GenerationResult {
	
	String documentationId();
	
	String name();
	
	String[] description();
	
	String[] examples();
	
	String since();
	
	Class<?> type();
	
	List<String> patterns();
	
	SyntaxInfo<?> info();
	
	default String[] keywords() {
		return new String[0];
	}
	
	default String[] requiredPlugins() {
		return new String[0];
	}
	
	default String[] events() {
		return new String[0];
	}
	
	default boolean deprecated() {
		return false;
	}
	
}
