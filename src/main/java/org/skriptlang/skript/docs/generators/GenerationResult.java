package org.skriptlang.skript.docs.generators;

import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Experimental
public interface GenerationResult {
	
	AtomicInteger IOTA = new AtomicInteger();
	
	default String documentationId() {
		return IOTA.getAndIncrement() + "-" + name().toLowerCase(Locale.ROOT)
				.replaceAll(" ", "-");
	}
	
	String name();
	
	String[] description();
	
	String[] examples();
	
	String since();
	
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
